package com.firstclub.membership.catalog.service;

import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierBenefit;
import com.firstclub.membership.catalog.dto.BenefitView;
import com.firstclub.membership.catalog.repository.TierBenefitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Resolves the effective set of perks for a tier from {@link TierBenefit} rows,
 * applying per-tier value overrides on top of benefit defaults. Single source of truth
 * shared by the catalogue view and the user's "my benefits" view.
 */
@Service
@Transactional(readOnly = true)
public class BenefitResolver {

    private final TierBenefitRepository tierBenefitRepository;

    public BenefitResolver(TierBenefitRepository tierBenefitRepository) {
        this.tierBenefitRepository = tierBenefitRepository;
    }

    public List<BenefitView> effectiveBenefits(MembershipTier tier) {
        return tierBenefitRepository.findByTierWithBenefit(tier).stream()
                .filter(tb -> tb.getBenefit().isActive())
                .map(tb -> new BenefitView(
                        tb.getBenefit().getCode(),
                        tb.getBenefit().getType(),
                        tb.getBenefit().getDescription(),
                        tb.effectiveValue()))
                .toList();
    }
}
