package com.firstclub.membership.subscription.dto;

import com.firstclub.membership.catalog.domain.BillingCadence;
import com.firstclub.membership.subscription.domain.MembershipSubscription;
import com.firstclub.membership.subscription.domain.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Client-facing view of a membership, including computed days-to-expiry. */
public record SubscriptionView(
        Long id,
        String userId,
        BillingCadence planCadence,
        String planName,
        String tierCode,
        String tierName,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate endDate,
        long daysRemaining,
        boolean autoRenew,
        BigDecimal pricePaid,
        String currency) {

    /** Map an entity to its view. Must be called inside the transaction (lazy plan/tier). */
    public static SubscriptionView from(MembershipSubscription s) {
        long remaining = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), s.getEndDate()));
        return new SubscriptionView(
                s.getId(),
                s.getUserId(),
                s.getPlan().getCadence(),
                s.getPlan().getDisplayName(),
                s.getTier().getCode(),
                s.getTier().getDisplayName(),
                s.getStatus(),
                s.getStartDate(),
                s.getEndDate(),
                remaining,
                s.isAutoRenew(),
                s.getPricePaid(),
                s.getCurrency());
    }
}
