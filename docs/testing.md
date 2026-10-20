# Testing

All tests are Minecraft GameTests in `src/main/java/com/philgatzka/minecraftmod/gametest/MinecraftModGameTests.java`.
Run them with `./gradlew runGameTestServer` using JDK 21 (see [README.md](README.md#build-and-test)). It starts a
headless server, runs every test, and exits with a non-zero code if any fail. **Never test by starting the client or
server yourself** ([0008](decisions/0008-automated-tests-only.md)).

## How the tests are set up

- The class is a `@GameTestHolder("minecraftmod")` with `@PrefixGameTestTemplate(false)`, so a template name like
  `test_platform` means `data/minecraftmod/structures/test_platform.nbt`.
- Test structures (all stone floor + air, made with `tools/make_test_structure.py`):
  - `test_platform` (5x3x5): most settlement and settler tests.
  - `blueprint_platform` (12x5x5): blueprint and construction tests. A source structure goes at x=1..3, and copies or sites
    at x=7..9.
- Tests call the server-side services (`Settlements`, `Blueprints`, `ConstructionSites`, `Territory`) directly, with
  `helper.makeMockPlayer()` (a creative player without a connection) or `makeSurvivalPlayer` for survival rules.
  Commands are tested through `server.getCommands().performPrefixedCommand(...)`.

## Gotchas (each of these broke tests once)

1. **Relative positions start one block below the structure.** In `GameTestHelper` coordinates, the structures' stone
   floor is at **y=1** and the first air layer is **y=2**. Use `FLOOR`/`ABOVE_FLOOR`, `SOURCE_MIN`, ... instead of
   writing new y values by hand.
2. **The test world is kept between runs** (`run/world`). Anything a test saves globally stays: blueprint files,
   construction sites, settlements, and settlers (which never despawn). Tests delete their blueprint files and sites
   in `finally` blocks (`withSourceBlueprint`, `deleteBlueprint`), and use random blueprint names (`uniqueBlueprintName`).
3. **Settlement territories are wider than the tests.** One settlement claims 5x5 chunks, so it reaches into
   neighbouring tests and into later runs. Every test that founds a settlement or places a construction site
   runs in **its own batch** (`batch = "<method name>"`; batches run one after another) and starts with
   `clearSettlementsAndSites(helper)`, which removes all settlements, sites and settlers. Do the same for new tests of
   that kind, or they will fail at random depending on what ran nearby.
4. **Don't count entities in an area blindly.** Settlers from other tests wander around; compare before and after,
   like `spawnEggSpawnsNamedSettler` does.
5. Breaking a block in the world (`helper.destroyBlock`) didn't produce the expected item drop in the test server,
   so drop tests ask the loot table (`Block.getDrops`) instead.

## What GameTests can't cover

Rendering, screens and input: outlines, the ghost preview, the territory fence, keys, and every GUI. Keep their logic
in shared, tested code (for example, `BlueprintPlacement` and `ConstructionSites.checkArea` are used by both the
preview and the server), and ask the user to check the visuals in-game.

After a run, check that no `java.exe` from the test server is left (`Get-CimInstance Win32_Process -Filter "Name='java.exe'"`).
