package com.firstclub.membership.subscription.web;

import com.firstclub.membership.catalog.dto.BenefitView;
import com.firstclub.membership.common.api.ApiResponse;
import com.firstclub.membership.subscription.dto.SubscribeRequest;
import com.firstclub.membership.subscription.dto.SubscriptionEventView;
import com.firstclub.membership.subscription.dto.SubscriptionView;
import com.firstclub.membership.subscription.dto.TierChangeRequest;
import com.firstclub.membership.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Membership lifecycle endpoints for a user. */
@RestController
@RequestMapping("/api/v1/users/{userId}")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/subscription")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubscriptionView> subscribe(
            @PathVariable String userId,
            @Valid @RequestBody SubscribeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.ok(subscriptionService.subscribe(userId, request, idempotencyKey));
    }

    @GetMapping("/subscription")
    public ApiResponse<SubscriptionView> current(@PathVariable String userId) {
        return ApiResponse.ok(subscriptionService.getCurrent(userId));
    }

    @PostMapping("/subscription/upgrade")
    public ApiResponse<SubscriptionView> upgrade(@PathVariable String userId,
                                                 @Valid @RequestBody TierChangeRequest request) {
        return ApiResponse.ok(subscriptionService.upgrade(userId, request.targetTierCode()));
    }

    @PostMapping("/subscription/downgrade")
    public ApiResponse<SubscriptionView> downgrade(@PathVariable String userId,
                                                   @Valid @RequestBody TierChangeRequest request) {
        return ApiResponse.ok(subscriptionService.downgrade(userId, request.targetTierCode()));
    }

    @PostMapping("/subscription/cancel")
    public ApiResponse<SubscriptionView> cancel(@PathVariable String userId) {
        return ApiResponse.ok(subscriptionService.cancel(userId));
    }

    @GetMapping("/subscription/history")
    public ApiResponse<List<SubscriptionEventView>> history(@PathVariable String userId) {
        return ApiResponse.ok(subscriptionService.history(userId));
    }

    @GetMapping("/benefits")
    public ApiResponse<List<BenefitView>> benefits(@PathVariable String userId) {
        return ApiResponse.ok(subscriptionService.currentBenefits(userId));
    }

    /** Evaluate activity and auto-apply the highest qualified tier to the active membership. */
    @PostMapping("/tiers/sync")
    public ApiResponse<SubscriptionView> syncTier(@PathVariable String userId) {
        return ApiResponse.ok(subscriptionService.syncTierFromActivity(userId));
    }
}
