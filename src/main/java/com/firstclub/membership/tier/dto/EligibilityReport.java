package com.firstclub.membership.tier.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** A user's tier-eligibility across all tiers, with the highest one they qualify for. */
public record EligibilityReport(
        String userId,
        ActivitySnapshot activity,
        List<TierEligibilityResult> tiers,
        String recommendedTierCode) {

    /** Echo of the activity the evaluation was based on. */
    public record ActivitySnapshot(int orderCount, BigDecimal monthlyOrderValue, Set<String> cohorts) {
    }
}
