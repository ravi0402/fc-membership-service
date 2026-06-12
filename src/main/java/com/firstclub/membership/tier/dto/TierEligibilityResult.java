package com.firstclub.membership.tier.dto;

import com.firstclub.membership.catalog.domain.TierEligibilityMode;

import java.util.List;

/** Whether a user qualifies for one tier, with the criteria that gate it. */
public record TierEligibilityResult(
        String tierCode,
        String displayName,
        int level,
        TierEligibilityMode eligibilityMode,
        boolean eligible,
        List<String> criteria) {
}
