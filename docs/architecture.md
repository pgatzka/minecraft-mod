# Architecture

How the parts of the mod fit together. File paths are relative to `src/main/java/com/philgatzka/minecraftmod/`.

## Registration and wiring

Everything is registered in `MinecraftMod.java` with `DeferredRegister`s (blocks, items, block entities, entity
types, menu types, creative tab), the MDK's pattern ([0004](decisions/0004-registration-in-main-class.md)).
The constructor also registers network packets (`ModNetwork.register()`) and Forge-bus listeners (commands, player
login/logout, dimension changes, block placement). The nested `ClientModEvents` class registers screens, the
Settler renderer and key mappings. Other client-only event handlers are `@Mod.EventBusSubscriber(value = Dist.CLIENT)`
classes in `client/` (`SelectionRenderer`, `ConstructionRenderer`).

## Server-side state

All persistent game state lives in `SavedData` on the overworld, keyed by ids, not inside blocks or entities:

| Saved data | File in `<world>/data/` | Holds |
|---|---|---|
| `settlement/SettlementData` | `minecraftmod_settlements.dat` | `Settlement`s: name, leader, dimension, Town Hall position (nullable), settler records (`SettlerRecord`: entity id, names, job, workplace), building positions |
| `construction/ConstructionSiteData` | `minecraftmod_construction_sites.dat` | `ConstructionSite`s: blueprint id, dimension, city block position, rotation, area, settlement id |

Blocks and entities only store **references**: `TownHallBlockEntity` and `WorkplaceBlockEntity` store a settlement
id, and `SettlerEntity` stores its settlement id. The settlement is the single source of truth for who works where
([0016](decisions/0016-workplaces-and-jobs.md)), so it works while chunks are unloaded.

Rules live in static service classes that both packets and GameTests call:

- `settlement/Settlements`: founding (`tryCreate`), workplace claiming, job assignment, and what happens when Town
  Halls, workplaces or settlers are removed.
- `settlement/Territory`: computing territory chunks, the "who owns this chunk" checks, the hand-placement event
  check, and syncing the leader's territory to their client.
- `blueprint/Blueprints`: checking an area, saving, loading, listing and placing blueprint files.
- `construction/ConstructionSites`: placing a blueprint as a construction site, plus syncing site outlines to clients.

Each returns a small result record (`CreateResult`, `Result`, `SaveResult`, ...) with either the outcome or a
translatable error `Component`, which the caller shows to the player.

## Screens (menus + code-drawn UI)

Every screen is a **slot-less `AbstractContainerMenu`** opened with `NetworkHooks.openScreen`, carrying an "info"
record written into the open packet ([0010](decisions/0010-slotless-menus.md)):

| Menu | Info | Screen | Opened by |
|---|---|---|---|
| `TownHallMenu` | `TownHallInfo` | `TownHallScreen` | right-click Town Hall |
| `WorkplaceMenu` | `WorkplaceInfo` | `WorkplaceScreen` | right-click Builder's Hut (any `WorkplaceBlock`) |
| `BlueprintMenu` | `BlueprintMenu.Info` | `BlueprintScreen` | Blueprint Wand, right-click air |
| `BlueprintPickerMenu` | `BlueprintPickerMenu.Info` | `BlueprintPickerScreen` | Builder's Wand, right-click air |

The menu tells the server which block or selection a request is about (`player.containerMenu`) and whether the
player is still near it (`stillValid`). After a successful action the server **reopens** the menu to refresh it.

Screens extend `client/ui/UiScreen` ([0011](decisions/0011-code-drawn-gui-framework.md)): a code-drawn panel and
title, widgets stacked with `UiColumn` (labels, `EditBox` text fields, `UiButton`s, button rows,
`UiSelectionList`s), and an error line filled by `UiScreen.showError`. `UiLabel` never draws past the lines it
reserved. All colors are in `UiTheme`.

## Networking

`network/ModNetwork` is a Forge `SimpleChannel` ([0012](decisions/0012-networking.md)).

| Packet | Direction | Purpose |
|---|---|---|
| `CreateSettlementPacket` | to server | found a settlement at the open Town Hall |
| `AssignWorkerPacket` | to server | set or clear the open workplace's worker |
| `SaveBlueprintPacket` | to server | save the open Blueprint screen's area |
| `SelectBlueprintPacket` | to server | put a blueprint on the held Builder's Wand |
| `RotateBlueprintPacket` | to server | rotate the held Builder's Wand's blueprint |
| `RequestBlueprintPreviewPacket` | to server | ask for a blueprint's blocks for the preview |
| `ScreenErrorPacket` | to client | show an error in the open `UiScreen` |
| `SelectionSyncPacket` | to client | the player's blueprint selection corners, for the outline |
| `BlueprintPreviewPacket` | to client | a blueprint's blocks (`BlueprintBlocks`) for the ghost preview |
| `ConstructionSitesPacket` | to client | site outlines in the player's dimension |
| `TerritoryPacket` | to client | the chunks of the settlement the player leads |

The server checks every request again (permissions, distance, rules). Packets that reach client code go through
`DistExecutor` into `client/ClientPacketHandlers`, so client classes never load on a dedicated server. Client-side
copies of server data live in `client/ClientSelection` and `client/ClientBlueprints`, and are cleared on disconnect.

## Blueprints and construction

1. **Capture** ([0017](decisions/0017-blueprints-captured-from-world.md)). An operator marks two corners
   (`PlayerSelections`, shared by the Blueprint Wand and `/blueprint pos1|pos2`). `Blueprints.save` checks the area
   (at most 48 blocks per side, exactly one block from the `minecraftmod:city_blocks` tag), captures a vanilla
   `StructureTemplate`, strips settlement ids, adds metadata (`minecraftmod:blueprint`: city block and anchor) and
   writes `<world>/generated/minecraftmod/structures/blueprints/<city block>/<name>.nbt`. Files shipped in
   `data/<ns>/structures/blueprints/...` load the same way; world files win.
2. **Choose and preview** ([0020](decisions/0020-builders-wand-and-preview.md)). The Builder's Wand stores the
   blueprint id and rotation in its NBT. The client gets the blueprint's blocks (`BlueprintBlocks`) and draws them
   see-through with `GhostVertexConsumer` at `BlueprintPlacement`, which turns the blueprint around its anchor (the
   city block) so the anchor lands on the targeted block. The same math is used server-side.
3. **Place** ([0021](decisions/0021-construction-sites.md)). `ConstructionSites.place` checks the area, territory
   and costs, places only the rotated city block and stores a `ConstructionSite`.

## Territory

`Territory.of(settlement)` = chunks within `townHallTerritoryRadius` (default 2, so 5x5) of the Town Hall's chunk,
plus chunks within `buildingTerritoryRadius` (default 1, so 3x3) of each building in `Settlement.buildings()`
([0022](decisions/0022-territory-shape.md)). Buildings are workplaces claimed by the settlement. Placement rules are in
[0023](decisions/0023-buildings-only-in-own-territory.md) and [0024](decisions/0024-town-halls-not-in-other-territory.md).
The leader's client gets their territory (`TerritoryPacket`) and `ConstructionRenderer` draws a fence along its border.

## Adding a new building type (workplace)

1. Add the job to `settlement/SettlerJob` (and `job.minecraftmod.<name>` to the lang file).
2. Add a `WorkplaceBlockEntity` subclass that returns the job, and a `WorkplaceBlock` subclass creating it.
3. Register block, item and block entity type in `MinecraftMod`. Add the block to `data/minecraftmod/tags/blocks/city_blocks.json`,
   plus blockstate, models, loot table, tool tag and lang name.
4. The workplace screen, claiming, territory, blueprints and construction sites then work without further code.
