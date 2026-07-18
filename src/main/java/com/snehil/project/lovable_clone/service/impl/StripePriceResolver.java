package com.snehil.project.lovable_clone.service.impl;

import com.stripe.model.Price;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a Stripe Price id to a display amount (e.g. "20" for $20.00/month).
 * Plans only store the price id; the amount lives in Stripe. Successful lookups
 * are cached for the life of the process — a Stripe Price's amount is immutable,
 * so the cache can never go stale. Failures are NOT cached so a Stripe outage
 * does not permanently blank out pricing.
 */
@Component
@Slf4j
public class StripePriceResolver {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * @return the plain decimal amount ("20", "199.5"), or the raw price id when
     * Stripe is unreachable (the UI renders that fallback as "Paid"), or null for
     * blank ids (rendered as "Free").
     */
    public String displayAmount(String stripePriceId) {
        if (stripePriceId == null || stripePriceId.isBlank()) return null;

        String cached = cache.get(stripePriceId);
        if (cached != null) return cached;

        try {
            Price price = Price.retrieve(stripePriceId);
            Long unitAmount = price.getUnitAmount();
            if (unitAmount == null) return stripePriceId;

            String amount = BigDecimal.valueOf(unitAmount, 2).stripTrailingZeros().toPlainString();
            cache.put(stripePriceId, amount);
            return amount;
        } catch (Exception e) {
            log.warn("Could not resolve Stripe price {}: {}", stripePriceId, e.getMessage());
            return stripePriceId;
        }
    }
}
