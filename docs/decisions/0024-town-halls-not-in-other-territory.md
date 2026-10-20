# 0024. Town Halls not inside another settlement's territory

- Status: Accepted
- Date: 2026-10-20
- Decided by: user ("yes that should be locked"), Claude (where it's checked)

## Decision
A Town Hall may not be in **another** settlement's territory. This is checked in three places:
- **Founding** (`Settlements.tryCreate`): the Town Hall's chunk is in any settlement's territory. The founder leads no
  settlement yet, so every territory counts as "another".
- **Town Hall construction sites:** any chunk of the site's area is in a territory other than the placer's own.
- **Placing a Town Hall by hand:** cancelled when its chunk is in a territory other than the placer's own.

A leader may still place a Town Hall in their own territory.

## Why
A settlement's area shouldn't be taken over by a newcomer. The leader's own territory is allowed, because that's
where a settlement whose Town Hall was destroyed would rebuild ([0014](0014-town-hall-removal-keeps-settlement.md)).

## Consequences
- The client only knows the leader's own territory, so the Builder's Wand preview can show green for a Town Hall site
  that the server then refuses with "This is inside the territory of ...". Sending other settlements' territories
  would fix that. That's an open item.
- GameTests that found settlements had to be isolated ([0009](0009-gametest-setup.md)).
