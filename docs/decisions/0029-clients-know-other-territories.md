# 0029. Clients know other settlements' territories

- Status: Accepted
- Date: 2026-10-20
- Decided by: user ("do the small cleanups", which included this open item), Claude (how)

## Context
[0024](0024-town-halls-not-in-other-territory.md): the client only knew the leader's own territory. So the Builder's
Wand preview could show green for a Town Hall site that the server then refused.

## Decision
- `TerritoryPacket` carries two chunk sets: the player's own settlement's territory, and the combined territory of every
  other settlement in the player's dimension.
- When any settlement's territory changes, the server sends this to **every player in that dimension**
  (`Territory.syncChanged`, replacing `syncLeader`). It also still sends it on login and on dimension change.
- The preview uses the same rules as `ConstructionSites.place`. A building must be fully inside the player's own
  territory. A Town Hall must not touch another settlement's territory.

## Why
It's the smallest change that makes the preview match the server. The client only needs to know whether the chunk
belongs to someone else, not whose it is, so one combined set is enough. The server's error message still names the
settlement.

## Consequences
- Every player now learns where all settlements in their dimension are. That's fine for a cooperative settlement mod.
  If hiding them ever matters, only the preview depends on it.
- The territory fence still shows only the player's own territory. Showing other territories (for example, in red)
  would now be easy.
