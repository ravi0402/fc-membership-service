package com.firstclub.membership.catalog.domain;

/**
 * How a user may reach a tier.
 *
 * <ul>
 *   <li>{@code OPEN} — anyone may select / purchase the tier directly.</li>
 *   <li>{@code CRITERIA_BASED} — the user must satisfy the tier's criteria (evaluated
 *       by the rule engine), or be auto-promoted by the system.</li>
 * </ul>
 */
public enum TierEligibilityMode {
    OPEN,
    CRITERIA_BASED
}
