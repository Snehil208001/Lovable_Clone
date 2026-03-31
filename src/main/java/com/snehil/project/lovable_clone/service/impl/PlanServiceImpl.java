package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.subscriptions.PlanResponse;
import com.snehil.project.lovable_clone.mapper.SubscriptionMapper;
import com.snehil.project.lovable_clone.repository.PlanRepository;
import com.snehil.project.lovable_clone.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    public List<PlanResponse> getAllActivePlans() {
        return planRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(subscriptionMapper::toPlanResponse)
                .toList();
    }
}
