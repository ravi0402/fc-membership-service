# FirstClub Membership Service — Technical Specification

**Version:** 1.0.0
**Status:** Implemented
**Owner:** Backend / Membership
**Last updated:** 2026-06-12

---

## 1. Overview

FirstClub is building an omnichannel retail platform. This service powers the
**subscription-based Membership Program**: users buy a membership on a billing
cadence (Monthly / Quarterly / Yearly), sit at a benefit **tier** (Silver / Gold /
Platinum), and unlock configurable perks (free delivery, extra discounts, early
access, priority support). Users move between tiers either by **choosing** an open
tier or by **earning** a gated tier through activity-based criteria (order count,
monthly spend, cohort membership).

This document is the source of truth for the domain model, the abstractions, the
API contract, the concurrency strategy, and the extensibility seams.

### 1.1 Goals

- Functional, demo-able REST APIs for the full membership lifecycle.
- Clean, extensible domain model — the design is the deliverable.
- Configurable benefits and configurable tier-eligibility criteria (no redeploy to
  tune thresholds or perks).
- Correct behaviour under concurrency (no lost updates, no duplicate active
  memberships, idempotent subscribe).

### 1.2 Non-goals (v1)

- Real payment capture / invoicing (pricing is computed; charging is stubbed).
- Proration on mid-cycle tier changes (changes apply immediately, expiry unchanged).
- Ownership of order data (consumed through a port; see §6).
- AuthN/AuthZ (out of scope; `userId` is passed in. Hooks noted in §9).

---

## 2. The Core Modeling Insight

The brief blends two concepts that are frequently conflated. Separating them is the
backbone of this design:

| Concept | Question it answers | Examples | Mutable by user? |
|---------|--------------------|----------|------------------|
| **Plan** | *How long / how often do I pay?* | Monthly, Quarterly, Yearly | Chosen at subscribe |
| **Tier** | *What level of benefits do I get?* | Silver, Gold, Platinum | Upgrade / downgrade / earned |

A **Subscription** is the binding of a `(user, plan, tier)` over a time window.
**Price is a function of `(plan, tier)`** — a Platinum-Yearly membership costs more
than a Silver-Monthly one — so pricing lives in a small price matrix rather than on
the plan alone.

Tiers also carry a second tension: the brief says tiers are both *user-selected* and
*criteria-driven*. We reconcile this with a per-tier **eligibility mode**:

- `OPEN` — anyone may select/purchase the tier.
- `CRITERIA_BASED` — the user must satisfy the tier's criteria (evaluated by the
  rule engine) before they can move to it; the system can also auto-promote them.

This single enum cleanly supports "pick your tier" *and* "earn your tier" without
branching logic scattered across the codebase.

---

## 3. Domain Model

### 3.1 Entities

```
MembershipPlan      code(MONTHLY|QUARTERLY|YEARLY), displayName, billingMonths, active
MembershipTier      code(SILVER|GOLD|PLATINUM), displayName, level, eligibilityMode,
                    criteriaMatchMode(ANY|ALL), active
Benefit             code, type(BenefitType), description, defaultValue, active   (catalog of perks)
TierBenefit         tier ─< benefit, valueOverride        (which perks a tier unlocks; configurable)
MembershipPlanPrice plan + tier → amount, currency        (the price matrix)
TierCriterion       tier, type(CriterionType), operator, threshold, cohort       (configurable rules)

MembershipSubscription  userId, plan, tier, status, startDate, endDate, autoRenew,
                        pricePaid, currency, idempotencyKey, activeUserKey, @Version
SubscriptionEvent       subscriptionId, userId, type(SubscriptionEventType),
                        fromTier, toTier, note, occurredAt          (append-only audit)
```

### 3.2 Enumerations

- `BillingCadence` — MONTHLY(1), QUARTERLY(3), YEARLY(12) (months).
- `BenefitType` — `FREE_DELIVERY`, `PERCENTAGE_DISCOUNT`, `EXCLUSIVE_DEALS`,
  `EARLY_ACCESS`, `PRIORITY_SUPPORT`. New perk types = new enum constant.
- `TierEligibilityMode` — `OPEN`, `CRITERIA_BASED`.
- `CriteriaMatchMode` — `ANY`, `ALL`.
- `CriterionType` — `ORDER_COUNT`, `MONTHLY_ORDER_VALUE`, `COHORT`.
- `ComparisonOperator` — `GTE`, `GT`, `LTE`, `LT`, `EQ`.
- `SubscriptionStatus` — `ACTIVE`, `CANCELLED`, `EXPIRED`.
- `SubscriptionEventType` — `SUBSCRIBED`, `UPGRADED`, `DOWNGRADED`, `CANCELLED`,
  `RENEWED`, `AUTO_TIER_CHANGE`, `EXPIRED`.

### 3.3 Relationships & fetching

- `TierBenefit`, `MembershipPlanPrice`, `TierCriterion` reference `MembershipTier`
  (and `Benefit` / `MembershipPlan`) via `@ManyToOne(fetch = LAZY)`.
- Catalog reads use explicit `JOIN FETCH` / `@EntityGraph` to avoid N+1.
- Subscriptions store `plan`/`tier` as `@ManyToOne(LAZY)`; DTO projection happens
  inside the transaction to avoid `LazyInitializationException`.

---

## 4. Architecture

Layered, feature-modular (package-by-feature, not package-by-layer at the top):

```
com.firstclub.membership
├── catalog/        plans, tiers, benefits, pricing  (the configurable catalogue)
├── subscription/   lifecycle: subscribe/upgrade/downgrade/cancel/renew + history
├── tier/           eligibility rule engine + user-activity port
└── common/         response envelope, exceptions, config, seeding
```

Each module has `domain / repository / service / web / dto`. Flow is
**Controller → Service → Repository**; domain entities never leave the service layer
— controllers speak DTOs (`record`s) wrapped in a uniform `ApiResponse<T>` envelope.

### Key abstractions

1. **`TierEligibilityRule` (Strategy)** — one implementation per `CriterionType`
   (`OrderCountRule`, `MonthlyOrderValueRule`, `CohortRule`). The evaluator receives
   `List<TierEligibilityRule>` by Spring injection and dispatches by type (the
   Spring-idiomatic Factory from the design-patterns skill). **Adding a new
   criterion = add one class + config rows. Zero changes to existing code.**

2. **`UserActivityProvider` (Port / hexagonal seam)** — the membership service does
   not own orders. Activity (order count, monthly spend, cohorts) is read through a
   port. The shipped adapter is `InMemoryUserActivityProvider` (seeded, demo-mutable);
   in production this is swapped for an Order-service / data-warehouse adapter with
   no change to the rule engine.

3. **`PricingService`** — resolves price from the `(plan, tier)` matrix. Pricing
   policy is isolated, so promos/proration can be layered later.

4. **`BenefitResolver`** — computes the *effective* benefit set for a tier from
   `TierBenefit` overrides + `Benefit` defaults. Perks are data, not code.

---

## 5. Concurrency Strategy (explicit, the bonus)

| Risk | Mechanism |
|------|-----------|
| **Lost update** on concurrent upgrade/downgrade/cancel of the same subscription | `@Version` optimistic locking → `OptimisticLockingFailureException` surfaced as **409 Conflict** |
| **Two active memberships** from concurrent subscribe (TOCTOU) | App-level check *inside* `@Transactional` **plus** a DB `UNIQUE` constraint on `active_user_key` (holds `userId` while ACTIVE, `NULL` otherwise — DB-agnostic partial-uniqueness trick). Loser of the race fails the constraint → 409 |
| **Duplicate create on client retry** | Optional `Idempotency-Key` → unique column; a replayed key returns the original subscription (200) instead of creating a second |
| **Throughput** | Java 21 **virtual threads** (`spring.threads.virtual.enabled=true`); services are stateless |
| **Hidden mutation bugs** | Immutable DTOs (`record`), append-only `SubscriptionEvent`, no in-place entity mutation leaking across layers |

`SubscriptionService` write methods are `@Transactional`; reads are
`@Transactional(readOnly = true)`.

---

## 6. API Contract

Base path `/api/v1`. All responses use the envelope
`{ "success": boolean, "data": T|null, "error": string|null }`.
Full reference with curl examples: [`docs/API.md`](./API.md). Live Swagger UI at
`/swagger-ui.html`.

### Catalog (read)
- `GET  /plans` — membership plans (cadence + per-tier pricing).
- `GET  /tiers` — tiers with unlocked benefits and eligibility criteria.
- `GET  /benefits` — configurable benefit catalogue.

### Subscription lifecycle
- `POST /users/{userId}/subscription` — subscribe `(planCode, tierCode, autoRenew)`,
  honours `Idempotency-Key` header → **201**.
- `GET  /users/{userId}/subscription` — current membership + expiry.
- `POST /users/{userId}/subscription/upgrade` — `{ targetTierCode }`.
- `POST /users/{userId}/subscription/downgrade` — `{ targetTierCode }`.
- `POST /users/{userId}/subscription/cancel` — cancel active membership → **200**.
- `GET  /users/{userId}/subscription/history` — audit trail.
- `GET  /users/{userId}/benefits` — effective benefits for the user's current tier.

### Tier eligibility engine
- `GET  /users/{userId}/tiers/eligibility` — per-tier qualification + recommended tier.
- `POST /users/{userId}/tiers/sync` — evaluate criteria and auto-apply the highest
  qualified tier to the active subscription (logs `AUTO_TIER_CHANGE`).

### Demo / test seam
- `PUT  /users/{userId}/activity` — set a user's activity snapshot (order count,
  monthly spend, cohorts) to exercise the rule engine without a real Order service.

### Status codes
`200` ok · `201` created · `400` validation · `404` not found ·
`409` conflict (duplicate active / optimistic lock / not eligible) · `422` business rule.

---

## 7. Extensibility Scenarios (worked)

- **New perk** (e.g. "2-hour delivery"): add a `BenefitType` constant + a `Benefit`
  row; attach to tiers via `TierBenefit`. No service code changes.
- **New tier** (e.g. "Diamond"): insert a `MembershipTier` (level 4) + price-matrix
  rows + criteria rows. Upgrade/downgrade/eligibility all work via `level` ordering.
- **New eligibility rule** (e.g. "referrals ≥ N"): add `CriterionType.REFERRALS`, one
  `TierEligibilityRule` bean, config rows. Evaluator picks it up via injection.
- **New billing cadence** (e.g. Weekly): add a `MembershipPlan` row; `billingMonths`
  → generalises to a `Period`. Pricing via the matrix.
- **Real order data**: replace `InMemoryUserActivityProvider` with a real adapter
  implementing `UserActivityProvider`. Rule engine untouched.

---

## 8. Persistence & Config

- **Default profile** → in-memory **H2** (zero setup: `mvn spring-boot:run`).
- **`postgres` profile** → PostgreSQL via `docker-compose` for a production-like run.
- Schema via Hibernate `ddl-auto`; reference catalogue + demo data seeded
  idempotently by `DataSeeder` (DB-agnostic, runs only when tables are empty).
- Actuator health at `/actuator/health`.

## 9. Future hooks

AuthN/Z (resolve `userId` from JWT), payment gateway behind a `PaymentPort`,
scheduled `expiry`/`renewal` job, outbox for `SubscriptionEvent`, proration policy
in `PricingService`, Flyway migrations for prod.

## 10. Testing

Unit (rule engine, pricing, services with Mockito), web-slice (`@WebMvcTest`),
and integration (`@SpringBootTest` + H2) covering positive and negative paths,
including concurrency guards (duplicate-active, optimistic lock, idempotency).
Target ≥ 80% line coverage on service/rule logic.
