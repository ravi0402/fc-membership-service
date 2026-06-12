package com.firstclub.membership.tier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Set;

/** Demo seam: set a user's activity snapshot to exercise the tier rule engine. */
public record ActivityRequest(
        @NotNull @PositiveOrZero Integer orderCount,
        @NotNull @PositiveOrZero BigDecimal monthlyOrderValue,
        Set<String> cohorts) {
}
