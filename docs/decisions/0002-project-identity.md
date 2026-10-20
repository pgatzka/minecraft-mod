# 0002. Mod id `minecraftmod`, package `com.philgatzka.minecraftmod`

- Status: Accepted
- Date: 2026-10-20
- Decided by: Claude

## Context
The MDK ships with `examplemod` placeholders. The user didn't give a mod name.

## Decision
Mod id `minecraftmod` and display name "Minecraft Mod" (from the folder name `minecraft-mod`), package
`com.philgatzka.minecraftmod`, author "Phil Gatzka". Set in `gradle.properties`; the main class is `MinecraftMod`.

## Why
They are neutral placeholders taken from the project folder and the user's name. They're easy to change before release.

## Consequences
Renaming later means changing `gradle.properties`, `MinecraftMod.MODID`, the package folders, the
`assets/minecraftmod` and `data/minecraftmod` folders, and every `minecraftmod:` id in JSON and the lang file.
Saved blueprints and settlement data also use the namespace.
