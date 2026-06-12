package com.firstclub.membership.catalog.domain;

/**
 * Category of a perk. New perk types are added here; the value semantics live in the
 * {@code defaultValue}/{@code valueOverride} string (e.g. "10" for a 10% discount,
 * "true" for a boolean perk).
 */
public enum BenefitType {
    FREE_DELIVERY,
    PERCENTAGE_DISCOUNT,
    EXCLUSIVE_DEALS,
    EARLY_ACCESS,
    PRIORITY_SUPPORT
}
