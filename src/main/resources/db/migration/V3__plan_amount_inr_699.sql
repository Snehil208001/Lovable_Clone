-- Ensure active plans have Cashfree INR amount (₹699 Pro).
-- Safe to re-run conceptually: Flyway versions this once; initializer also syncs from env.

UPDATE plan
SET amount_inr = 699
WHERE active = true
  AND (amount_inr IS NULL OR amount_inr <= 0 OR amount_inr = 20);
