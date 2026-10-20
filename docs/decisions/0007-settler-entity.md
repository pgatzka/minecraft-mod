# 0007. How the Settler entity is built

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (names from fixed lists, player health, Steve skin, spawn egg), Claude (the rest)

## Decision
- `SettlerEntity extends PathfinderMob`, sized like a player (0.6x1.8), category `CREATURE`.
- **Health:** 20, the player's value. The test reads it from `Player.createAttributes()` instead of hard-coding it.
- **Names:** first and last name from the fixed lists in `SettlerNames`. They're chosen in the constructor on the
  server, so every way of creating a settler (spawn egg, `/summon`, founding) gets a name, and saved settlers overwrite
  them when loaded. They're stored in synced entity data so the client can show them, and saved to NBT.
- `getTypeName()` returns the full name, so chat and death messages use it, and `shouldShowName()` is always true,
  like a player's name tag.
- **Looks:** `HumanoidMobRenderer` with `PlayerModel` and `DefaultPlayerSkin.getDefaultSkin()` (Steve).
- **Behavior:** it never despawns (`removeWhenFarAway` false), uses player hurt and death sounds, and has simple goals
  (float, panic, wander, look at player).

## Why
Choosing names in the constructor covers every spawn path without relying on `finalizeSpawn`, which some paths skip.
Settlers are meant to be residents, so they mustn't despawn like animals.

## Consequences
Settlers pile up if nothing removes them. This matters in the kept GameTest world (see [0009](0009-gametest-setup.md)).
