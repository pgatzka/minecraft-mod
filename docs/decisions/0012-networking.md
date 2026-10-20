# 0012. SimpleChannel; the server checks everything; errors go back to the screen

- Status: Accepted
- Date: 2026-10-20
- Decided by: Claude

## Decision
- One Forge `SimpleChannel` (`network/ModNetwork`, protocol "1"). Each packet is a record with `encode`/`decode`/`handle`
  and runs on the main thread.
- **The server checks every request again:** permissions, the open menu, distance, and the game rules (through the
  same service methods the GameTests call). Client-side checks only decide what the UI shows (disabled buttons,
  red previews).
- Rejections go back as `ScreenErrorPacket`, shown in the open `UiScreen`'s error line. It was first named
  `SettlementErrorPacket` and renamed once other screens used it. Results without a screen (wand actions) go to the
  action bar.
- Packets that reach client code call `ClientPacketHandlers` through `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)`,
  so client classes never load on a dedicated server. The GameTest server, which is a dedicated server, confirms this
  on every run.
- String fields have a generous size limit (256), so modified clients can't send huge strings. The real limits are
  checked in the services.

## Why
Clients can be modified, so the server must not trust them. Chat messages aren't visible while a screen is open, which
is why errors are shown inside the screen.

## Consequences
Every new action needs a packet, a registration in `ModNetwork` and a server-side check.
