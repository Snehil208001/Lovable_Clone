package com.snehil.project.lovable_clone.config;

import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures at least one active {@link Plan} exists when a default Stripe Price ID is configured
 * (so {@code POST /api/payments/checkout} with {@code planId: 1} works on a fresh database).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingPlansInitializer implements ApplicationRunner {

    private final PlanRepository planRepository;

    @Value("${billing.default-plan.name:Pro}")
    private String defaultPlanName;

    @Value("${billing.default-plan.stripe-price-id:}")
    private String defaultStripePriceId;

    @Override
    public void run(ApplicationArguments args) {
        if (planRepository.count() > 0) {
            return;
        }
        if (defaultStripePriceId == null || defaultStripePriceId.isBlank()) {
            log.warn(
                    "No plans in the database and billing.default-plan.stripe-price-id is empty. "
                            + "Set STRIPE_DEFAULT_PRICE_ID (or billing.default-plan.stripe-price-id) "
                            + "to a real Stripe Price id, then restart to seed plan id 1. "
                            + "Until then, checkout will return 404 for unknown plan ids.");
            return;
        }

        Plan plan = new Plan();
        plan.setName(defaultPlanName);
        plan.setStripePriceId(defaultStripePriceId.trim());
        plan.setMaxProjects(100);
        plan.setMaxTokensPerDay(100_000);
        plan.setMaxPreviews(50);
        plan.setUnlimitedAi(false);
        plan.setActive(true);
        planRepository.save(plan);
        log.info(
                "Seeded default billing plan '{}' id={} (Stripe price id configured)",
                defaultPlanName,
                plan.getId());
    }
}
