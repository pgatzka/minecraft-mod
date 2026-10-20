# 0017. Blueprints are captured from the world as structure files

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (capture tool around an existing structure, the area must contain a city block, save as a file and
  load by command), Claude (format and rules)

## Context
Structures were needed and the user said "I am not a good builder". Before Claude finished looking at reusing
vanilla's village templates, the user chose a capture tool instead.

## Decision
- **Format:** a blueprint is a vanilla structure file (`.nbt` written from `StructureTemplate`) plus a
  `minecraftmod:blueprint` entry holding the city block's id and its position inside the structure (the "anchor").
  Vanilla structure blocks can still open these files.
- **Area rules:** at most 48 blocks per side (vanilla's structure-block limit), and **exactly one** block from the
  tag `minecraftmod:city_blocks` (Town Hall, Builder's Hut). Zero or several are refused, since several would make it
  unclear what the blueprint is for.
- **Ids and names:** `minecraftmod:blueprints/<city block>/<name>`, where the name uses `a-z`, `0-9` and `_`, up to 32
  characters. Saving over an existing name is refused.
- **Storage:**
  - Saved to `<world>/generated/minecraftmod/structures/...`, where vanilla also keeps saved structures.
  - Files shipped in `data/<ns>/structures/blueprints/` load the same way, and a world file with the same id wins.
- **Cleaning:** settlement ids in captured block entities are removed, so a placed copy is unclaimed.
- **Commands:** `/blueprint pos1|pos2 [pos]`, `clear`, `save <name>`, `load <id> [pos]` and `list`. `load` places the
  whole structure with its lower corner at the given position, and selects what it placed.

## Why
A capture tool lets the user use structures they find or get, without building or writing code. Using the vanilla
format keeps the files compatible with vanilla tools, and the extra entry is ignored by them.

## Consequences
- Overwriting needs deleting the file by hand.
- `load` has no rotation.
- Shipping a blueprint means copying its file into `src/main/resources/data/minecraftmod/structures/blueprints/<city block>/`.
