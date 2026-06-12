# API Reference — FC Membership Service

Base URL: `http://localhost:8080/api/v1`
All responses use the envelope `{ "success": boolean, "data": <payload>|null, "error": string|null }`.
Interactive docs: `http://localhost:8080/swagger-ui.html`.

> `{userId}` is any string (no auth in v1). Seeded demo users: `demo-user` (Gold-eligible),
> `vip-user` (Platinum-eligible).

---

## Catalog

### `GET /plans`
Membership plans with per-tier pricing.
```bash
curl -s http://localhost:8080/api/v1/plans
```

### `GET /tiers`
Tiers with the perks they unlock and the criteria that gate them.
```bash
curl -s http://localhost:8080/api/v1/tiers
```

### `GET /benefits`
The configurable benefit catalogue.
```bash
curl -s http://localhost:8080/api/v1/benefits
```

---

## Tier eligibility & activity

### `PUT /users/{userId}/activity`
Demo seam standing in for a real Order-service feed. Sets the activity the rule engine reads.
```bash
curl -s -X PUT http://localhost:8080/api/v1/users/alice/activity \
  -H 'Content-Type: application/json' \
  -d '{"orderCount":8,"monthlyOrderValue":7000,"cohorts":["EARLY_ADOPTER"]}'
```

### `GET /users/{userId}/tiers/eligibility`
Per-tier qualification + recommended tier for the user's current activity.
```bash
curl -s http://localhost:8080/api/v1/users/alice/tiers/eligibility
```

---

## Subscription lifecycle

### `POST /users/{userId}/subscription` → 201
Subscribe to a `(planCadence, tierCode)`. Optional `Idempotency-Key` header makes retries safe.
```bash
curl -s -X POST http://localhost:8080/api/v1/users/alice/subscription \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: alice-2026-06-01' \
  -d '{"planCadence":"MONTHLY","tierCode":"SILVER","autoRenew":true}'
```
`planCadence` ∈ `MONTHLY | QUARTERLY | YEARLY`; `tierCode` ∈ `SILVER | GOLD | PLATINUM`.

### `GET /users/{userId}/subscription`
Current membership + expiry (`daysRemaining`).

### `POST /users/{userId}/subscription/upgrade`
Move to a higher tier. Gated tiers require eligibility (else 409).
```bash
curl -s -X POST http://localhost:8080/api/v1/users/alice/subscription/upgrade \
  -H 'Content-Type: application/json' -d '{"targetTierCode":"GOLD"}'
```

### `POST /users/{userId}/subscription/downgrade`
Move to a lower tier (no eligibility check).
```bash
curl -s -X POST http://localhost:8080/api/v1/users/alice/subscription/downgrade \
  -H 'Content-Type: application/json' -d '{"targetTierCode":"SILVER"}'
```

### `POST /users/{userId}/subscription/cancel`
Cancel the active membership.

### `POST /users/{userId}/tiers/sync`
Evaluate the user's activity and auto-apply the highest tier they qualify for (logs `AUTO_TIER_CHANGE`).

### `GET /users/{userId}/subscription/history`
Append-only audit trail (newest first).

### `GET /users/{userId}/benefits`
Effective perks for the user's current tier.

---

## Status codes
| Code | Meaning |
|------|---------|
| 200 | OK |
| 201 | Subscription created |
| 400 | Validation error (malformed body) |
| 404 | No active subscription / unknown plan or tier |
| 409 | Duplicate active subscription, optimistic-lock conflict, or tier not eligible |
| 422 | Business-rule violation (e.g. "upgrade" to a lower tier) |
