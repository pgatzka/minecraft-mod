# 0016. Generic workplaces; the settlement records who works where

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (Builder's Hut with a screen to pick a settler for the job), Claude (design)

## Decision
- **Generic workplaces:** `WorkplaceBlock`, `WorkplaceBlockEntity`, `WorkplaceMenu` and `WorkplaceScreen` work for any
  workplace. A building only has to say its `SettlerJob`. The Builder's Hut is the first one (`SettlerJob.BUILDER`).
- **Who works where** is stored **only in the settlement** (`SettlerRecord.job` and `.workplace`), not in the block
  entity or the entity. The block entity stores only its settlement id.
- **Rules:**
  - One worker per workplace, and one job per settler. Assigning someone new makes the old worker unemployed, and
    picking a settler who works elsewhere moves them.
  - Only the leader can assign.
  - Breaking a workplace makes its worker unemployed; they stay in the settlement.
- **Claiming:** a workplace joins the settlement of the leader who places or opens it. That's now limited by territory
  ([0023](0023-buildings-only-in-own-territory.md)).

## Why
A single source of truth avoids the block, the entity and the settlement disagreeing, and works with unloaded chunks.
The generic design means a new job building needs no new screen, menu or packet (see docs/architecture.md).

## Consequences
Code that needs a settler's job looks it up in the settlement by entity UUID.
