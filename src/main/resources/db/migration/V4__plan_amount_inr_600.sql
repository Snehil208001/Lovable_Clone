-- Pro plan Cashfree / display amount: ₹600 / month.

UPDATE plan
SET amount_inr = 600
WHERE active = true
  AND (amount_inr IS NULL OR amount_inr <= 0 OR amount_inr IN (20, 699, 999));
