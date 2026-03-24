package com.snehil.project.lovable_clone.controller;

import com.snehil.project.lovable_clone.dto.subscriptions.PlanLimitsResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.UsageTodayResponse;
import com.snehil.project.lovable_clone.security.JwtUserPrincipal;
import com.snehil.project.lovable_clone.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;

    @GetMapping("/today")
    public ResponseEntity<UsageTodayResponse> getTodayUsage(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(usageService.getTodayUsageOfUser(currentUser.userId()));
    }

    @GetMapping("/limits")
    public ResponseEntity<PlanLimitsResponse> getPlanLimits(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(usageService.getCurrentSubscriptionLimitsOfUser(currentUser.userId()));
    }
}
