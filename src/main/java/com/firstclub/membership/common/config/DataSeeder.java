package com.firstclub.membership.common.config;

import com.firstclub.membership.catalog.domain.Benefit;
import com.firstclub.membership.catalog.domain.BenefitType;
import com.firstclub.membership.catalog.domain.BillingCadence;
import com.firstclub.membership.catalog.domain.ComparisonOperator;
import com.firstclub.membership.catalog.domain.CriteriaMatchMode;
import com.firstclub.membership.catalog.domain.CriterionType;
import com.firstclub.membership.catalog.domain.MembershipPlan;
import com.firstclub.membership.catalog.domain.MembershipPlanPrice;
import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierBenefit;
import com.firstclub.membership.catalog.domain.TierCriterion;
import com.firstclub.membership.catalog.domain.TierEligibilityMode;
import com.firstclub.membership.catalog.repository.BenefitRepository;
import com.firstclub.membership.catalog.repository.MembershipPlanPriceRepository;
import com.firstclub.membership.catalog.repository.MembershipPlanRepository;
import com.firstclub.membership.catalog.repository.MembershipTierRepository;
import com.firstclub.membership.catalog.repository.TierBenefitRepository;
import com.firstclub.membership.catalog.repository.TierCriterionRepository;
import com.firstclub.membership.tier.activity.InMemoryUserActivityProvider;
import com.firstclub.membership.tier.activity.UserActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Seeds the reference catalogue (plans, tiers, benefits, price matrix, tier criteria) and
 * a little demo activity. Idempotent and DB-agnostic: runs only when the catalogue is
 * empty, so it works for both H2 and Postgres without migration tooling.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String INR = "INR";

    private final MembershipPlanRepository planRepo;
    private final MembershipTierRepository tierRepo;
    private final BenefitRepository benefitRepo;
    private final TierBenefitRepository tierBenefitRepo;
    private final MembershipPlanPriceRepository priceRepo;
    private final TierCriterionRepository criterionRepo;
    private final InMemoryUserActivityProvider activityProvider;

    public DataSeeder(MembershipPlanRepository planRepo, MembershipTierRepository tierRepo,
                      BenefitRepository benefitRepo, TierBenefitRepository tierBenefitRepo,
                      MembershipPlanPriceRepository priceRepo, TierCriterionRepository criterionRepo,
                      InMemoryUserActivityProvider activityProvider) {
        this.planRepo = planRepo;
        this.tierRepo = tierRepo;
        this.benefitRepo = benefitRepo;
        this.tierBenefitRepo = tierBenefitRepo;
        this.priceRepo = priceRepo;
        this.criterionRepo = criterionRepo;
        this.activityProvider = activityProvider;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedDemoActivity();
        if (planRepo.count() > 0) {
            return;
        }
        log.info("Seeding membership catalogue...");

        MembershipPlan monthly = planRepo.save(new MembershipPlan(BillingCadence.MONTHLY, "Monthly", true));
        MembershipPlan quarterly = planRepo.save(new MembershipPlan(BillingCadence.QUARTERLY, "Quarterly", true));
        MembershipPlan yearly = planRepo.save(new MembershipPlan(BillingCadence.YEARLY, "Yearly", true));

        MembershipTier silver = tierRepo.save(new MembershipTier(
                "SILVER", "Silver", 1, TierEligibilityMode.OPEN, CriteriaMatchMode.ANY, true));
        MembershipTier gold = tierRepo.save(new MembershipTier(
                "GOLD", "Gold", 2, TierEligibilityMode.CRITERIA_BASED, CriteriaMatchMode.ANY, true));
        MembershipTier platinum = tierRepo.save(new MembershipTier(
                "PLATINUM", "Platinum", 3, TierEligibilityMode.CRITERIA_BASED, CriteriaMatchMode.ALL, true));

        Benefit freeDelivery = benefitRepo.save(new Benefit(
                "FREE_DELIVERY", BenefitType.FREE_DELIVERY, "Free delivery on eligible orders", "true", true));
        Benefit discount = benefitRepo.save(new Benefit(
                "EXTRA_DISCOUNT", BenefitType.PERCENTAGE_DISCOUNT, "Extra % off on selected items", "5", true));
        Benefit deals = benefitRepo.save(new Benefit(
                "EXCLUSIVE_DEALS", BenefitType.EXCLUSIVE_DEALS, "Access to exclusive deals", "true", true));
        Benefit earlyAccess = benefitRepo.save(new Benefit(
                "EARLY_ACCESS", BenefitType.EARLY_ACCESS, "Early access to sales", "true", true));
        Benefit support = benefitRepo.save(new Benefit(
                "PRIORITY_SUPPORT", BenefitType.PRIORITY_SUPPORT, "Priority customer support", "true", true));

        // Each tier unlocks progressively richer (configurable) perks.
        tierBenefitRepo.save(new TierBenefit(silver, freeDelivery, null));
        tierBenefitRepo.save(new TierBenefit(silver, discount, "5"));

        tierBenefitRepo.save(new TierBenefit(gold, freeDelivery, null));
        tierBenefitRepo.save(new TierBenefit(gold, discount, "10"));
        tierBenefitRepo.save(new TierBenefit(gold, deals, null));
        tierBenefitRepo.save(new TierBenefit(gold, earlyAccess, null));

        tierBenefitRepo.save(new TierBenefit(platinum, freeDelivery, null));
        tierBenefitRepo.save(new TierBenefit(platinum, discount, "15"));
        tierBenefitRepo.save(new TierBenefit(platinum, deals, null));
        tierBenefitRepo.save(new TierBenefit(platinum, earlyAccess, null));
        tierBenefitRepo.save(new TierBenefit(platinum, support, null));

        // Price matrix: (plan, tier) -> amount.
        Map<MembershipTier, int[]> matrix = Map.of(
                silver, new int[]{199, 499, 1799},
                gold, new int[]{399, 999, 3499},
                platinum, new int[]{699, 1799, 5999});
        MembershipPlan[] plans = {monthly, quarterly, yearly};
        matrix.forEach((tier, prices) -> {
            for (int i = 0; i < plans.length; i++) {
                priceRepo.save(new MembershipPlanPrice(plans[i], tier, BigDecimal.valueOf(prices[i]), INR));
            }
        });

        // Gold: ANY of these. Platinum: ALL of these.
        criterionRepo.save(new TierCriterion(gold, CriterionType.ORDER_COUNT, ComparisonOperator.GTE, BigDecimal.valueOf(5), null));
        criterionRepo.save(new TierCriterion(gold, CriterionType.MONTHLY_ORDER_VALUE, ComparisonOperator.GTE, BigDecimal.valueOf(5000), null));
        criterionRepo.save(new TierCriterion(gold, CriterionType.COHORT, null, null, "EARLY_ADOPTER"));

        criterionRepo.save(new TierCriterion(platinum, CriterionType.ORDER_COUNT, ComparisonOperator.GTE, BigDecimal.valueOf(15), null));
        criterionRepo.save(new TierCriterion(platinum, CriterionType.MONTHLY_ORDER_VALUE, ComparisonOperator.GTE, BigDecimal.valueOf(20000), null));

        log.info("Catalogue seeded: {} plans, {} tiers, {} benefits",
                planRepo.count(), tierRepo.count(), benefitRepo.count());
    }

    private void seedDemoActivity() {
        // gold-eligible (orders >= 5); platinum-eligible (orders >= 15 AND value >= 20000)
        activityProvider.setActivity(new UserActivity("demo-user", 6, BigDecimal.valueOf(6000), Set.of()));
        activityProvider.setActivity(new UserActivity("vip-user", 20, BigDecimal.valueOf(25000), Set.of("EARLY_ADOPTER")));
    }
}
