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
 * A membership plan = a billing cadence (Monthly / Quarterly / Yearly).
 * The plan answers "how often do I pay"; price is resolved per (plan, tier) elsewhere.
 */
@Entity
@Table(name = "membership_plan")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private BillingCadence cadence;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private boolean active = true;

    protected MembershipPlan() {
        // for JPA
    }

    public MembershipPlan(BillingCadence cadence, String displayName, boolean active) {
        this.cadence = cadence;
        this.displayName = displayName;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public BillingCadence getCadence() {
        return cadence;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBillingMonths() {
        return cadence.getMonths();
    }

    public boolean isActive() {
        return active;
    }
}
