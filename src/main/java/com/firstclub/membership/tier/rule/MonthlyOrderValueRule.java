package com.firstclub.membership.tier.rule;

import com.firstclub.membership.catalog.domain.CriterionType;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.tier.activity.UserActivity;
import org.springframework.stereotype.Component;

/** Qualifies on total order value within a month, e.g. "monthly value >= 5000". */
@Component
public class MonthlyOrderValueRule implements TierEligibilityRule {

    @Override
    public CriterionType type() {
        return CriterionType.MONTHLY_ORDER_VALUE;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, UserActivity activity) {
        return criterion.getOperator().test(activity.monthlyOrderValue(), criterion.getThreshold());
    }
}
