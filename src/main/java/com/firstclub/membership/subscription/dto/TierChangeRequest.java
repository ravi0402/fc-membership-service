package com.firstclub.membership.subscription.dto;

import jakarta.validation.constraints.NotBlank;

/** Request to move the active subscription to a different tier (upgrade or downgrade). */
public record TierChangeRequest(@NotBlank String targetTierCode) {
}
