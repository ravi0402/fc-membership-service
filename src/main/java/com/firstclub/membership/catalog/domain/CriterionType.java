package com.firstclub.membership.catalog.domain;

/** The kind of activity a tier-eligibility criterion measures. */
public enum CriterionType {
    /** Number of orders placed in the measured window. */
    ORDER_COUNT,
    /** Total order value within a month. */
    MONTHLY_ORDER_VALUE,
    /** Membership of a named cohort (e.g. "EARLY_ADOPTER"). */
    COHORT
}
