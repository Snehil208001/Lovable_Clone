package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.subscriptions.PlanLimitsResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.UsageTodayResponse;
import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.entity.Subscription;
import com.snehil.project.lovable_clone.entity.UsageLog;
import com.snehil.project.lovable_clone.enums.SubscriptionStatus;
import com.snehil.project.lovable_clone.error.BadRequestException;
import com.snehil.project.lovable_clone.repository.SubscriptionRepository;
import com.snehil.project.lovable_clone.repository.UsageLogRepository;
import com.snehil.project.lovable_clone.repository.UserRepository;
import com.snehil.project.lovable_clone.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsageServiceImpl implements UsageService {

    private static final String AI_GENERATION_ACTION = "AI_GENERATION";
    private static final String FREE_PLAN_NAME = "Free";
    private static final int FREE_TIER_TOKENS_PER_DAY = 10_000;
    private static final int FREE_TIER_MAX_PREVIEWS = 1;
    // Keep in sync with SubscriptionServiceImpl.FREE_TIER_PROJECTS_ALLOWED
    private static final int FREE_TIER_MAX_PROJECTS = 1;

    private static final Set<SubscriptionStatus> CURRENT_SUBSCRIPTION_STATUSES = Set.of(
            SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE, SubscriptionStatus.TRAILING
    );

    private final UsageLogRepository usageLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UsageTodayResponse getTodayUsageOfUser(Long userId) {
        int tokensUsed = getTokensUsedToday(userId);
        Optional<Plan> plan = resolvePlan(userId);

        Integer tokensLimit = plan.map(Plan::getMaxTokensPerDay).orElse(FREE_TIER_TOKENS_PER_DAY);
        Integer previewsLimit = plan.map(Plan::getMaxPreviews).orElse(FREE_TIER_MAX_PREVIEWS);

        return new UsageTodayResponse(tokensUsed, tokensLimit, 0, previewsLimit);
    }

    @Override
    @Transactional
    public void recordTokenUsage(Long userId, int actualTokens) {
        UsageLog usageLog = usageLogRepository.findByUserIdAndDate(userId, LocalDate.now())
                .orElseGet(() -> UsageLog.builder()
                        .user(userRepository.getReferenceById(userId))
                        .action(AI_GENERATION_ACTION)
                        .tokensUser(0)
                        .date(LocalDate.now())
                        .build());

        int currentTokens = usageLog.getTokensUser() != null ? usageLog.getTokensUser() : 0;
        usageLog.setTokensUser(currentTokens + actualTokens);
        usageLogRepository.save(usageLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanLimitsResponse getCurrentSubscriptionLimitsOfUser(Long userId) {
        return resolvePlan(userId)
                .map(plan -> new PlanLimitsResponse(
                        plan.getName(),
                        plan.getMaxTokensPerDay(),
                        plan.getMaxProjects(),
                        plan.getUnlimitedAi()
                ))
                .orElseGet(() -> new PlanLimitsResponse(
                        FREE_PLAN_NAME,
                        FREE_TIER_TOKENS_PER_DAY,
                        FREE_TIER_MAX_PROJECTS,
                        false
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public void checkDailyTokenLimit(Long userId) {
        Optional<Plan> plan = resolvePlan(userId);

        if (plan.map(Plan::getUnlimitedAi).filter(Boolean.TRUE::equals).isPresent()) {
            return;
        }

        int tokensLimit = plan.map(Plan::getMaxTokensPerDay).orElse(FREE_TIER_TOKENS_PER_DAY);
        int tokensUsed = getTokensUsedToday(userId);

        if (tokensUsed >= tokensLimit) {
            throw new BadRequestException(
                    "Daily AI token limit reached (" + tokensUsed + "/" + tokensLimit + "). Upgrade your plan or try again tomorrow."
            );
        }
    }

    private int getTokensUsedToday(Long userId) {
        return usageLogRepository.findByUserIdAndDate(userId, LocalDate.now())
                .map(UsageLog::getTokensUser)
                .orElse(0);
    }

    private Optional<Plan> resolvePlan(Long userId) {
        return subscriptionRepository.findByUserIdAndStatusIn(userId, CURRENT_SUBSCRIPTION_STATUSES)
                .map(Subscription::getPlan);
    }
}
