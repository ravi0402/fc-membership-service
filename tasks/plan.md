# Implementation Plan — FC Membership Service

> Tech spec: [`docs/TECH_SPEC.md`](../docs/TECH_SPEC.md). Build order below; each phase
> compiles and is independently testable.

## Phase 0 — Project setup
- [x] `pom.xml`: Spring Boot 3.4.x, Java 21, web/data-jpa/validation/actuator, H2,
      postgresql, springdoc-openapi, test. groupId `com.firstclub`, artifact
      `fc-membership-service`, version `1.0.0`.
- [x] `application.yml` (default H2 + virtual threads) + `application-postgres.yml`.

## Phase 1 — Common foundation
- [x] `ApiResponse<T>` envelope, `ApiError`.
- [x] Exceptions: `ResourceNotFoundException`, `BusinessRuleException`,
      `DuplicateActiveSubscriptionException`, `TierNotEligibleException` + `GlobalExceptionHandler`.
- [x] `OpenApiConfig`.

## Phase 2 — Catalog module (plans, tiers, benefits, pricing)
- [x] Entities + enums, repositories, `CatalogService`, `CatalogController`, DTOs.

## Phase 3 — Tier eligibility engine
- [x] `UserActivity` + `UserActivityProvider` port + `InMemoryUserActivityProvider`.
- [x] `TierEligibilityRule` strategy + 3 rules + `TierEligibilityEvaluator`.
- [x] `TierCriterion` entity/repo, `TierEvaluationService`, controller, activity seam.

## Phase 4 — Subscription module
- [x] `MembershipSubscription` (+`@Version`, `activeUserKey`, idempotency), `SubscriptionEvent`.
- [x] `PricingService`, `BenefitResolver`, `SubscriptionService` (subscribe/upgrade/
      downgrade/cancel/history/benefits), `SubscriptionController`.

## Phase 5 — Seed data
- [x] `DataSeeder`: 3 plans, 3 tiers, benefits, price matrix, tier criteria, demo activity.

## Phase 6 — Tests
- [x] Unit: rule engine, pricing, subscription service (positive + negative + concurrency).
- [x] Web-slice + integration (`@SpringBootTest`, H2).

## Phase 7 — Ops & docs
- [x] `Dockerfile`, `docker-compose.yml` (app + postgres), `.circleci/config.yml`.
- [x] `README.md`, `docs/API.md`. Verify `mvn test` green + manual curl demo.
