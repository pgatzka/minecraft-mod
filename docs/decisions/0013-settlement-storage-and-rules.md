# 0013. Settlements in SavedData; one per player, one per Town Hall

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (players found settlements at the Town Hall; one per player), Claude (the rest)

## Decision
- All settlements are kept in `SettlementData`, a `SavedData` on the overworld (`minecraftmod_settlements.dat`). A Town
  Hall's block entity only stores the settlement's id.
- **One per player:** a player can lead **one** settlement, checked through `getByLeader`.
- **One per Town Hall:** a Town Hall hosts **one** settlement.
- **Names:** formatting codes are stripped (`SharedConstants.filterText`), surrounding spaces are trimmed, and the
  name must be 1–32 characters. Duplicate names are allowed.
- The settlement remembers its leader's name at founding, to show in screens.

## Why
Global server data can answer "does this player already lead a settlement?" and works while the Town Hall's chunk is
unloaded. Requiring unique names wasn't asked for and was left out.

## Consequences
A settlement has exactly one member who can act, its leader. Other players can view but not manage it.
