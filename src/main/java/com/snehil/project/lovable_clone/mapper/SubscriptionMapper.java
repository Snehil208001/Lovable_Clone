package com.snehil.project.lovable_clone.mapper;

import com.snehil.project.lovable_clone.dto.subscriptions.PlanResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.SubscriptionResponse;
import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.entity.Subscription;
import com.snehil.project.lovable_clone.service.impl.StripePriceResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

// Abstract class instead of interface so the Stripe price resolver can be injected:
// plans store only a Stripe price id, the display amount is looked up (and cached).
@Mapper(componentModel = "spring")
public abstract class SubscriptionMapper {

    @Autowired
    protected StripePriceResolver stripePriceResolver;

    public abstract SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    @Mapping(target = "price", source = "stripePriceId", qualifiedByName = "displayAmount")
    public abstract PlanResponse toPlanResponse(Plan plan);

    @Named("displayAmount")
    protected String displayAmount(String stripePriceId) {
        return stripePriceResolver.displayAmount(stripePriceId);
    }
}
