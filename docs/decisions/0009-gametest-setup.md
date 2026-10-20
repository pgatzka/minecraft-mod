# 0009. GameTest layout: main source set, generated platforms, isolation

- Status: Accepted
- Date: 2026-10-20
- Decided by: Claude

## Decision
- **Where the tests live:** in the main source set (`gametest/MinecraftModGameTests.java`), like Create, AE2 and
  Botania. They ship in the jar but only load when GameTests are on. Mekanism's separate source set was considered;
  it's more Gradle setup for little gain right now.
- **Test structures:** generated with `tools/make_test_structure.py` (stone floor and air) instead of built in-game,
  because the user doesn't want manual game launches ([0008](0008-automated-tests-only.md)).
- **Item drops:** checked through the loot table (`Block.getDrops`). Breaking the block in the test world didn't
  produce the item, for reasons not tracked down.
- **Isolation:** tests that found settlements or place construction sites each run in their own batch and start by
  clearing all settlements, sites and settlers. Tests that write files delete them in `finally` and use random names.
- **Coordinates:** test positions are kept above the floor. GameTest-relative y=1 is the structure's floor.

## Why
Each isolation rule fixes a real failure (details in docs/testing.md):
- The test world (`run/world`) is kept between runs.
- Settler entities never despawn.
- One settlement's territory (5x5 chunks) reaches neighbouring tests once founding near another settlement is refused.
- The y offset was found when tests placed blocks into the floor.

## Consequences
Running each settlement test alone makes the run slower (29 batches, about 30 s in total) but reliable. New
settlement tests must follow the same pattern (docs/testing.md).
