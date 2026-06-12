package com.firstclub.membership.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Associates a {@link Benefit} with a {@link MembershipTier}, optionally overriding the
 * benefit's default value for that tier (e.g. Gold gets 10% but Platinum gets 15%).
 * This is the seam that makes "each tier unlocks additional perks" fully configurable.
 */
@Entity
@Table(name = "tier_benefit",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tier_id", "benefit_id"}))
public class TierBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MembershipTier tier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Benefit benefit;

    @Column(name = "value_override")
    private String valueOverride;

    protected TierBenefit() {
        // for JPA
    }

    public TierBenefit(MembershipTier tier, Benefit benefit, String valueOverride) {
        this.tier = tier;
        this.benefit = benefit;
        this.valueOverride = valueOverride;
    }

    public Long getId() {
        return id;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public String getValueOverride() {
        return valueOverride;
    }

    /** Effective value = per-tier override if present, else the benefit default. */
    public String effectiveValue() {
        return valueOverride != null ? valueOverride : benefit.getDefaultValue();
    }
}
