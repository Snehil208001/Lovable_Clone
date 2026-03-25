package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutRequest;
import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.PostalResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.SubscriptionResponse;
import org.jspecify.annotations.Nullable;

public interface SubscriptionService {
    SubscriptionResponse getCurrentSubscription(Long userId);

}
