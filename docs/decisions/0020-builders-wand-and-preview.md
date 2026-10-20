# 0020. Separate Builder's Wand with a ghost preview and server-side rotation

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (place blueprints with a wand and see the actual structure), Claude (design)

## Decision
- **A separate item:** the **Builder's Wand** for players, while the Blueprint Wand stays the operator's capture tool
  ([0018](0018-blueprint-permissions.md)). The user said "builders wand", so the new item got that name.
- **Choosing:** right-click the air to open a picker listing every blueprint. The chosen id and the rotation are stored
  in the item's NBT, so the server controls them and they sync to the client by themselves.
- **Rotating:** the R key sends `RotateBlueprintPacket`, and the server turns it 90° clockwise. The key can be changed
  in Controls under "Minecraft Mod".
- **Preview data:** the client gets the blueprint's non-air blocks once (`BlueprintPreviewPacket`). It's sent when
  choosing, or requested when missing (for example after rejoining), and cached until disconnect.
- **Drawing the preview:** block models are drawn with `Sheets.translucentCullBlockSheet()` through
  `GhostVertexConsumer`, which halves alpha, at full brightness. They're tinted green when the site can be placed and
  red when it can't.
  - Blocks hidden on all six sides by full blocks of the blueprint are skipped.
  - Block entities (chests and similar) are drawn only through their block model.
- **Placement math:** `BlueprintPlacement` rotates the blueprint around its anchor, so the city block lands on the
  targeted spot: the clicked block if it can be replaced (like grass), otherwise the block in front of the clicked face.
  The client and server use the same code.

## Why
Keeping the rotation on the item means the server decides where the site goes, without trusting coordinates sent by
the client. Sharing the placement math guarantees that what's shown is what gets marked.

## Consequences
Blocks that render only through a block entity renderer don't show in the preview. Very large blueprints may draw slowly.
