# 0001. Minecraft 1.20.1 with Forge 47.4.10, from the official MDK

- Status: Accepted
- Date: 2026-10-20
- Decided by: Claude (the user asked for "the version you are most familiar with")

## Context
The project had to start on some Minecraft and Forge version.

## Decision
Minecraft **1.20.1** with Forge **47.4.10**, Forge's "recommended" build for 1.20.1 at the time (the latest was
47.4.28). The project was created from the official MDK (`forge-1.20.1-47.4.10-mdk.zip`): Gradle 8.8, ForgeGradle 6 and
Mojang's official mappings.

## Why
1.20.1 is the most widely used and best documented Forge version, with many mods to learn from, and the one Claude
knows best. The recommended build is the stable choice; the newer "latest" builds were skipped for stability. Using the
MDK instead of a hand-written build keeps the project close to what Forge documents.

## Consequences
APIs are the 1.20.1 ones: `SimpleChannel` networking, `FriendlyByteBuf` packets, `IForgeMenuType`,
`ResourceLocation.fromNamespaceAndPath` (backported in 47.4), and so on. Moving to NeoForge or a newer version later
would mean porting these.
