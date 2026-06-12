package com.firstclub.membership.subscription.dto;

import com.firstclub.membership.catalog.domain.BillingCadence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to subscribe to a plan at a tier. */
public record SubscribeRequest(
        @NotNull BillingCadence planCadence,
        @NotBlank String tierCode,
        boolean autoRenew) {
}
