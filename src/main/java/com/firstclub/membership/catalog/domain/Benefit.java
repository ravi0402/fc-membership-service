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
 * A perk in the configurable catalogue. {@code defaultValue} carries type-specific data
 * as a string (e.g. "5" = 5% discount, "true" = boolean perk) so new perk shapes do not
 * require schema changes. Tiers attach benefits via {@link TierBenefit}.
 */
@Entity
@Table(name = "benefit")
public class Benefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BenefitType type;

    @Column(nullable = false)
    private String description;

    /** Type-specific default value, nullable for flag-style perks. */
    @Column(name = "default_value")
    private String defaultValue;

    @Column(nullable = false)
    private boolean active = true;

    protected Benefit() {
        // for JPA
    }

    public Benefit(String code, BenefitType type, String description, String defaultValue, boolean active) {
        this.code = code;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public BenefitType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean isActive() {
        return active;
    }
}
