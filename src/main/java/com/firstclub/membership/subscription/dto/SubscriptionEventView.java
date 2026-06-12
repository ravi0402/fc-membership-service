package com.firstclub.membership.subscription.dto;

import com.firstclub.membership.subscription.domain.SubscriptionEvent;
import com.firstclub.membership.subscription.domain.SubscriptionEventType;

import java.time.Instant;

/** A single entry in the membership history. */
public record SubscriptionEventView(
        SubscriptionEventType type,
        String fromTier,
        String toTier,
        String note,
        Instant occurredAt) {

    public static SubscriptionEventView from(SubscriptionEvent e) {
        return new SubscriptionEventView(e.getType(), e.getFromTier(), e.getToTier(),
                e.getNote(), e.getOccurredAt());
    }
}
