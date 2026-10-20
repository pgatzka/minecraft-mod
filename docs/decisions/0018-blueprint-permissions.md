# 0018. Only operators and singleplayer owners can capture blueprints

- Status: Accepted
- Date: 2026-10-20
- Decided by: Claude

## Decision
Saving blueprints, the Blueprint Wand and all `/blueprint` commands need permission level 2 or being the singleplayer
world's owner (`Blueprints.canUse`). Placing blueprints with the Builder's Wand needs no permission.

## Why
Saving writes files on the server and `/blueprint load` places whole structures, so ordinary players on a public
server shouldn't have it. The owner exception makes it work in singleplayer without cheats enabled, where the
permission level is 0.

## Consequences
Capturing is an admin or creator tool. Normal gameplay goes through the Builder's Wand ([0020](0020-builders-wand-and-preview.md)).
