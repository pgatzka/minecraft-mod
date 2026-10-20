# 0019. One blueprint selection per player, shared by wand and commands, outlined

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (see the marked area; save and load by command), Claude (design)

## Decision
- **Storage:** the two selection corners are stored per player on the server (`PlayerSelections`), in memory only,
  like WorldEdit. They're forgotten on logout or server stop.
- **Shared:** the Blueprint Wand sets them (right-click = corner 1, sneak + right-click = corner 2) and so do
  `/blueprint pos1|pos2`. Corners from another dimension are ignored.
- The wand uses `onItemUseFirst`, so clicking chests or Town Halls sets a corner instead of opening them.
- **Outline:** every change is sent to the player's client (`SelectionSyncPacket`), and `SelectionRenderer` draws
  corner 1 in orange, corner 2 in blue, and the box in green, or red when it's over 48 blocks. It stays until
  `/blueprint clear`.

## Why
Keeping the selection on the server instead of on the item lets commands and the wand share it. Storing it only in
memory avoids save-data clean-up for something short-lived.

## Consequences
Selections don't survive restarts.
