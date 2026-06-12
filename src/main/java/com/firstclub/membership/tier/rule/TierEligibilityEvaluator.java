package com.firstclub.membership.tier.rule;

import com.firstclub.membership.catalog.domain.CriteriaMatchMode;
import com.firstclub.membership.catalog.domain.CriterionType;
import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.catalog.domain.TierEligibilityMode;
import com.firstclub.membership.tier.activity.UserActivity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Decides whether a user qualifies for a tier. Aggregates the per-type
 * {@link TierEligibilityRule} strategies (injected by Spring — the Factory-via-DI
 * pattern) and applies the tier's {@link CriteriaMatchMode} (ANY/ALL).
 */
@Component
public class TierEligibilityEvaluator {

    private final Map<CriterionType, TierEligibilityRule> rulesByType;

    public TierEligibilityEvaluator(List<TierEligibilityRule> rules) {
        this.rulesByType = rules.stream()
                .collect(Collectors.toUnmodifiableMap(TierEligibilityRule::type, Function.identity()));
    }

    /**
     * @return true if the user with the given activity qualifies for the tier. OPEN tiers
     *         always qualify; CRITERIA_BASED tiers must satisfy their criteria per match mode.
     */
    public boolean qualifies(MembershipTier tier, List<TierCriterion> criteria, UserActivity activity) {
        if (tier.getEligibilityMode() == TierEligibilityMode.OPEN) {
            return true;
        }
        if (criteria.isEmpty()) {
            return false; // gated tier with no configured way in
        }
        return tier.getCriteriaMatchMode() == CriteriaMatchMode.ALL
                ? criteria.stream().allMatch(c -> isSatisfied(c, activity))
                : criteria.stream().anyMatch(c -> isSatisfied(c, activity));
    }

    private boolean isSatisfied(TierCriterion criterion, UserActivity activity) {
        TierEligibilityRule rule = rulesByType.get(criterion.getType());
        if (rule == null) {
            throw new IllegalStateException("No eligibility rule registered for " + criterion.getType());
        }
        return rule.isSatisfied(criterion, activity);
    }
}
