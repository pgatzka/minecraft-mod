# 0021. Placing a blueprint places only the city block and records a site

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (only build the city block and mark the area), Claude (rules)

## Decision
`ConstructionSites.place` does the following:
- **Checks:**
  - The area is inside the world's height limits.
  - The city block's spot can be replaced (grass is fine).
  - The area doesn't overlap another site.
  - The territory rules pass ([0023](0023-buildings-only-in-own-territory.md), [0024](0024-town-halls-not-in-other-territory.md)).
- **Cost:** outside creative, it **uses up one of the city block's items** from the inventory.
- **Places** only the city block (rotated, fresh, without a settlement).
- **Records** a `ConstructionSite` (blueprint, dimension, city block position, rotation, area, settlement id) in
  `ConstructionSiteData`.
- **Claims** a placed workplace for the placer's settlement.

Breaking a site's city block removes the site. All players get the sites in their dimension (`ConstructionSitesPacket`),
drawn as yellow boxes.

## Why
The cost was Claude's choice, so the wand doesn't hand out free Town Halls. Blocking overlaps keeps future building
jobs from fighting over the same blocks.

## Consequences
Nothing builds sites yet. That's the next feature: the builder works through the site using the stored blueprint and rotation.
