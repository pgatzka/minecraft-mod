# 0026. MDK sample content removed

- Status: Accepted. It replaces the "MDK samples kept" part of [0004](0004-registration-in-main-class.md).
- Date: 2026-10-20
- Decided by: user ("do the small cleanups"), Claude (what counts as sample content, the new tab id)

## Context
[0004](0004-registration-in-main-class.md) kept the Forge MDK's samples as working references. They were an open item
in docs/README.md, to be removed before a release.

## Decision
Removed: the example block and its item, the example food item, the sample config values (`logDirtBlock`,
`magicNumber`, `magicNumberIntroduction`, `items`), the "HELLO" log lines in common, client and server setup, and their
lang entries. The creative tab is now the mod's own tab: id `minecraftmod:main` (was `minecraftmod:example_tab`),
with the Town Hall as its icon. All registrations stay in `MinecraftMod.java`.

## Why
Nothing used them. The real registrations now serve as the code examples.

## Consequences
- Worlds made before this change lose any example blocks and items in them. Nobody plays those worlds yet.
- Existing `minecraftmod-common.toml` files still contain the old keys. Forge logs a warning and rewrites the file.
