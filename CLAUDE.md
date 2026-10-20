# Minecraft Mod (Forge 1.20.1)

Start with `docs/README.md` (status, open questions, build/test commands), then `docs/architecture.md` and
`docs/testing.md`. Why things are the way they are: `docs/decisions/`.

Rules for working on this project:

- Never start the game yourself (no `runClient`/`runServer`). Check changes only with
  `./gradlew runGameTestServer`, run in the foreground with `JAVA_HOME` set to JDK 21, then make sure no test server
  `java.exe` is left running.
- Record every decision you make in `docs/decisions/NNNN-title.md` (template and index in `docs/decisions/README.md`)
  in the same change, and keep `docs/README.md` current.
- GUIs use the code-drawn framework in `client/ui`, with no texture assets.
- New GameTests that found settlements or place construction sites need their own batch and
  `clearSettlementsAndSites(helper)` (see `docs/testing.md`).
- Commit only when asked.
