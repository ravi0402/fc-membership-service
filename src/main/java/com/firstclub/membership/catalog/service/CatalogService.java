package com.firstclub.membership.catalog.service;

import com.firstclub.membership.catalog.domain.BillingCadence;
import com.firstclub.membership.catalog.domain.MembershipPlan;
import com.firstclub.membership.catalog.domain.MembershipPlanPrice;
import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.catalog.dto.BenefitView;
import com.firstclub.membership.catalog.dto.PlanView;
import com.firstclub.membership.catalog.dto.TierView;
import com.firstclub.membership.catalog.repository.BenefitRepository;
import com.firstclub.membership.catalog.repository.MembershipPlanPriceRepository;
import com.firstclub.membership.catalog.repository.MembershipPlanRepository;
import com.firstclub.membership.catalog.repository.MembershipTierRepository;
import com.firstclub.membership.catalog.repository.TierCriterionRepository;
import com.firstclub.membership.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-side access to the membership catalogue: plans (with pricing), tiers (with perks
 * and criteria), and benefits. Also exposes typed lookups used by other modules.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final MembershipPlanRepository planRepository;
    private final MembershipTierRepository tierRepository;
    private final BenefitRepository benefitRepository;
    private final MembershipPlanPriceRepository priceRepository;
    private final TierCriterionRepository criterionRepository;
    private final BenefitResolver benefitResolver;

    public CatalogService(MembershipPlanRepository planRepository,
                          MembershipTierRepository tierRepository,
                          BenefitRepository benefitRepository,
                          MembershipPlanPriceRepository priceRepository,
                          TierCriterionRepository criterionRepository,
                          BenefitResolver benefitResolver) {
        this.planRepository = planRepository;
        this.tierRepository = tierRepository;
        this.benefitRepository = benefitRepository;
        this.priceRepository = priceRepository;
        this.criterionRepository = criterionRepository;
        this.benefitResolver = benefitResolver;
    }

    // ---- Lookups (used by subscription / tier modules) -----------------------------

    public MembershipPlan getPlanByCadence(BillingCadence cadence) {
        return planRepository.findByCadence(cadence)
                .orElseThrow(() -> ResourceNotFoundException.of("Plan", cadence));
    }

    public MembershipTier getTierByCode(String code) {
        return tierRepository.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Tier", code));
    }

    public List<MembershipTier> activeTiersByLevel() {
        return tierRepository.findByActiveTrueOrderByLevelAsc();
    }

    // ---- Catalogue views -----------------------------------------------------------

    public List<PlanView> listPlans() {
        Map<Long, List<MembershipPlanPrice>> byPlanId = priceRepository.findAllWithPlanAndTier().stream()
                .collect(Collectors.groupingBy(p -> p.getPlan().getId()));

        return planRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(plan -> new PlanView(
                        plan.getCadence(),
                        plan.getDisplayName(),
                        plan.getBillingMonths(),
                        byPlanId.getOrDefault(plan.getId(), List.of()).stream()
                                .sorted(Comparator.comparingInt(p -> p.getTier().getLevel()))
                                .map(p -> new PlanView.TierPrice(
                                        p.getTier().getCode(), p.getAmount(), p.getCurrency()))
                                .toList()))
                .toList();
    }

    public List<TierView> listTiers() {
        return activeTiersByLevel().stream().map(this::toTierView).toList();
    }

    public List<BenefitView> listBenefits() {
        return benefitRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(b -> new BenefitView(b.getCode(), b.getType(), b.getDescription(), b.getDefaultValue()))
                .toList();
    }

    private TierView toTierView(MembershipTier tier) {
        List<String> criteria = criterionRepository.findByTier(tier).stream()
                .map(TierCriterion::describe)
                .toList();
        return new TierView(
                tier.getCode(),
                tier.getDisplayName(),
                tier.getLevel(),
                tier.getEligibilityMode(),
                tier.getCriteriaMatchMode(),
                benefitResolver.effectiveBenefits(tier),
                criteria);
    }
}
