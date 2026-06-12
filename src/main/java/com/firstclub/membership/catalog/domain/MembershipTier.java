package com.firstclub.membership.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A benefit tier (Silver / Gold / Platinum). {@code level} gives a total ordering used
 * to decide upgrade vs downgrade direction and to pick the "highest qualified" tier.
 */
@Entity
@Table(name = "membership_tier")
public class MembershipTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false)
    private String displayName;

    /** Rank within the tier ladder; higher = better. Unique across active tiers. */
    @Column(nullable = false, unique = true)
    private int level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TierEligibilityMode eligibilityMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CriteriaMatchMode criteriaMatchMode;

    @Column(nullable = false)
    private boolean active = true;

    protected MembershipTier() {
        // for JPA
    }

    public MembershipTier(String code, String displayName, int level,
                          TierEligibilityMode eligibilityMode,
                          CriteriaMatchMode criteriaMatchMode, boolean active) {
        this.code = code;
        this.displayName = displayName;
        this.level = level;
        this.eligibilityMode = eligibilityMode;
        this.criteriaMatchMode = criteriaMatchMode;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public TierEligibilityMode getEligibilityMode() {
        return eligibilityMode;
    }

    public CriteriaMatchMode getCriteriaMatchMode() {
        return criteriaMatchMode;
    }

    public boolean isActive() {
        return active;
    }
}
