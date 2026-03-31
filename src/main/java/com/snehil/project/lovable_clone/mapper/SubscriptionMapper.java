package com.snehil.project.lovable_clone.mapper;

import com.snehil.project.lovable_clone.dto.subscriptions.PlanResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.SubscriptionResponse;
import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    @Mapping(target = "price", source = "stripePriceId")
    PlanResponse toPlanResponse(Plan plan);
}
