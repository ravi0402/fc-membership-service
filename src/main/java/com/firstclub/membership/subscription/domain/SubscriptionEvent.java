package com.firstclub.membership.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Append-only audit record of a change to a subscription. Immutable once written — there
 * are no setters — giving a tamper-evident membership history.
 */
@Entity
@Table(name = "subscription_event",
        indexes = @Index(name = "idx_event_user", columnList = "user_id"))
public class SubscriptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionEventType type;

    @Column(name = "from_tier", length = 30)
    private String fromTier;

    @Column(name = "to_tier", length = 30)
    private String toTier;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SubscriptionEvent() {
        // for JPA
    }

    public SubscriptionEvent(Long subscriptionId, String userId, SubscriptionEventType type,
                             String fromTier, String toTier, String note) {
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.type = type;
        this.fromTier = fromTier;
        this.toTier = toTier;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public String getUserId() {
        return userId;
    }

    public SubscriptionEventType getType() {
        return type;
    }

    public String getFromTier() {
        return fromTier;
    }

    public String getToTier() {
        return toTier;
    }

    public String getNote() {
        return note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
