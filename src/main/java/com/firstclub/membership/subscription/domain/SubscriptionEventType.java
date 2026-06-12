package com.firstclub.membership.subscription.domain;

/** Type of an entry in the append-only subscription audit trail. */
public enum SubscriptionEventType {
    SUBSCRIBED,
    UPGRADED,
    DOWNGRADED,
    CANCELLED,
    RENEWED,
    AUTO_TIER_CHANGE,
    EXPIRED
}
