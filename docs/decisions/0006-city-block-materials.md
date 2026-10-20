# 0006. Town Hall is stone (pickaxe), Builder's Hut is wood (axe)

- Status: Accepted
- Date: 2026-10-20
- Decided by: Claude

## Context
The blocks needed hardness, sound, map color and a tool.

## Decision
- **Town Hall:** quartz map color, stone sound, strength 2.0/6.0, needs a pickaxe to drop (`mineable/pickaxe`).
  Its first version used vanilla bookshelf and planks textures and was wood/axe. That changed with the marble textures.
- **Builder's Hut:** wood map color, wood sound, strength 2.0/3.0, mined with an axe (`mineable/axe`), drops without a tool.

Both drop themselves (loot tables) and use `BaseEntityBlock` with `RenderShape.MODEL` (the default is invisible).

## Why
They match what the textures look like.

## Consequences
None beyond normal block behavior.
