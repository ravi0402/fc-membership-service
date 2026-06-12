package com.firstclub.membership.catalog.dto;

import com.firstclub.membership.catalog.domain.CriteriaMatchMode;
import com.firstclub.membership.catalog.domain.TierEligibilityMode;

import java.util.List;

/** A tier with the perks it unlocks and the criteria (if any) required to reach it. */
public record TierView(
        String code,
        String displayName,
        int level,
        TierEligibilityMode eligibilityMode,
        CriteriaMatchMode criteriaMatchMode,
        List<BenefitView> benefits,
        List<String> criteria) {
}
