package com.firstclub.membership.subscription.domain;

import com.firstclub.membership.catalog.domain.MembershipPlan;
import com.firstclub.membership.catalog.domain.MembershipTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A user's membership: a (plan, tier) binding over a time window.
 *
 * <p>Concurrency safety is built into the entity:
 * <ul>
 *   <li>{@code @Version} optimistic locking guards against lost updates on concurrent
 *       upgrade/downgrade/cancel.</li>
 *   <li>{@code activeUserKey} holds the userId while ACTIVE and is null otherwise; its
 *       UNIQUE constraint enforces "at most one active subscription per user" in a
 *       DB-agnostic way (multiple NULLs are allowed).</li>
 * </ul>
 *
 * State changes go through intent-revealing methods; the audit trail is kept separately
 * and append-only via {@link SubscriptionEvent}.
 */
@Entity
@Table(name = "membership_subscription",
        indexes = @Index(name = "idx_subscription_user", columnList = "user_id"))
public class MembershipSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MembershipPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MembershipTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean autoRenew;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePaid;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    /** Holds userId while ACTIVE (unique), null otherwise — enforces one active membership. */
    @Column(name = "active_user_key", unique = true)
    private String activeUserKey;

    @Version
    private long version;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected MembershipSubscription() {
        // for JPA
    }

    public MembershipSubscription(String userId, MembershipPlan plan, MembershipTier tier,
                                  LocalDate startDate, LocalDate endDate, boolean autoRenew,
                                  BigDecimal pricePaid, String currency, String idempotencyKey) {
        this.userId = userId;
        this.plan = plan;
        this.tier = tier;
        this.status = SubscriptionStatus.ACTIVE;
        this.startDate = startDate;
        this.endDate = endDate;
        this.autoRenew = autoRenew;
        this.pricePaid = pricePaid;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.activeUserKey = userId;
    }

    // ---- State transitions ---------------------------------------------------------

    public void changeTier(MembershipTier newTier) {
        ensureActive();
        this.tier = newTier;
    }

    public void cancel() {
        ensureActive();
        this.status = SubscriptionStatus.CANCELLED;
        this.activeUserKey = null;
    }

    public void expire() {
        ensureActive();
        this.status = SubscriptionStatus.EXPIRED;
        this.activeUserKey = null;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    private void ensureActive() {
        if (!isActive()) {
            throw new IllegalStateException("Subscription " + id + " is not active (" + status + ")");
        }
    }

    // ---- Accessors -----------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public MembershipPlan getPlan() {
        return plan;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public BigDecimal getPricePaid() {
        return pricePaid;
    }

    public String getCurrency() {
        return currency;
    }

    public long getVersion() {
        return version;
    }
}
