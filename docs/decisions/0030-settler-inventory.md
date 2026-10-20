# 0030. Settlers have a 4x10 inventory with an armor column

- Status: Accepted
- Date: 2026-10-20
- Decided by: user ("settlers have an inventory of 4x10, where the most left slots is the armor slots. shift+rightclick
  opens a settlers inventory allowing me to add or remove items"), Claude (the details below)

## Decision
- **Layout:** 4 rows x 10 columns. The left column holds the four armor slots, head to feet. The other 9 columns are
  36 storage slots. The player's inventory and hotbar sit below, aligned with the storage columns
  (`SettlerInventoryMenu`, `SettlerInventoryScreen`).
- **Armor is the settler's real equipment.** The armor slots read and write the mob's equipment slots, so the settler
  wears the armor, gets its protection, and `SettlerRenderer` draws it (`HumanoidArmorLayer`). An armor slot only takes
  armor for that body part, one piece at a time.
- **Storage** is a 36-slot container on `SettlerEntity`, saved in its NBT as `Storage` with slot numbers, so items
  stay where they were put.
- **Opening:** sneak + right-click the settler (`mobInteract` with `isSecondaryUseActive`). Only the leader of the
  settler's settlement may open it. A settler without a settlement (spawn egg, `/summon`) is open to anyone. The
  screen closes when the settler dies or the player moves more than 8 blocks away.
- **Shift-click:** from the settler, items go to the player. From the player, armor goes to its armor slot if that's
  free, and everything else goes to storage.
- **Death:** storage and armor drop on death. Armor uses guaranteed drop chances, so it drops undamaged.
- **Drawing:** slots are drawn in code (`UiDraw.slot`, `UiTheme.SLOT_*`). Armor slots have a bluish tint instead of
  vanilla's empty-armor icons, because those are textures ([0011](0011-code-drawn-gui-framework.md)).

## Why
- Real equipment slots make armor do what the player expects: the settler looks and acts armored. A separate
  "armor" container would only be storage with a different label.
- Leader-only access matches who may give settlers jobs ([0016](0016-workplaces-and-jobs.md)). It keeps other players
  from emptying your settlers.
- Dropping everything on death means the player never loses items they handed over.

## Consequences
- This is the first menu **with slots**, an exception to [0010](0010-slotless-menus.md). Vanilla's slot sync keeps it
  up to date, so it doesn't need reopening.
- The inventory is stored on the entity, not in `SettlementData`. Code that needs a settler's items (for example,
  builders fetching materials) needs the entity loaded.
- Hands (main hand, off hand) are not part of the inventory yet.
