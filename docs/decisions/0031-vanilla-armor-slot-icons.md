# 0031. Empty armor slots show vanilla's armor icons

- Status: Accepted. It replaces the bluish armor slot tint from [0030](0030-settler-inventory.md).
- Date: 2026-10-20
- Decided by: user ("use vanilla armor slot icons")

## Decision
The settler inventory's armor slots show vanilla's empty armor slot icons (helmet, chestplate, leggings, boots outlines)
while they're empty. These are the same sprites the player inventory uses. Each armor slot sets them with Forge's
`Slot.setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_*)`, and vanilla's container screen draws
them. The slots themselves are still drawn in code, all with the normal slot color.

## Why
It's what the user asked for, and players already know these icons from their own inventory.

## Consequences
This is an exception to [0011](0011-code-drawn-gui-framework.md): the screen now uses vanilla sprites. The mod still
ships no GUI textures of its own.
