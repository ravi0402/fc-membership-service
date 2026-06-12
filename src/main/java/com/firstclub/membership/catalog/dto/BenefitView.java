package com.firstclub.membership.catalog.dto;

import com.firstclub.membership.catalog.domain.BenefitType;

/** A resolved perk as seen by clients (value already reflects any per-tier override). */
public record BenefitView(String code, BenefitType type, String description, String value) {
}
