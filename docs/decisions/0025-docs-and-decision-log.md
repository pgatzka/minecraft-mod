# 0025. Handover notes in `docs/` and this decision log

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (docs so a new session can start right away; every decision tracked in docs/decisions, including
  past ones), Claude (structure)

## Decision
- `docs/README.md`: start here. What the mod is, its status and open questions, working rules, build and test
  commands, and the folder layout.
- `docs/architecture.md`: how the systems fit together.
- `docs/testing.md`: how the tests work and their gotchas.
- `docs/decisions/`: one numbered record per decision, with an index and a template. Decisions 0001–0024 were written
  down afterwards for everything decided before this log existed.
- `CLAUDE.md` in the project root, which Claude Code loads by itself, points to these docs and repeats the working rules.
- The tools that had only been in a temporary folder were moved into `tools/` (`make_test_structure.py`, `Resize.java`).

## Consequences
Every change from now on includes its decision record and keeps docs/README.md's status and open items current.
