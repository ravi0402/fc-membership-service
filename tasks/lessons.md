# Lessons (Self-Improvement Log)

Patterns learned on this project. Reviewed at session start.

## Project conventions
- Base package `com.firstclub.membership`; groupId `com.firstclub`; artifact
  `fc-membership-service` (must match parent dir). No company name other than FirstClub.
- No Lombok — hand-write constructors/getters; prefer `record` DTOs.
- Bump PATCH on every regeneration; keep `README.md` and this file current.
- H2 is the default runtime; `postgres` profile for docker-compose.

## Design decisions
- Plan (billing cadence) and Tier (benefit level) are separate axes; price is a
  function of `(plan, tier)` via a price matrix, not a scalar on the plan.
- Tier `eligibilityMode` (OPEN vs CRITERIA_BASED) reconciles "pick a tier" with
  "earn a tier" without scattered branching.
- Eligibility is a Strategy engine keyed by `CriterionType`; user activity comes
  through a `UserActivityProvider` port (hexagonal) so orders stay external.
