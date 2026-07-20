package com.snehil.project.lovable_clone.config;

import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Ensures at least one active {@link Plan} exists when a default Stripe Price ID and/or
 * Cashfree INR amount is configured (so checkout with {@code planId: 1} works on a fresh DB).
 * Also keeps {@code amount_inr} in sync with {@code CASHFREE_DEFAULT_AMOUNT_INR}.
 */
@Component
@Order(1) // before StripePlanPriceValidator, which checks the seeded rows
@RequiredArgsConstructor
@Slf4j
public class BillingPlansInitializer implements ApplicationRunner {

    private final PlanRepository planRepository;

    @Value("${billing.default-plan.name:Pro}")
    private String defaultPlanName;

    @Value("${billing.default-plan.stripe-price-id:}")
    private String defaultStripePriceId;

    @Value("${billing.default-plan.amount-inr:699}")
    private BigDecimal defaultAmountInr;

    @Override
    public void run(ApplicationArguments args) {
        boolean hasStripe = StringUtils.hasText(defaultStripePriceId);
        boolean hasCashfree = defaultAmountInr != null && defaultAmountInr.compareTo(BigDecimal.ZERO) > 0;

        if (planRepository.count() > 0) {
            if (hasCashfree) {
                syncAmountInr();
            }
            return;
        }

        if (!hasStripe && !hasCashfree) {
            log.warn(
                    "No plans in the database and no default Stripe price / Cashfree amount configured. "
                            + "Set STRIPE_DEFAULT_PRICE_ID and/or CASHFREE_DEFAULT_AMOUNT_INR, then restart.");
            return;
        }

        Plan plan = new Plan();
        plan.setName(defaultPlanName);
        if (hasStripe) {
            plan.setStripePriceId(defaultStripePriceId.trim());
        }
        if (hasCashfree) {
            plan.setAmountInr(defaultAmountInr);
        }
        plan.setMaxProjects(100);
        plan.setMaxTokensPerDay(100_000);
        plan.setMaxPreviews(50);
        plan.setUnlimitedAi(false);
        plan.setActive(true);
        planRepository.save(plan);
        log.info(
                "Seeded default billing plan '{}' id={} (stripe={}, amountInr={})",
                defaultPlanName,
                plan.getId(),
                hasStripe,
                hasCashfree ? defaultAmountInr : null);
    }

    /** Keep Cashfree INR amount aligned with env (e.g. 699) even if an older value was stored. */
    private void syncAmountInr() {
        for (Plan plan : planRepository.findAll()) {
            if (plan.getAmountInr() == null || plan.getAmountInr().compareTo(defaultAmountInr) != 0) {
                BigDecimal previous = plan.getAmountInr();
                plan.setAmountInr(defaultAmountInr);
                planRepository.save(plan);
                log.info(
                        "Synced amount_inr on plan id={} from {} to {}",
                        plan.getId(),
                        previous,
                        defaultAmountInr);
            }
        }
    }
}
