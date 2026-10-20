# 0023. Buildings other than Town Halls only inside your own territory

- Status: Accepted
- Date: 2026-10-20
- Decided by: user ("buildings can only be placed inside that radius"), Claude (details)

## Decision
- **Sites:** a Builder's Hut (any non-Town-Hall) site needs its **whole area** inside the placer's territory, and the
  placer must lead a settlement in that dimension. One block outside is refused.
- **By hand:** placing a workplace block by hand is cancelled outside the placer's territory (`BlockEvent.EntityPlaceEvent`),
  so the Builder's Wand can't be bypassed.
- **Claiming:** a workplace only becomes claimed (and so a building that extends the territory) when it's inside the
  territory.
- **Town Halls are exempt,** because they found the territory. See [0024](0024-town-halls-not-in-other-territory.md)
  for their limit.
- **Preview:** the wand preview turns red when the site would leave the territory, using the territory the client was sent.

## Why
Checking the whole area keeps buildings from sticking out of the territory. Checking hand placement closes the obvious
way around the rule.

## Consequences
Blocks set by commands or `setBlock` skip the hand-placement check, but such huts still can't be claimed outside the territory.
