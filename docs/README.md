# Minecraft Mod: start here

Notes for picking up this project in a new session. Read this file first, then [architecture.md](architecture.md)
for how the code fits together, [testing.md](testing.md) before writing or running tests, and
[decisions/](decisions/README.md) for why things are the way they are.

## What it is

A Minecraft **1.20.1 / Forge 47.4.10** mod (mod id `minecraftmod`, package `com.philgatzka.minecraftmod`) about founding
and growing settlements:

- **Town Hall** block: right-click to found a settlement (one per player). Founding spawns settlers. A settlement whose
  Town Hall was destroyed can move to a free one.
- **Settlers**: player-like mobs with random names that belong to a settlement and can be given jobs. Sneak +
  right-click opens a settler's inventory (armor column + 36 storage slots); only its settlement's leader may.
- **Builder's Hut**: a workplace; its screen assigns one settler as its builder.
- **Blueprints**: operators capture existing structures around a city block (Blueprint Wand or `/blueprint`)
  and save them as structure files.
- **Builder's Wand**: players choose a blueprint, see a see-through preview, rotate it with R and place it as a
  **construction site**. Only the city block is placed; the rest of the area is marked to be built later.
- **Territory**: a settlement may only build in its chunks (5x5 around the Town Hall, plus 3x3 around each other
  building). Shown as a blue fence while holding the Builder's Wand, or toggled with B.

## Status (2026-10-20)

Everything above works and is covered by 50 GameTests that pass. Not checked in the real game yet, because GameTests
can't draw or click: the screens, the selection and site outlines, the Builder's Wand ghost preview, the territory
fence and the R/B keys, the Town Hall screen's "Move ... here" button, the settler inventory screen and armor shown
on settlers. Ask the user to try these.

**Not built yet / open questions** (ask the user before deciding):

- **Builders don't build yet.** The next step is the Builder's Hut's builder working through construction sites.
- Git: work is committed on `main` and pushed to GitHub (public). Commit only when asked.

## Working rules (from the user)

- **Never start the game yourself** (`runClient`, `runServer`, or piping commands into a server). Check changes
  only with automated tests ([0008](decisions/0008-automated-tests-only.md)). Things that need eyes in-game: ask the user.
- **Record every decision** in [decisions/](decisions/README.md) (one numbered file each, in the same turn as the
  change), and keep this file current.
- The GUI must not use texture assets: screens are drawn in code with the small framework in `client/ui`
  ([0011](decisions/0011-code-drawn-gui-framework.md)).

## Build and test

Gradle 8.8 can't run on this machine's default Java 25, but `gradle/gradle-daemon-jvm.properties` makes it start its
build process on the installed **JDK 21** by itself, whatever `JAVA_HOME` is. The foojay toolchain resolver downloads
the Java 17 that Minecraft 1.20.1 needs ([0033](decisions/0033-gradle-picks-its-own-jdk.md)).

```bash
./gradlew build              # compile and build the jar (build/libs/)
./gradlew runGameTestServer  # run all GameTests (about 30 s); exits non-zero if any fail
```

(`.\gradlew` in PowerShell.)

Run `runGameTestServer` in the foreground with a timeout, and afterwards check that no `java.exe` from it is left.
A stopped background run once left Gradle and a server running. The test log is easiest to read via
`./gradlew runGameTestServer > build/gametest.log` and then grepping for `failed!` and `required tests`.

## CI/CD

GitHub Actions ([0032](decisions/0032-ci-cd-github-actions.md)):

- `.github/workflows/ci.yml`: every push to `main` and every pull request builds the mod and runs all GameTests. The
  jar and the GameTest server log are uploaded as artifacts.
- `.github/workflows/release.yml`: to release, bump `mod_version` in `gradle.properties`, commit, then
  `git tag v<version>` and `git push origin v<version>`. It builds, tests, and creates a GitHub release with the jar
  (`-` in the version = pre-release). It also publishes to Modrinth/CurseForge once the repository has the secrets
  `MODRINTH_TOKEN`/`CURSEFORGE_TOKEN` and the variables `MODRINTH_PROJECT_ID`/`CURSEFORGE_PROJECT_ID`.
- `.github/actions/setup-build`: the shared JDK 17 + 21 and Gradle setup.
- `.github/dependabot.yml`: weekly updates for the Actions only.

Lint workflow changes with [actionlint](https://github.com/rhysd/actionlint) before committing them.

Known harmless noise: a javac note about deprecated API (overriding `Block.use`/`onRemove` is normal in 1.20.1), and startup stack traces about
`setAccessible`/netty/`server.properties` in the test server log.

## Repository layout

```
.github/                 CI/CD workflows, shared setup action, Dependabot
docs/                    these notes and the decision log
tools/                   make_test_structure.py (GameTest platforms), Resize.java (texture downscaling)
src/main/java/com/philgatzka/minecraftmod/
  MinecraftMod.java      all registrations (blocks, items, entities, menus), event wiring, client setup
  Config.java            common config (settlers on founding, territory radii)
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
