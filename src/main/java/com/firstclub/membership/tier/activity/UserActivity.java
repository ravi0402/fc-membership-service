package com.firstclub.membership.tier.activity;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Immutable snapshot of the activity signals the tier rule engine evaluates. Owned by an
 * external system (orders / CRM) and read through {@link UserActivityProvider}.
 *
 * @param userId            the user
 * @param orderCount        orders placed in the measured window
 * @param monthlyOrderValue total order value within the current month
 * @param cohorts           cohort tags the user belongs to (defensively copied)
 */
public record UserActivity(String userId, int orderCount, BigDecimal monthlyOrderValue, Set<String> cohorts) {

    public UserActivity {
        monthlyOrderValue = monthlyOrderValue == null ? BigDecimal.ZERO : monthlyOrderValue;
        cohorts = cohorts == null ? Set.of() : Set.copyOf(cohorts);
    }

    public static UserActivity empty(String userId) {
        return new UserActivity(userId, 0, BigDecimal.ZERO, Set.of());
    }
}
