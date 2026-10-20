# 0022. Territory is chunk squares around the Town Hall and buildings

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (the rule), Claude (how to read the sizes, and the design)

## Context
The user: the Town Hall has "the town hall's chunk plus 5x5 around it", and every new building adds "the chunk the
building is in plus 3x3 around it". It also had to be visible in-game.

## Decision
- **Reading of the sizes:** a **5x5 square of chunks centred on the Town Hall's chunk**, and a **3x3 square around
  each building's chunk**. Both are configurable as radii: `townHallTerritoryRadius = 2` and
  `buildingTerritoryRadius = 1`. The other reading (5 chunks in every direction, 11x11) is a one-line config change.
- **Worked out, not stored:** `Territory.of(settlement)` builds the territory each time from the Town Hall position and
  `Settlement.buildings()`, the workplaces claimed by the settlement. Nothing about the territory itself is stored.
- **Shown in-game:** a blue grid fence along every chunk edge where the territory ends. It's drawn from 8 blocks below to
  24 above the player, within about 160 blocks, while the Builder's Wand is held or after toggling with **B**.
  Only the leader's own territory is sent (`TerritoryPacket`).

## Why
Working it out each time means it can't get out of date when a Town Hall or building disappears. The config radii were
added because the sizes were ambiguous.

## Consequences
Losing the Town Hall removes its square. Huts claimed before this existed join `buildings()` the next time they're opened.
