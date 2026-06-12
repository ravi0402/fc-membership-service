package com.firstclub.membership.catalog.web;

import com.firstclub.membership.catalog.dto.BenefitView;
import com.firstclub.membership.catalog.dto.PlanView;
import com.firstclub.membership.catalog.dto.TierView;
import com.firstclub.membership.catalog.service.CatalogService;
import com.firstclub.membership.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only catalogue endpoints the storefront uses to render membership options. */
@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/plans")
    public ApiResponse<List<PlanView>> plans() {
        return ApiResponse.ok(catalogService.listPlans());
    }

    @GetMapping("/tiers")
    public ApiResponse<List<TierView>> tiers() {
        return ApiResponse.ok(catalogService.listTiers());
    }

    @GetMapping("/benefits")
    public ApiResponse<List<BenefitView>> benefits() {
        return ApiResponse.ok(catalogService.listBenefits());
    }
}
