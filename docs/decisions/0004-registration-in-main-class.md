# 0004. All registrations in the main class; MDK samples kept

- Status: Accepted
- Date: 2026-10-20
- Decided by: Claude

## Context
Blocks, items, block entities, entities and menus need registering. The MDK registers its samples with
`DeferredRegister`s in the main class.

## Decision
All `DeferredRegister`s and `RegistryObject`s live in `MinecraftMod.java`, next to the MDK samples, each with a short
comment. Game logic goes in separate classes and packages. The MDK samples (example block, example item, sample config
values) were kept as working references. The creative tab got a title, which the MDK leaves out.

## Why
It follows the existing code's style and keeps every id in one place. With about 15 registrations, a split into
`ModBlocks`/`ModItems` classes wasn't worth it yet.

## Consequences
`MinecraftMod.java` grows with every feature. Split it into registry classes once it gets hard to read. The MDK samples
are still shipped and should be removed before a release (open item in docs/README.md).
