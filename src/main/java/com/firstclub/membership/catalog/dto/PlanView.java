package com.firstclub.membership.catalog.dto;

import com.firstclub.membership.catalog.domain.BillingCadence;

import java.math.BigDecimal;
import java.util.List;

/** A plan (billing cadence) with its per-tier pricing. */
public record PlanView(
        BillingCadence cadence,
        String displayName,
        int billingMonths,
        List<TierPrice> pricing) {

    /** Price of this plan at a specific tier. */
    public record TierPrice(String tierCode, BigDecimal amount, String currency) {
    }
}
