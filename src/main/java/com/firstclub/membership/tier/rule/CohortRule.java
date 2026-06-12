package com.firstclub.membership.tier.rule;

import com.firstclub.membership.catalog.domain.CriterionType;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.tier.activity.UserActivity;
import org.springframework.stereotype.Component;

/** Qualifies on membership of a named cohort, e.g. "belongs to EARLY_ADOPTER". */
@Component
public class CohortRule implements TierEligibilityRule {

    @Override
    public CriterionType type() {
        return CriterionType.COHORT;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, UserActivity activity) {
        return criterion.getCohort() != null && activity.cohorts().contains(criterion.getCohort());
    }
}
