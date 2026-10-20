# 0028. A settlement without a Town Hall can move to a free one

- Status: Accepted
- Date: 2026-10-20
- Decided by: user ("do the small cleanups", which included this open item), Claude (how it works)

## Context
[0014](0014-town-hall-removal-keeps-settlement.md) left a gap: a leader whose Town Hall was destroyed still leads the
settlement, so they can't found another, and they had no way to give it a new Town Hall.

## Decision
When the leader of a settlement **without** a Town Hall opens a free Town Hall, the Town Hall screen offers
"Move <settlement> here" instead of "You already lead ...". `Settlements.tryMoveTo` (via `MoveSettlementPacket`)
checks:
- the Town Hall is free,
- the player leads a settlement, and that settlement has no Town Hall,
- the Town Hall is in the settlement's dimension,
- the Town Hall is not inside another settlement's territory (the same rule as founding, [0024](0024-town-halls-not-in-other-territory.md)).

Then the Town Hall becomes the settlement's Town Hall, and the territory is rebuilt around it. Settlers, jobs and
buildings stay unchanged.

## Why
- This was the "move settlement here" action suggested in 0014. It reuses the existing Town Hall screen and
  founding rules, and adds nothing new to learn.
- Only settlements that lost their Town Hall can move. Moving a settlement that still has one would raise open
  questions (what happens to the old block, can a settlement jump far away). Those can be decided later if wanted.
- The Town Hall must be in the settlement's dimension, because its buildings and settlers are there.

## Consequences
- A leader can place a Town Hall anywhere that isn't another settlement's territory (hand placement only blocks
  that), then move the settlement to it. That includes places far from the old site.
- The network protocol version went from 1 to 2, because the new packet and the extra `TownHallInfo` field change the wire format.
