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

import java.math.BigDecimal;

/**
 * One cell of the price matrix: the price of a given {@link MembershipPlan} at a given
 * {@link MembershipTier}. Keeping price as a function of (plan, tier) lets cadence and
 * benefit level vary the cost independently.
 */
@Entity
@Table(name = "membership_plan_price",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "tier_id"}))
public class MembershipPlanPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MembershipPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MembershipTier tier;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    protected MembershipPlanPrice() {
        // for JPA
    }

    public MembershipPlanPrice(MembershipPlan plan, MembershipTier tier, BigDecimal amount, String currency) {
        this.plan = plan;
        this.tier = tier;
        this.amount = amount;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public MembershipPlan getPlan() {
        return plan;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
