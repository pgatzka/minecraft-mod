# Decision log

Every decision about this mod gets a short record here, so anyone can see **why** something was done a certain way.
This covers design choices, interpretations of an unclear request, trade-offs and workarounds.

## Rules

- One file per decision: `NNNN-short-title.md`, numbered in order. Add it in the same change as the code.
- Never delete or rewrite an old decision. If a later decision replaces it, set the old one's status to
  `Superseded by NNNN` and link both ways.
- Say who decided: **user** (asked for it), or **Claude** (chose it, and should say why and what else was considered).
- Add the decision to the index below.

## Template

```markdown
# NNNN. Title

- Status: Accepted | Superseded by NNNN
- Date: YYYY-MM-DD
- Decided by: user | Claude

## Context
What problem or request led to this.

## Decision
What was decided, concretely.

## Why
The reasons, and the alternatives that were considered.

## Consequences
What follows from it: limitations, follow-ups, things to watch out for.
```

## Index

| # | Decision | Status |
|---|---|---|
| [0001](0001-minecraft-1.20.1-forge.md) | Minecraft 1.20.1 with Forge 47.4.10, from the official MDK | Accepted |
| [0002](0002-project-identity.md) | Mod id `minecraftmod`, package `com.philgatzka.minecraftmod` | Accepted |
| [0003](0003-build-on-jdk-21.md) | Run Gradle on JDK 21; Java 17 toolchain downloaded automatically | Accepted |
| [0004](0004-registration-in-main-class.md) | All registrations in the main class; MDK samples kept | Accepted (samples part superseded by 0026) |
| [0005](0005-textures-from-sphaxified.md) | Textures only from BDcraft's Sphaxified repo, at 128x128 | Accepted |
| [0006](0006-city-block-materials.md) | Town Hall is stone (pickaxe), Builder's Hut is wood (axe) | Accepted |
| [0007](0007-settler-entity.md) | How the Settler entity is built | Accepted |
| [0008](0008-automated-tests-only.md) | Check changes only with automated GameTests, never by starting the game | Accepted |
| [0009](0009-gametest-setup.md) | GameTest layout: main source set, generated platforms, isolation | Accepted |
| [0010](0010-slotless-menus.md) | Screens are slot-less menus with extra data, refreshed by reopening | Accepted |
| [0011](0011-code-drawn-gui-framework.md) | GUIs drawn in code with a small framework, no texture assets | Accepted |
| [0012](0012-networking.md) | SimpleChannel; the server checks everything; errors go back to the screen | Accepted |
| [0013](0013-settlement-storage-and-rules.md) | Settlements in SavedData; one per player, one per Town Hall | Accepted |
| [0014](0014-town-hall-removal-keeps-settlement.md) | Removing a Town Hall keeps its settlement | Accepted (supersedes an earlier choice) |
| [0015](0015-founding-spawns-settlers.md) | Founding spawns a configurable number of settlers | Accepted |
| [0016](0016-workplaces-and-jobs.md) | Generic workplaces; the settlement records who works where | Accepted |
| [0017](0017-blueprints-captured-from-world.md) | Blueprints are captured from the world as structure files | Accepted |
| [0018](0018-blueprint-permissions.md) | Only operators and singleplayer owners can capture blueprints | Accepted |
| [0019](0019-shared-blueprint-selection.md) | One blueprint selection per player, shared by wand and commands, outlined | Accepted |
| [0020](0020-builders-wand-and-preview.md) | Separate Builder's Wand with a ghost preview and server-side rotation | Accepted |
| [0021](0021-construction-sites.md) | Placing a blueprint places only the city block and records a site | Accepted |
| [0022](0022-territory-shape.md) | Territory is chunk squares around the Town Hall and buildings | Accepted |
| [0023](0023-buildings-only-in-own-territory.md) | Buildings other than Town Halls only inside your own territory | Accepted |
| [0024](0024-town-halls-not-in-other-territory.md) | Town Halls not inside another settlement's territory | Accepted |
| [0025](0025-docs-and-decision-log.md) | Handover notes in `docs/` and this decision log | Accepted |
| [0026](0026-remove-mdk-samples.md) | MDK sample content removed; creative tab is `minecraftmod:main` | Accepted |
| [0027](0027-blueprint-load-rotation.md) | `/blueprint load` takes an optional rotation | Accepted |
| [0028](0028-settlement-moves-to-new-town-hall.md) | A settlement without a Town Hall can move to a free one | Accepted |
| [0029](0029-clients-know-other-territories.md) | Clients know other settlements' territories | Accepted |
| [0030](0030-settler-inventory.md) | Settlers have a 4x10 inventory with an armor column | Accepted (tint superseded by 0031) |
| [0031](0031-vanilla-armor-slot-icons.md) | Empty armor slots show vanilla's armor icons | Accepted |
