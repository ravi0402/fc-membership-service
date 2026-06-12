package com.firstclub.membership.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A configurable rule that gates a {@link MembershipTier}. Threshold criteria use
 * {@code operator} + {@code threshold}; cohort criteria use {@code cohort}. Stored as
 * data so thresholds are tuned without a redeploy; interpreted by the tier rule engine.
 */
@Entity
@Table(name = "tier_criterion")
public class TierCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MembershipTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CriterionType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private ComparisonOperator operator;

    @Column(precision = 12, scale = 2)
    private BigDecimal threshold;

    @Column(length = 50)
    private String cohort;

    protected TierCriterion() {
        // for JPA
    }

    public TierCriterion(MembershipTier tier, CriterionType type, ComparisonOperator operator,
                         BigDecimal threshold, String cohort) {
        this.tier = tier;
        this.type = type;
        this.operator = operator;
        this.threshold = threshold;
        this.cohort = cohort;
    }

    public Long getId() {
        return id;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public CriterionType getType() {
        return type;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public String getCohort() {
        return cohort;
    }

    /** Human-readable description used in API responses. */
    public String describe() {
        return switch (type) {
            case ORDER_COUNT -> "Orders " + operator + " " + threshold.toBigInteger();
            case MONTHLY_ORDER_VALUE -> "Monthly order value " + operator + " " + threshold;
            case COHORT -> "Belongs to cohort '" + cohort + "'";
        };
    }
}
