package com.firstclub.membership.tier.rule;

import com.firstclub.membership.catalog.domain.ComparisonOperator;
import com.firstclub.membership.catalog.domain.CriteriaMatchMode;
import com.firstclub.membership.catalog.domain.CriterionType;
import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.catalog.domain.TierEligibilityMode;
import com.firstclub.membership.tier.activity.UserActivity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TierEligibilityEvaluatorTest {

    private final TierEligibilityEvaluator evaluator = new TierEligibilityEvaluator(
            List.of(new OrderCountRule(), new MonthlyOrderValueRule(), new CohortRule()));

    private static MembershipTier tier(TierEligibilityMode mode, CriteriaMatchMode match) {
        return new MembershipTier("GOLD", "Gold", 2, mode, match, true);
    }

    private static UserActivity activity(int orders, int monthlyValue, Set<String> cohorts) {
        return new UserActivity("u1", orders, BigDecimal.valueOf(monthlyValue), cohorts);
    }

    @Test
    void openTier_alwaysQualifies_evenWithNoActivity() {
        MembershipTier silver = new MembershipTier(
                "SILVER", "Silver", 1, TierEligibilityMode.OPEN, CriteriaMatchMode.ANY, true);
        assertThat(evaluator.qualifies(silver, List.of(), UserActivity.empty("u1"))).isTrue();
    }

    @Test
    void criteriaBasedTier_withNoCriteria_isUnreachable() {
        MembershipTier gold = tier(TierEligibilityMode.CRITERIA_BASED, CriteriaMatchMode.ANY);
        assertThat(evaluator.qualifies(gold, List.of(), activity(100, 100000, Set.of()))).isFalse();
    }

    @Test
    void anyMatch_qualifiesWhenOneCriterionMet() {
        MembershipTier gold = tier(TierEligibilityMode.CRITERIA_BASED, CriteriaMatchMode.ANY);
        List<TierCriterion> criteria = List.of(
                new TierCriterion(gold, CriterionType.ORDER_COUNT, ComparisonOperator.GTE, BigDecimal.valueOf(5), null),
                new TierCriterion(gold, CriterionType.MONTHLY_ORDER_VALUE, ComparisonOperator.GTE, BigDecimal.valueOf(5000), null));

        // meets order count only -> ANY qualifies
        assertThat(evaluator.qualifies(gold, criteria, activity(6, 0, Set.of()))).isTrue();
    }

    @Test
    void anyMatch_failsWhenNoCriterionMet() {
        MembershipTier gold = tier(TierEligibilityMode.CRITERIA_BASED, CriteriaMatchMode.ANY);
        List<TierCriterion> criteria = List.of(
                new TierCriterion(gold, CriterionType.ORDER_COUNT, ComparisonOperator.GTE, BigDecimal.valueOf(5), null),
                new TierCriterion(gold, CriterionType.COHORT, null, null, "EARLY_ADOPTER"));

        assertThat(evaluator.qualifies(gold, criteria, activity(2, 0, Set.of("OTHER")))).isFalse();
    }

    @Test
    void allMatch_requiresEveryCriterion() {
        MembershipTier platinum = new MembershipTier(
                "PLATINUM", "Platinum", 3, TierEligibilityMode.CRITERIA_BASED, CriteriaMatchMode.ALL, true);
        List<TierCriterion> criteria = List.of(
                new TierCriterion(platinum, CriterionType.ORDER_COUNT, ComparisonOperator.GTE, BigDecimal.valueOf(15), null),
                new TierCriterion(platinum, CriterionType.MONTHLY_ORDER_VALUE, ComparisonOperator.GTE, BigDecimal.valueOf(20000), null));

        assertThat(evaluator.qualifies(platinum, criteria, activity(20, 25000, Set.of()))).isTrue();
        assertThat(evaluator.qualifies(platinum, criteria, activity(20, 19999, Set.of()))).isFalse(); // value short
        assertThat(evaluator.qualifies(platinum, criteria, activity(14, 25000, Set.of()))).isFalse(); // orders short
    }

    @Test
    void cohortCriterion_matchesMembership() {
        MembershipTier gold = tier(TierEligibilityMode.CRITERIA_BASED, CriteriaMatchMode.ANY);
        List<TierCriterion> criteria = List.of(
                new TierCriterion(gold, CriterionType.COHORT, null, null, "EARLY_ADOPTER"));

        assertThat(evaluator.qualifies(gold, criteria, activity(0, 0, Set.of("EARLY_ADOPTER")))).isTrue();
        assertThat(evaluator.qualifies(gold, criteria, activity(0, 0, Set.of("REGULAR")))).isFalse();
    }
}
