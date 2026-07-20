package com.snehil.project.lovable_clone.config;

import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.repository.PlanRepository;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Verifies on startup that every active {@link Plan}'s Stripe Price id exists in the
 * Stripe account the configured API key belongs to. Plan rows survive key/account
 * switches (and dashboard deletions), after which checkout fails with
 * "No such price: ...". With a TEST key this auto-creates a replacement
 * Product + Price and updates the plan; with a live key it only logs the problem.
 */
@Component
@Order(2) // after BillingPlansInitializer has seeded plan rows
@RequiredArgsConstructor
@Slf4j
public class StripePlanPriceValidator implements ApplicationRunner {

    private final PlanRepository planRepository;

    @Value("${stripe.api.secret:}")
    private String stripeSecretKey;

    @Override
    public void run(ApplicationArguments args) {
        if (stripeSecretKey == null || !stripeSecretKey.startsWith("sk_")) {
            log.debug("No usable Stripe API key configured; skipping plan price validation");
            return;
        }

        for (Plan plan : planRepository.findAll()) {
            if (!Boolean.TRUE.equals(plan.getActive())) continue;
            if (plan.getStripePriceId() == null || plan.getStripePriceId().isBlank()) {
                // Cashfree-only plans are valid without a Stripe price id — provision one for Stripe checkout.
                if (stripeSecretKey.startsWith("sk_test_") && hasInrAmount(plan)) {
                    provisionTestPrice(plan);
                }
                continue;
            }
            if (isPriceUsable(plan)) continue;

            if (!stripeSecretKey.startsWith("sk_test_")) {
                log.error("Plan '{}' (id={}) has an unusable Stripe price id '{}' and the API key is a LIVE key. "
                                + "Create a Price in the Stripe dashboard and update plan.stripe_price_id manually.",
                        plan.getName(), plan.getId(), plan.getStripePriceId());
                continue;
            }

            provisionTestPrice(plan);
        }
    }

    private boolean isPriceUsable(Plan plan) {
        String priceId = plan.getStripePriceId();
        if (priceId == null || priceId.isBlank()) {
            log.warn("Plan '{}' (id={}) has no Stripe price id", plan.getName(), plan.getId());
            return false;
        }
        try {
            Price price = Price.retrieve(priceId);
            if (!Boolean.TRUE.equals(price.getActive())) {
                log.warn("Stripe price {} for plan '{}' is archived", priceId, plan.getName());
                return false;
            }
            // TEST only: replace outdated $20 placeholders when plan has ₹ amount.
            if (stripeSecretKey.startsWith("sk_test_") && hasInrAmount(plan)
                    && !matchesInrAmount(price, plan.getAmountInr())) {
                log.warn("Stripe price {} for plan '{}' is {} {} (expected INR {}) — will re-provision",
                        priceId, plan.getName(), price.getCurrency(), price.getUnitAmount(), plan.getAmountInr());
                return false;
            }
            return true;
        } catch (InvalidRequestException e) {
            if ("resource_missing".equals(e.getCode())) {
                log.warn("Stripe price {} for plan '{}' does not exist in the connected Stripe account",
                        priceId, plan.getName());
                return false;
            }
            log.error("Could not verify Stripe price {} for plan '{}'", priceId, plan.getName(), e);
            return true; // unknown failure: leave the plan untouched
        } catch (StripeException e) {
            // Auth/network problems — validating other plans would fail the same way.
            log.error("Stripe unavailable while validating plan prices: {}", e.getMessage());
            return true;
        }
    }

    private void provisionTestPrice(Plan plan) {
        try {
            long unitAmount = unitAmountFor(plan);
            String currency = hasInrAmount(plan) ? "inr" : "usd";
            Product product = Product.create(ProductCreateParams.builder()
                    .setName(plan.getName().trim())
                    .build());
            Price price = Price.create(PriceCreateParams.builder()
                    .setProduct(product.getId())
                    .setCurrency(currency)
                    .setUnitAmount(unitAmount)
                    .setRecurring(PriceCreateParams.Recurring.builder()
                            .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                            .build())
                    .build());
            plan.setStripePriceId(price.getId());
            planRepository.save(plan);
            log.warn("Auto-provisioned Stripe TEST price {} ({} {}/month) for plan '{}'",
                    price.getId(), currency.toUpperCase(Locale.ROOT), unitAmount / 100.0, plan.getName());
        } catch (StripeException e) {
            log.error("Failed to auto-provision a Stripe price for plan '{}'", plan.getName(), e);
        }
    }

    private static boolean hasInrAmount(Plan plan) {
        return plan.getAmountInr() != null && plan.getAmountInr().compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean matchesInrAmount(Price price, BigDecimal amountInr) {
        if (price.getUnitAmount() == null || amountInr == null) return false;
        long expectedPaise = amountInr.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        return "inr".equalsIgnoreCase(price.getCurrency()) && price.getUnitAmount().equals(expectedPaise);
    }

    private static long unitAmountFor(Plan plan) {
        if (hasInrAmount(plan)) {
            return plan.getAmountInr().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
        String name = plan.getName() == null ? "" : plan.getName().toLowerCase(Locale.ROOT);
        return name.contains("business") ? 5000L : 2000L;
    }
}
