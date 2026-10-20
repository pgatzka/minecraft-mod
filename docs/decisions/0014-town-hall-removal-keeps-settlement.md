# 0014. Removing a Town Hall keeps its settlement

- Status: Accepted. It replaces Claude's first choice that removing the Town Hall dissolves the settlement. The open gap below
  is closed by [0028](0028-settlement-moves-to-new-town-hall.md).
- Date: 2026-10-20
- Decided by: user ("destroying the town hall should not delete the settlement")

## Context
At first, breaking a Town Hall deleted its settlement. Claude chose that so a player couldn't be stuck forever as
leader of a settlement without a Town Hall, given the one-settlement-per-player rule.

## Decision
Breaking a Town Hall keeps the settlement, with its name, leader, settlers, jobs and buildings. Its `townHallPos` becomes
null, and the Town Hall's part of the territory disappears ([0022](0022-territory-shape.md)). Settlement records are
only deleted by GameTest cleanup.

## Consequences
**Open gap:** a leader who lost their Town Hall can't found a new settlement (they still lead one), and there's no way
yet to give the settlement a new Town Hall. A "move settlement here" action on a free Town Hall was suggested to the
user and not decided yet.
