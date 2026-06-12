# FirstClub Membership Service

**Version 1.0.0** · Spring Boot 3.4 · Java 21

Backend for FirstClub's subscription-based **Membership Program**: users buy a
membership on a billing cadence (Monthly / Quarterly / Yearly), sit at a benefit
**tier** (Silver / Gold / Platinum), and unlock **configurable** perks (free delivery,
extra discounts, early access, priority support). Tiers can be chosen when open, or
**earned** through activity-based criteria (order count, monthly spend, cohort).

> Design deep-dive: [`docs/TECH_SPEC.md`](docs/TECH_SPEC.md) ·
> API reference: [`docs/API.md`](docs/API.md)

---

## Quick start (zero setup — in-memory H2)

```bash
mvn spring-boot:run
```
Then:
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

The catalogue (plans, tiers, benefits, pricing, criteria) and two demo users
(`demo-user`, `vip-user`) are seeded automatically on startup.

### Run the tests
```bash
mvn test
```
22 tests: rule-engine units, service units (Mockito), web/integration (MockMvc + H2),
and a concurrency test proving only one active subscription survives a race.

### Production-like run (PostgreSQL via Docker)
```bash
docker compose up --build
```
Brings up Postgres + the service on the `postgres` profile.

---

## 60-second demo (curl)

```bash
B=http://localhost:8080/api/v1; U=alice

# 1. Browse catalogue
curl -s $B/plans; curl -s $B/tiers; curl -s $B/benefits

# 2. Give the user activity so they can earn Gold (stand-in for the Order service)
curl -s -X PUT $B/users/$U/activity -H 'Content-Type: application/json' \
  -d '{"orderCount":8,"monthlyOrderValue":7000,"cohorts":[]}'

# 3. Check tier eligibility
curl -s $B/users/$U/tiers/eligibility

# 4. Subscribe (Monthly / Silver)
curl -s -X POST $B/users/$U/subscription -H 'Content-Type: application/json' \
  -d '{"planCadence":"MONTHLY","tierCode":"SILVER","autoRenew":true}'

# 5. Upgrade to Gold (eligible), inspect benefits, view history
curl -s -X POST $B/users/$U/subscription/upgrade -H 'Content-Type: application/json' \
  -d '{"targetTierCode":"GOLD"}'
curl -s $B/users/$U/benefits
curl -s $B/users/$U/subscription/history

# 6. Auto-tier from activity, then cancel
curl -s -X PUT $B/users/$U/activity -H 'Content-Type: application/json' \
  -d '{"orderCount":20,"monthlyOrderValue":25000,"cohorts":["EARLY_ADOPTER"]}'
curl -s -X POST $B/users/$U/tiers/sync
curl -s -X POST $B/users/$U/subscription/cancel
```

---

## Design at a glance

**Plan vs Tier are separate axes.** A plan answers *how often you pay*; a tier answers
*what benefits you get*. Price is a function of `(plan, tier)` via a small **price
matrix**, so cadence and benefit level vary the cost independently.

**Tiers reconcile "pick" and "earn"** with a per-tier `eligibilityMode`
(`OPEN` vs `CRITERIA_BASED`) — no scattered branching.

### Key abstractions (the extensibility story)
| Abstraction | Role | Extending it |
|-------------|------|--------------|
| `TierEligibilityRule` (Strategy) | one rule per criterion type | new criterion = **add one bean** + config rows |
| `UserActivityProvider` (Port) | reads order/cohort activity | swap the in-memory adapter for a real Order-service adapter |
| `PricingService` | resolves `(plan, tier)` price | layer promos/proration here |
| `BenefitResolver` | effective perks per tier | perks are **data** (`Benefit` + `TierBenefit`), not code |

Add a **new perk** (row), a **new tier** (row + prices + criteria), or a **new cadence**
(row) without touching service logic. See `docs/TECH_SPEC.md` §7 for worked scenarios.

### Concurrency (the bonus)
- `@Version` optimistic locking → concurrent tier changes return **409** instead of a lost update.
- A `UNIQUE` constraint on `active_user_key` (holds `userId` while ACTIVE, `NULL` otherwise)
  guarantees **at most one active membership per user**, even under a subscribe race.
- Optional `Idempotency-Key` makes subscribe retries safe.
- Java 21 **virtual threads** enabled for scalable request handling.

---

## Project layout

```
src/main/java/com/firstclub/membership
├── catalog/        plans, tiers, benefits, pricing  (configurable catalogue)
├── subscription/   lifecycle: subscribe/upgrade/downgrade/cancel/renew + history
├── tier/           eligibility rule engine + user-activity port
└── common/         response envelope, exceptions, config, data seeding
```
Each module is layered `web → service → repository → domain`; entities never cross the
service boundary (controllers speak DTO `record`s wrapped in `ApiResponse<T>`).

## Tech stack
Spring Boot 3.4, Spring Web / Data JPA / Validation / Actuator, Hibernate, H2 (default) &
PostgreSQL (`postgres` profile), springdoc-openapi, JUnit 5 + Mockito + AssertJ, Maven.
No Lombok.
