# 0027. `/blueprint load` takes an optional rotation

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (asked for the open item to be fixed), Claude (syntax and pivot)

## Decision
`/blueprint load <id> [pos] [rotation]`. The rotation uses vanilla's `TemplateRotationArgument`, the same names as
`/place template`: `none`, `clockwise_90`, `180`, `counterclockwise_90`. The rotated area's min corner lands on `pos`,
just like the unrotated blueprint did before. `Blueprints.place` takes the rotation and returns the area it covers,
which becomes the player's selection as before.

## Why
- It reuses vanilla's argument type, so players already know the names and get tab completion.
- Putting the min corner at `pos` keeps the meaning of `pos` the same for every rotation. The other option was
  turning around the city block, like the Builder's Wand does. That would make `pos` mean something different in the
  command than in the unrotated case.

## Consequences
Rotation needs `pos`, because Brigadier arguments are positional. Use `~ ~ ~` for "here".
