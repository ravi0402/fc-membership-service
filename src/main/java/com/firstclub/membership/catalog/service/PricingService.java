package com.firstclub.membership.catalog.service;

import com.firstclub.membership.catalog.domain.MembershipPlan;
import com.firstclub.membership.catalog.domain.MembershipPlanPrice;
import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.repository.MembershipPlanPriceRepository;
import com.firstclub.membership.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the price of a (plan, tier) pairing from the price matrix. Isolated so future
 * pricing policy (promotions, proration) can be layered here without touching callers.
 */
@Service
@Transactional(readOnly = true)
public class PricingService {

    private final MembershipPlanPriceRepository priceRepository;

    public PricingService(MembershipPlanPriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    public MembershipPlanPrice priceFor(MembershipPlan plan, MembershipTier tier) {
        return priceRepository.findByPlanAndTier(plan, tier)
                .orElseThrow(() -> new BusinessRuleException(
                        "No price configured for plan %s at tier %s"
                                .formatted(plan.getCadence(), tier.getCode())));
    }
}
