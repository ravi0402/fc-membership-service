package com.firstclub.membership.tier.rule;

import com.firstclub.membership.catalog.domain.CriterionType;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.tier.activity.UserActivity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Qualifies on number of orders, e.g. "orders >= 5". */
@Component
public class OrderCountRule implements TierEligibilityRule {

    @Override
    public CriterionType type() {
        return CriterionType.ORDER_COUNT;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, UserActivity activity) {
        return criterion.getOperator().test(
                BigDecimal.valueOf(activity.orderCount()), criterion.getThreshold());
    }
}
