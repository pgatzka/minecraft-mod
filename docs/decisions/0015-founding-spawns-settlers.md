# 0015. Founding spawns a configurable number of settlers

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (spawn a configured amount, mapped to the settlement), Claude (details)

## Decision
- **Config:** `settlersOnFounding` in the common config, default 3, allowed 0–32.
- **Where they spawn:** on free spots within 3 blocks of the Town Hall, nearest first, spread over the spots found.
  A free spot has a solid block below and two free blocks above. If there are none, settlers go on top of the Town Hall.
- **Two-way link:** each settler stores the settlement id, and the settlement stores a `SettlerRecord` for each
  settler (entity UUID, first and last name, job, workplace).
- **Death:** a settler that dies is removed from the settlement (`SettlerEntity.die`). Settlers from the spawn egg
  belong to no settlement.

## Why
Storing names in the settlement lets screens list settlers whose chunks aren't loaded. Death removal keeps the list
true. Removal without death (for example `discard()`) is not handled.

## Consequences
The settlement's settler list can differ from the world if settlers are removed without dying.
