package com.firstclub.membership.subscription.service;

import com.firstclub.membership.catalog.domain.MembershipPlan;
import com.firstclub.membership.catalog.domain.MembershipPlanPrice;
import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierEligibilityMode;
import com.firstclub.membership.catalog.dto.BenefitView;
import com.firstclub.membership.catalog.service.BenefitResolver;
import com.firstclub.membership.catalog.service.CatalogService;
import com.firstclub.membership.catalog.service.PricingService;
import com.firstclub.membership.common.exception.BusinessRuleException;
import com.firstclub.membership.common.exception.DuplicateActiveSubscriptionException;
import com.firstclub.membership.common.exception.ResourceNotFoundException;
import com.firstclub.membership.common.exception.TierNotEligibleException;
import com.firstclub.membership.subscription.domain.MembershipSubscription;
import com.firstclub.membership.subscription.domain.SubscriptionEvent;
import com.firstclub.membership.subscription.domain.SubscriptionEventType;
import com.firstclub.membership.subscription.dto.SubscribeRequest;
import com.firstclub.membership.subscription.dto.SubscriptionEventView;
import com.firstclub.membership.subscription.dto.SubscriptionView;
import com.firstclub.membership.subscription.repository.MembershipSubscriptionRepository;
import com.firstclub.membership.subscription.repository.SubscriptionEventRepository;
import com.firstclub.membership.tier.service.TierEligibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Orchestrates the membership lifecycle: subscribe, upgrade, downgrade, cancel, history,
 * benefits, and activity-driven auto-tiering. Write methods are transactional; gating of
 * moves to criteria-based tiers delegates to {@link TierEligibilityService}.
 */
@Service
@Transactional(readOnly = true)
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final MembershipSubscriptionRepository subscriptionRepository;
    private final SubscriptionEventRepository eventRepository;
    private final CatalogService catalogService;
    private final PricingService pricingService;
    private final TierEligibilityService eligibilityService;
    private final BenefitResolver benefitResolver;

    public SubscriptionService(MembershipSubscriptionRepository subscriptionRepository,
                               SubscriptionEventRepository eventRepository,
                               CatalogService catalogService,
                               PricingService pricingService,
                               TierEligibilityService eligibilityService,
                               BenefitResolver benefitResolver) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.catalogService = catalogService;
        this.pricingService = pricingService;
        this.eligibilityService = eligibilityService;
        this.benefitResolver = benefitResolver;
    }

    @Transactional
    public SubscriptionView subscribe(String userId, SubscribeRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            var replay = subscriptionRepository.findByIdempotencyKey(idempotencyKey);
            if (replay.isPresent()) {
                log.info("Idempotent replay of subscribe for user={} key={}", userId, idempotencyKey);
                return SubscriptionView.from(replay.get());
            }
        }
        subscriptionRepository.findActiveByUserId(userId).ifPresent(s -> {
            throw new DuplicateActiveSubscriptionException(userId);
        });

        MembershipPlan plan = catalogService.getPlanByCadence(request.planCadence());
        MembershipTier tier = requireActiveTier(request.tierCode());
        requireEligible(userId, tier);

        MembershipPlanPrice price = pricingService.priceFor(plan, tier);
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(plan.getBillingMonths());

        var subscription = new MembershipSubscription(userId, plan, tier, start, end,
                request.autoRenew(), price.getAmount(), price.getCurrency(), idempotencyKey);
        // Unique constraint on active_user_key turns a concurrent double-subscribe into a 409.
        var saved = subscriptionRepository.save(subscription);

        record(saved, SubscriptionEventType.SUBSCRIBED, null, tier.getCode(),
                "Subscribed to %s / %s".formatted(plan.getCadence(), tier.getCode()));
        log.info("user={} subscribed plan={} tier={} expires={}", userId, plan.getCadence(), tier.getCode(), end);
        return SubscriptionView.from(saved);
    }

    public SubscriptionView getCurrent(String userId) {
        return SubscriptionView.from(requireActiveSubscription(userId));
    }

    @Transactional
    public SubscriptionView upgrade(String userId, String targetTierCode) {
        return changeTier(userId, targetTierCode, true);
    }

    @Transactional
    public SubscriptionView downgrade(String userId, String targetTierCode) {
        return changeTier(userId, targetTierCode, false);
    }

    @Transactional
    public SubscriptionView cancel(String userId) {
        MembershipSubscription subscription = requireActiveSubscription(userId);
        subscription.cancel();
        record(subscription, SubscriptionEventType.CANCELLED, subscription.getTier().getCode(), null, "Cancelled");
        log.info("user={} cancelled subscription={}", userId, subscription.getId());
        return SubscriptionView.from(subscription);
    }

    /** Evaluate the user's activity and auto-apply the highest tier they qualify for. */
    @Transactional
    public SubscriptionView syncTierFromActivity(String userId) {
        MembershipSubscription subscription = requireActiveSubscription(userId);
        MembershipTier current = subscription.getTier();
        MembershipTier earned = eligibilityService.highestEligibleTier(userId).orElse(current);

        if (earned.getLevel() == current.getLevel()) {
            return SubscriptionView.from(subscription);
        }
        subscription.changeTier(earned);
        record(subscription, SubscriptionEventType.AUTO_TIER_CHANGE, current.getCode(), earned.getCode(),
                "Activity-based tier sync");
        log.info("user={} auto-tiered {} -> {}", userId, current.getCode(), earned.getCode());
        return SubscriptionView.from(subscription);
    }

    public List<SubscriptionEventView> history(String userId) {
        return eventRepository.findByUserIdOrderByOccurredAtDescIdDesc(userId).stream()
                .map(SubscriptionEventView::from)
                .toList();
    }

    public List<BenefitView> currentBenefits(String userId) {
        return benefitResolver.effectiveBenefits(requireActiveSubscription(userId).getTier());
    }

    // ---- internals -----------------------------------------------------------------

    private SubscriptionView changeTier(String userId, String targetTierCode, boolean upgrade) {
        MembershipSubscription subscription = requireActiveSubscription(userId);
        MembershipTier current = subscription.getTier();
        MembershipTier target = requireActiveTier(targetTierCode);

        if (target.getLevel() == current.getLevel()) {
            throw new BusinessRuleException("Already on tier " + target.getCode());
        }
        if (upgrade && target.getLevel() < current.getLevel()) {
            throw new BusinessRuleException(target.getCode() + " is not an upgrade from " + current.getCode());
        }
        if (!upgrade && target.getLevel() > current.getLevel()) {
            throw new BusinessRuleException(target.getCode() + " is not a downgrade from " + current.getCode());
        }
        if (upgrade) {
            requireEligible(userId, target); // gated only blocks upward moves
        }

        subscription.changeTier(target); // @Version guards concurrent tier changes -> 409
        var type = upgrade ? SubscriptionEventType.UPGRADED : SubscriptionEventType.DOWNGRADED;
        record(subscription, type, current.getCode(), target.getCode(),
                (upgrade ? "Upgraded " : "Downgraded ") + current.getCode() + " -> " + target.getCode());
        log.info("user={} {} {} -> {}", userId, type, current.getCode(), target.getCode());
        return SubscriptionView.from(subscription);
    }

    private void requireEligible(String userId, MembershipTier tier) {
        if (tier.getEligibilityMode() == TierEligibilityMode.CRITERIA_BASED
                && !eligibilityService.isEligible(userId, tier)) {
            throw new TierNotEligibleException(userId, tier.getCode());
        }
    }

    private MembershipTier requireActiveTier(String tierCode) {
        MembershipTier tier = catalogService.getTierByCode(tierCode);
        if (!tier.isActive()) {
            throw new BusinessRuleException("Tier is not available: " + tierCode);
        }
        return tier;
    }

    private MembershipSubscription requireActiveSubscription(String userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
    }

    private void record(MembershipSubscription s, SubscriptionEventType type,
                        String fromTier, String toTier, String note) {
        eventRepository.save(new SubscriptionEvent(s.getId(), s.getUserId(), type, fromTier, toTier, note));
    }
}
