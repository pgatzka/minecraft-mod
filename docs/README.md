# Minecraft Mod: start here

Notes for picking up this project in a new session. Read this file first, then [architecture.md](architecture.md)
for how the code fits together, [testing.md](testing.md) before writing or running tests, and
[decisions/](decisions/README.md) for why things are the way they are.

## What it is

A Minecraft **1.20.1 / Forge 47.4.10** mod (mod id `minecraftmod`, package `com.philgatzka.minecraftmod`) about founding
and growing settlements:

- **Town Hall** block: right-click to found a settlement (one per player). Founding spawns settlers.
- **Settlers**: player-like mobs with random names that belong to a settlement and can be given jobs.
- **Builder's Hut**: a workplace; its screen assigns one settler as its builder.
- **Blueprints**: operators capture existing structures around a city block (Blueprint Wand or `/blueprint`)
  and save them as structure files.
- **Builder's Wand**: players choose a blueprint, see a see-through preview, rotate it with R and place it as a
  **construction site**. Only the city block is placed; the rest of the area is marked to be built later.
- **Territory**: a settlement may only build in its chunks (5x5 around the Town Hall, plus 3x3 around each other
  building). Shown as a blue fence while holding the Builder's Wand, or toggled with B.

## Status (2026-10-20)

Everything above works and is covered by 42 GameTests that pass. Not checked in the real game yet, because GameTests
can't draw or click: the screens, the selection and site outlines, the Builder's Wand ghost preview, the territory
fence and the R/B keys. Ask the user to try these.

**Not built yet / open questions** (ask the user before deciding):

- **Builders don't build yet.** The next step is the Builder's Hut's builder working through construction sites.
- A leader whose Town Hall was destroyed keeps the settlement but can't give it a new Town Hall
  ([0014](decisions/0014-town-hall-removal-keeps-settlement.md)).
- The Builder's Wand preview doesn't know other settlements' territories, so it can show green for a Town Hall site
  the server then refuses ([0024](decisions/0024-town-halls-not-in-other-territory.md)).
- `/blueprint load` always places unrotated.
- The MDK's sample content (example block, example item, sample config values, `Config.items`) is still in the mod.
- Git: the repository is initialized and everything is staged, but nothing is committed yet. Commit only when asked.

## Working rules (from the user)

- **Never start the game yourself** (`runClient`, `runServer`, or piping commands into a server). Check changes
  only with automated tests ([0008](decisions/0008-automated-tests-only.md)). Things that need eyes in-game: ask the user.
- **Record every decision** in [decisions/](decisions/README.md) (one numbered file each, in the same turn as the
  change), and keep this file current.
- The GUI must not use texture assets: screens are drawn in code with the small framework in `client/ui`
  ([0011](decisions/0011-code-drawn-gui-framework.md)).

## Build and test

The default Java on this machine is 25, which Gradle 8.8 can't run on. Run Gradle with **JDK 21**; it downloads the
Java 17 that Minecraft 1.20.1 needs by itself ([0003](decisions/0003-build-on-jdk-21.md)).

```bash
# Git Bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
./gradlew build              # compile and build the jar (build/libs/)
./gradlew runGameTestServer  # run all GameTests (about 30 s); exits non-zero if any fail
```

```powershell
# PowerShell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"; .\gradlew runGameTestServer
```

Run `runGameTestServer` in the foreground with a timeout, and afterwards check that no `java.exe` from it is left.
A stopped background run once left Gradle and a server running. The test log is easiest to read via
`./gradlew runGameTestServer > build/gametest.log` and then grepping for `failed!` and `required tests`.

Known harmless noise: two deprecation warnings from the MDK's `Config.java`, and startup stack traces about
`setAccessible`/netty/`server.properties` in the test server log.

## Repository layout

```
docs/                    these notes and the decision log
tools/                   make_test_structure.py (GameTest platforms), Resize.java (texture downscaling)
src/main/java/com/philgatzka/minecraftmod/
  MinecraftMod.java      all registrations (blocks, items, entities, menus), event wiring, client setup
  Config.java            common config (settlers on founding, territory radii, MDK samples)
  block/, block/entity/  Town Hall, Builder's Hut, generic workplace block/block entity
  entity/                Settler and its name lists
  settlement/            settlements, settlers, jobs, territory (rules + saved data)
  blueprint/             blueprint files, selections, /blueprint commands, placement math
  construction/          construction sites (rules + saved data)
  item/                  Blueprint Wand, Builder's Wand
  menu/                  slot-less menus carrying screen data from server to client
  network/               SimpleChannel and all packets
  client/                screens, renderers (outlines, ghost preview, territory), client caches, keys
  client/ui/             the code-drawn GUI framework
  gametest/              all GameTests
src/main/resources/      assets (models, textures, lang), data (loot, tags, test structures)
CREDITS.md               texture sources
run/                     dev game directory (git-ignored); the GameTest world lives in run/world
```
