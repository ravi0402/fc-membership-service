package com.firstclub.membership.catalog.domain;

/**
 * Billing cadence of a membership plan. The {@code months} value generalises the
 * cadence so adding e.g. a half-yearly plan is a one-line change.
 */
public enum BillingCadence {
    MONTHLY(1),
    QUARTERLY(3),
    YEARLY(12);

    private final int months;

    BillingCadence(int months) {
        this.months = months;
    }

    public int getMonths() {
        return months;
    }
}
