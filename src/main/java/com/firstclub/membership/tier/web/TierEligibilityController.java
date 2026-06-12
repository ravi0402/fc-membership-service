package com.firstclub.membership.tier.web;

import com.firstclub.membership.common.api.ApiResponse;
import com.firstclub.membership.tier.activity.InMemoryUserActivityProvider;
import com.firstclub.membership.tier.activity.UserActivity;
import com.firstclub.membership.tier.dto.ActivityRequest;
import com.firstclub.membership.tier.dto.EligibilityReport;
import com.firstclub.membership.tier.service.TierEligibilityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Tier-eligibility endpoints plus a demo seam to set user activity. The activity seam
 * stands in for a real Order-service integration so the rule engine is demo-able.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}")
public class TierEligibilityController {

    private final TierEligibilityService eligibilityService;
    private final InMemoryUserActivityProvider activityProvider;

    public TierEligibilityController(TierEligibilityService eligibilityService,
                                     InMemoryUserActivityProvider activityProvider) {
        this.eligibilityService = eligibilityService;
        this.activityProvider = activityProvider;
    }

    @GetMapping("/tiers/eligibility")
    public ApiResponse<EligibilityReport> eligibility(@PathVariable String userId) {
        return ApiResponse.ok(eligibilityService.evaluate(userId));
    }

    /** Demo/test seam: set the activity snapshot the rule engine reads for this user. */
    @PutMapping("/activity")
    public ApiResponse<EligibilityReport> setActivity(@PathVariable String userId,
                                                      @Valid @RequestBody ActivityRequest request) {
        activityProvider.setActivity(new UserActivity(
                userId,
                request.orderCount(),
                request.monthlyOrderValue(),
                request.cohorts() == null ? Set.of() : request.cohorts()));
        return ApiResponse.ok(eligibilityService.evaluate(userId));
    }
}
