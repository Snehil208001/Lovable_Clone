package com.snehil.project.lovable_clone.repository;

import com.snehil.project.lovable_clone.entity.Subscription;
import com.snehil.project.lovable_clone.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> statusSet);

    // Changed 'BY' to 'By'
    boolean existsByStripeSubscriptionId(String subscriptionId);

    // Changed 'Bu' to 'By'
    Optional<Subscription> findByStripeSubscriptionId(String gatewaySubscriptionId);
}