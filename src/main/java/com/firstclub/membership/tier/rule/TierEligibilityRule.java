package com.firstclub.membership.tier.rule;

import com.firstclub.membership.catalog.domain.CriterionType;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.tier.activity.UserActivity;

/**
 * Strategy for evaluating a single kind of tier-eligibility criterion. One implementation
 * per {@link CriterionType}; the evaluator dispatches by {@link #type()}.
 *
 * <p>Adding a new criterion type = add one bean implementing this interface. No existing
 * code changes — Spring injects all rules into the evaluator.
 */
public interface TierEligibilityRule {

    CriterionType type();

    boolean isSatisfied(TierCriterion criterion, UserActivity activity);
}
