package com.firstclub.membership.subscription.service;

import com.firstclub.membership.catalog.domain.BillingCadence;
import com.firstclub.membership.catalog.domain.CriteriaMatchMode;
import com.firstclub.membership.catalog.domain.MembershipPlan;
import com.firstclub.membership.catalog.domain.MembershipPlanPrice;
import com.firstclub.membership.catalog.domain.MembershipTier;
import com.firstclub.membership.catalog.domain.TierEligibilityMode;
import com.firstclub.membership.catalog.service.BenefitResolver;
import com.firstclub.membership.catalog.service.CatalogService;
import com.firstclub.membership.catalog.service.PricingService;
import com.firstclub.membership.common.exception.BusinessRuleException;
import com.firstclub.membership.common.exception.DuplicateActiveSubscriptionException;
import com.firstclub.membership.common.exception.TierNotEligibleException;
import com.firstclub.membership.subscription.domain.MembershipSubscription;
import com.firstclub.membership.subscription.domain.SubscriptionEvent;
import com.firstclub.membership.subscription.domain.SubscriptionEventType;
import com.firstclub.membership.subscription.domain.SubscriptionStatus;
import com.firstclub.membership.subscription.dto.SubscribeRequest;
import com.firstclub.membership.subscription.dto.SubscriptionView;
import com.firstclub.membership.subscription.repository.MembershipSubscriptionRepository;
import com.firstclub.membership.subscription.repository.SubscriptionEventRepository;
import com.firstclub.membership.tier.service.TierEligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock MembershipSubscriptionRepository subscriptionRepository;
    @Mock SubscriptionEventRepository eventRepository;
    @Mock CatalogService catalogService;
    @Mock PricingService pricingService;
    @Mock TierEligibilityService eligibilityService;
    @Mock BenefitResolver benefitResolver;

    @InjectMocks SubscriptionService service;

    private MembershipPlan monthly;
    private MembershipTier silver;
    private MembershipTier gold;

    @BeforeEach
    void setUp() {
        monthly = new MembershipPlan(BillingCadence.MONTHLY, "Monthly", true);
        silver = new MembershipTier("SILVER", "Silver", 1, TierEligibilityMode.OPEN, CriteriaMatchMode.ANY, true);
        gold = new MembershipTier("GOLD", "Gold", 2, TierEligibilityMode.CRITERIA_BASED, CriteriaMatchMode.ANY, true);
    }

    private MembershipSubscription activeSub(MembershipTier tier) {
        return new MembershipSubscription("u1", monthly, tier, LocalDate.now(), LocalDate.now().plusMonths(1),
                true, BigDecimal.valueOf(199), "INR", null);
    }

    @Test
    void subscribe_toOpenTier_succeedsAndRecordsEvent() {
        when(subscriptionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(subscriptionRepository.findActiveByUserId("u1")).thenReturn(Optional.empty());
        when(catalogService.getPlanByCadence(BillingCadence.MONTHLY)).thenReturn(monthly);
        when(catalogService.getTierByCode("SILVER")).thenReturn(silver);
        when(pricingService.priceFor(monthly, silver))
                .thenReturn(new MembershipPlanPrice(monthly, silver, BigDecimal.valueOf(199), "INR"));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SubscriptionView view = service.subscribe("u1",
                new SubscribeRequest(BillingCadence.MONTHLY, "SILVER", true), "key-1");

        assertThat(view.tierCode()).isEqualTo("SILVER");
        assertThat(view.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        ArgumentCaptor<SubscriptionEvent> event = ArgumentCaptor.forClass(SubscriptionEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getType()).isEqualTo(SubscriptionEventType.SUBSCRIBED);
    }

    @Test
    void subscribe_whenAlreadyActive_throwsDuplicate() {
        when(subscriptionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(subscriptionRepository.findActiveByUserId("u1")).thenReturn(Optional.of(activeSub(silver)));

        assertThatThrownBy(() -> service.subscribe("u1",
                new SubscribeRequest(BillingCadence.MONTHLY, "SILVER", true), "key-1"))
                .isInstanceOf(DuplicateActiveSubscriptionException.class);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribe_idempotentReplay_returnsExistingWithoutSaving() {
        when(subscriptionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(activeSub(silver)));

        SubscriptionView view = service.subscribe("u1",
                new SubscribeRequest(BillingCadence.MONTHLY, "SILVER", true), "key-1");

        assertThat(view.tierCode()).isEqualTo("SILVER");
        verify(subscriptionRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void subscribe_toGatedTier_whenNotEligible_throws() {
        when(subscriptionRepository.findActiveByUserId("u1")).thenReturn(Optional.empty());
        when(catalogService.getPlanByCadence(BillingCadence.MONTHLY)).thenReturn(monthly);
        when(catalogService.getTierByCode("GOLD")).thenReturn(gold);
        when(eligibilityService.isEligible("u1", gold)).thenReturn(false);

        assertThatThrownBy(() -> service.subscribe("u1",
                new SubscribeRequest(BillingCadence.MONTHLY, "GOLD", true), null))
                .isInstanceOf(TierNotEligibleException.class);
    }

    @Test
    void upgrade_toEligibleGatedTier_changesTierAndRecordsEvent() {
        MembershipSubscription sub = activeSub(silver);
        when(subscriptionRepository.findActiveByUserId("u1")).thenReturn(Optional.of(sub));
        when(catalogService.getTierByCode("GOLD")).thenReturn(gold);
        when(eligibilityService.isEligible("u1", gold)).thenReturn(true);

        SubscriptionView view = service.upgrade("u1", "GOLD");

        assertThat(view.tierCode()).isEqualTo("GOLD");
        assertThat(sub.getTier()).isEqualTo(gold);
        ArgumentCaptor<SubscriptionEvent> event = ArgumentCaptor.forClass(SubscriptionEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getType()).isEqualTo(SubscriptionEventType.UPGRADED);
    }

    @Test
    void upgrade_toLowerTier_isRejected() {
        when(subscriptionRepository.findActiveByUserId("u1")).thenReturn(Optional.of(activeSub(gold)));
        when(catalogService.getTierByCode("SILVER")).thenReturn(silver);

        assertThatThrownBy(() -> service.upgrade("u1", "SILVER"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void downgrade_toLowerTier_succeeds() {
        MembershipSubscription sub = activeSub(gold);
        when(subscriptionRepository.findActiveByUserId("u1")).thenReturn(Optional.of(sub));
        when(catalogService.getTierByCode("SILVER")).thenReturn(silver);

        SubscriptionView view = service.downgrade("u1", "SILVER");

        assertThat(view.tierCode()).isEqualTo("SILVER");
        assertThat(sub.getTier()).isEqualTo(silver);
    }

    @Test
    void cancel_marksCancelledAndRecordsEvent() {
        MembershipSubscription sub = activeSub(silver);
        when(subscriptionRepository.findActiveByUserId("u1")).thenReturn(Optional.of(sub));

        SubscriptionView view = service.cancel("u1");

        assertThat(view.status()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(sub.isActive()).isFalse();
    }
}
