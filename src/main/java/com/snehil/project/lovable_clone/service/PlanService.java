package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.subscriptions.PlanResponse;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PlanService {
    List<PlanResponse> getAllActivePlans();
}
