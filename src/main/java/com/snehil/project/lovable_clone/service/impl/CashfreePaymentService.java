package com.snehil.project.lovable_clone.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutRequest;
import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutResponse;
import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.entity.User;
import com.snehil.project.lovable_clone.error.BadRequestException;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.repository.PlanRepository;
import com.snehil.project.lovable_clone.repository.UserRepository;
import com.snehil.project.lovable_clone.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashfreePaymentService {

    private static final String API_VERSION = "2025-01-01";
    private static final String DEFAULT_PHONE = "9999999999";

    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @Value("${cashfree.app-id:}")
    private String appId;

    @Value("${cashfree.secret-key:}")
    private String secretKey;

    @Value("${cashfree.env:SANDBOX}")
    private String env;

    @Value("${cashfree.notify-url:}")
    private String notifyUrl;

    @Value("${client.url}")
    private String frontendUrl;

    public CheckoutResponse createCheckoutSession(CheckoutRequest request, Long userId) {
        requireConfigured();

        Plan plan = planRepository.findById(request.planId()).orElseThrow(() ->
                new ResourceNotFoundException("Plan", request.planId().toString()));
        if (plan.getAmountInr() == null || plan.getAmountInr().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "This plan has no Cashfree INR amount. Set plan.amount_inr (or billing.default-plan.amount-inr).");
        }

        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("user", userId.toString()));

        String orderId = "ac" + userId + "p" + plan.getId() + "t" + System.currentTimeMillis();
        String phone = StringUtils.hasText(request.customerPhone())
                ? request.customerPhone().trim()
                : DEFAULT_PHONE;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id", orderId);
        body.put("order_amount", plan.getAmountInr());
        body.put("order_currency", "INR");
        body.put("customer_details", Map.of(
                "customer_id", "user_" + userId,
                "customer_email", user.getUsername() != null ? user.getUsername() : "user" + userId + "@example.com",
                "customer_phone", phone,
                "customer_name", user.getName() != null ? user.getName() : "Customer"
        ));
        body.put("order_meta", buildOrderMeta(orderId));
        body.put("order_tags", Map.of(
                "user_id", userId.toString(),
                "plan_id", plan.getId().toString()
        ));

        String mode = resolvedMode();
        log.info("Cashfree create order env={} baseUrl={} orderId={} amountInr={}",
                mode, baseUrl(), orderId, plan.getAmountInr());

        try {
            JsonNode response = restClient().post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.hasNonNull("payment_session_id")) {
                throw new BadRequestException("Cashfree did not return a payment session");
            }
            return CheckoutResponse.cashfree(response.get("payment_session_id").asText(), mode);
        } catch (BadRequestException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("Cashfree create order failed status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Cashfree rejected checkout (" + mode + "): " + shortCashfreeError(e));
        } catch (RestClientException e) {
            log.error("Cashfree create order transport error env={}", mode, e);
            throw new BadRequestException("Cashfree checkout unreachable (" + mode + "): " + e.getMessage());
        } catch (Exception e) {
            log.error("Cashfree create order unexpected error env={}", mode, e);
            throw new BadRequestException("Cashfree checkout failed (" + mode + "): " + e.getMessage());
        }
    }

    public void handleWebhook(String rawBody, String signature, String timestamp) {
        requireConfigured();
        if (!verifySignature(rawBody, signature, timestamp)) {
            throw new BadRequestException("Invalid Cashfree webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String type = text(root, "type");
            if (type == null) {
                log.debug("Cashfree webhook missing type");
                return;
            }

            switch (type) {
                case "PAYMENT_SUCCESS_WEBHOOK", "SUBSCRIPTION_PAYMENT_SUCCESS" -> handlePaymentSuccess(root);
                case "PAYMENT_FAILED_WEBHOOK" -> log.info("Cashfree payment failed webhook ignored for activation");
                default -> log.debug("Ignoring Cashfree webhook type: {}", type);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process Cashfree webhook", e);
            throw new BadRequestException("Cashfree webhook processing failed");
        }
    }

    private void handlePaymentSuccess(JsonNode root) {
        JsonNode order = root.path("data").path("order");
        if (order.isMissingNode() || order.isNull()) {
            order = root.path("data");
        }

        String orderId = text(order, "order_id");
        if (orderId == null) {
            log.warn("Cashfree success webhook missing order_id");
            return;
        }

        JsonNode tags = order.path("order_tags");
        String userIdRaw = text(tags, "user_id");
        String planIdRaw = text(tags, "plan_id");
        if (userIdRaw == null || planIdRaw == null) {
            // Fallback: parse from order_id pattern ac{userId}p{planId}t{ts}
            Long[] parsed = parseOrderIds(orderId);
            if (parsed == null) {
                log.warn("Cashfree webhook missing user/plan tags for order {}", orderId);
                return;
            }
            userIdRaw = parsed[0].toString();
            planIdRaw = parsed[1].toString();
        }

        Long userId = Long.parseLong(userIdRaw);
        Long planId = Long.parseLong(planIdRaw);

        subscriptionService.activateSubscription(userId, planId, orderId, null, "CASHFREE");
        Instant start = Instant.now();
        subscriptionService.renewSubscriptionPeriod(orderId, start, start.plus(30, ChronoUnit.DAYS));
        log.info("Activated Cashfree subscription for user {} plan {} order {}", userId, planId, orderId);
    }

    boolean verifySignature(String rawBody, String signature, String timestamp) {
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(timestamp) || rawBody == null) {
            return false;
        }
        try {
            String payload = timestamp + rawBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expected.equals(signature);
        } catch (Exception e) {
            log.warn("Cashfree signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildOrderMeta(String orderId) {
        String base = StringUtils.hasText(frontendUrl) ? frontendUrl.trim() : "http://localhost:5173";
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("return_url", base + "/billing?status=success&order_id=" + orderId);
        if (StringUtils.hasText(notifyUrl)) {
            meta.put("notify_url", notifyUrl.trim());
        }
        return meta;
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(baseUrl())
                .defaultHeader("x-client-id", appId.trim())
                .defaultHeader("x-client-secret", secretKey.trim())
                .defaultHeader("x-api-version", API_VERSION)
                .defaultHeader("accept", "application/json")
                .build();
    }

    private String baseUrl() {
        return "sandbox".equals(resolvedMode())
                ? "https://sandbox.cashfree.com/pg"
                : "https://api.cashfree.com/pg";
    }

    /** sandbox | production — null/blank env treated as sandbox (test keys are common during setup). */
    private String resolvedMode() {
        if (!StringUtils.hasText(env) || "SANDBOX".equalsIgnoreCase(env.trim())) {
            return "sandbox";
        }
        return "production";
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(secretKey)) {
            throw new BadRequestException("Cashfree is not configured (cashfree.app-id / cashfree.secret-key)");
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return StringUtils.hasText(value) ? value : null;
    }

    private static Long[] parseOrderIds(String orderId) {
        // ac{userId}p{planId}t{timestamp}
        try {
            if (!orderId.startsWith("ac")) return null;
            int p = orderId.indexOf('p', 2);
            int t = orderId.indexOf('t', p + 1);
            if (p < 0 || t < 0) return null;
            return new Long[]{
                    Long.parseLong(orderId.substring(2, p)),
                    Long.parseLong(orderId.substring(p + 1, t))
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static String shortCashfreeError(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (!StringUtils.hasText(body)) {
            return e.getMessage();
        }
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }
}
