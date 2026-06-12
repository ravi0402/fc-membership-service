package com.firstclub.membership.tier.service;

import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.catalog.repository.TierCriterionRepository;
import com.firstclub.membership.catalog.service.CatalogService;
import com.firstclub.membership.tier.activity.UserActivity;
import com.firstclub.membership.tier.activity.UserActivityProvider;
import com.firstclub.membership.tier.dto.EligibilityReport;
import com.firstclub.membership.tier.dto.TierEligibilityResult;
import com.firstclub.membership.tier.rule.TierEligibilityEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Read-only tier eligibility: evaluates a user's activity against every tier's criteria.
 * Depended on by the subscription module to gate upgrades to gated tiers and to drive
 * activity-based auto-tiering. Holds no dependency on the subscription module (acyclic).
 */
@Service
@Transactional(readOnly = true)
public class TierEligibilityService {

    private final CatalogService catalogService;
    private final TierCriterionRepository criterionRepository;
    private final TierEligibilityEvaluator evaluator;
    private final UserActivityProvider activityProvider;

    public TierEligibilityService(CatalogService catalogService,
                                  TierCriterionRepository criterionRepository,
                                  TierEligibilityEvaluator evaluator,
                                  UserActivityProvider activityProvider) {
        this.catalogService = catalogService;
        this.criterionRepository = criterionRepository;
        this.evaluator = evaluator;
        this.activityProvider = activityProvider;
    }

    /** Full per-tier report plus the highest tier the user currently qualifies for. */
    public EligibilityReport evaluate(String userId) {
        UserActivity activity = activityProvider.getActivity(userId);
        List<MembershipTier> tiers = catalogService.activeTiersByLevel();

        List<TierEligibilityResult> results = tiers.stream()
                .map(tier -> toResult(tier, activity))
                .toList();

        String recommended = results.stream()
                .filter(TierEligibilityResult::eligible)
                .reduce((a, b) -> a.level() >= b.level() ? a : b)
                .map(TierEligibilityResult::tierCode)
                .orElse(null);

        return new EligibilityReport(
                userId,
                new EligibilityReport.ActivitySnapshot(
                        activity.orderCount(), activity.monthlyOrderValue(), activity.cohorts()),
                results,
                recommended);
    }

    /** Whether the user qualifies for a specific tier (used by upgrade gating). */
    public boolean isEligible(String userId, MembershipTier tier) {
        UserActivity activity = activityProvider.getActivity(userId);
        return evaluator.qualifies(tier, criterionRepository.findByTier(tier), activity);
    }

    /** The highest-level tier the user currently qualifies for, if any. */
    public Optional<MembershipTier> highestEligibleTier(String userId) {
        UserActivity activity = activityProvider.getActivity(userId);
        return catalogService.activeTiersByLevel().stream()
                .filter(tier -> evaluator.qualifies(tier, criterionRepository.findByTier(tier), activity))
                .reduce((a, b) -> a.getLevel() >= b.getLevel() ? a : b);
    }

    private TierEligibilityResult toResult(MembershipTier tier, UserActivity activity) {
        List<TierCriterion> criteria = criterionRepository.findByTier(tier);
        return new TierEligibilityResult(
                tier.getCode(),
                tier.getDisplayName(),
                tier.getLevel(),
                tier.getEligibilityMode(),
                evaluator.qualifies(tier, criteria, activity),
                criteria.stream().map(TierCriterion::describe).toList());
    }
}
