-- Cashfree support: INR amount on plans + which gateway owns a subscription.

alter table plan
    add column amount_inr numeric(12, 2);

alter table subscription
    add column payment_provider varchar(32);

update subscription
set payment_provider = 'STRIPE'
where payment_provider is null
  and stripe_subscription_id is not null;
