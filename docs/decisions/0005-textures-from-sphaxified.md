# 0005. Textures only from BDcraft's Sphaxified repo, at 128x128

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (source), Claude (which repo, which files, resolution)

## Context
The user asked for textures from https://github.com/BDcraft-Patches. Most repos there have no license and fall under
BDcraft's own terms, which generally don't allow shipping their textures inside another mod.

## Decision
Only use textures from **`BDcraft-Patches/Sphaxified`**. Its README says the textures are "released freely" and "not
restricted by the main BDcraft license".
- **Town Hall:** marble bricks (sides), marble paver (top), chiseled marble (bottom), all 128x128.
- **Builder's Hut:** cherry planks (sides), hickory (top), mahogany (bottom). They're 512x512 originals, scaled down
  to 128x128 with `tools/Resize.java`. Walnut and teak were tried first but looked grey or green.

Both sets are credited in `CREDITS.md`.

## Why
The licensing reason above. 128x128 keeps both blocks at the same detail level as the marble set.

## Consequences
In the default resource pack these blocks look sharper and more cartoon-like than vanilla's 16x16 blocks. They blend in
with a 128x PureBDcraft pack. Keep `CREDITS.md` when publishing. Item textures reuse vanilla items (blaze rod, stick)
instead of shipping new art.
