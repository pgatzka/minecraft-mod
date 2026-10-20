# 0010. Screens are slot-less menus with extra data, refreshed by reopening

- Status: Accepted
- Date: 2026-10-20
- Decided by: Claude

## Context
The Town Hall, workplace and blueprint screens have no inventory slots, but act on a specific block or selection on
the server.

## Decision
Each screen has a slot-less `AbstractContainerMenu`, registered with `IForgeMenuType.create` and opened with
`NetworkHooks.openScreen`. The server writes an "info" record (`TownHallInfo`, `WorkplaceInfo`, ...) into the open
packet for the client to display. Requests from the screen are handled for `player.containerMenu`, after checking
`stillValid`, which means the block still exists and the player is within 8 blocks. After a successful action the
server **reopens** the menu, so the screen shows fresh data.

## Why
A plain client-only screen plus a packet would leave the server unsure which block the player means and whether they're
still near it. The menu system gives that for free and closes the screen when the player walks away. Reopening is
simpler than keeping screen data in sync.

## Consequences
A refresh briefly closes and reopens the screen, and text typed before it is lost. Only successful actions reopen;
errors keep the screen open (see [0012](0012-networking.md)).
