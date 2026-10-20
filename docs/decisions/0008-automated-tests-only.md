# 0008. Check changes only with automated GameTests, never by starting the game

- Status: Accepted
- Date: 2026-10-20
- Decided by: user (no manual launching), Claude (GameTests as the tool)

## Context
Claude once started a dedicated server and piped `/summon` commands into it to check the Settler. The user stopped it
and said: "dont start the server or client urself, do not do that. only every use automated tests". Stopping that task
also left Gradle and a Minecraft server running in the background, which had to be ended by hand.

Research into large mods (Create, Mekanism, AE2, Botania, Immersive Engineering, Refined Storage 2, Tinkers'
Construct) showed two layers: JUnit for plain logic, and Minecraft's GameTest framework for in-world behavior, run in CI
with `runGameTestServer`.

## Decision
- Check every change with GameTests (`./gradlew runGameTestServer`), and with JUnit once there is plain logic worth
  testing on its own.
- Never run `runClient`/`runServer` or start the game any other way.
- Run the test server in the foreground with a timeout, then check that no `java.exe` from it is left.
- Things only visible in-game (rendering, screens, keys): keep their logic in tested shared code, and ask the user to look.

## Why
The user's explicit wish. GameTests also give repeatable pass/fail results that could run in CI.

## Consequences
Visual features ship without being seen by Claude. docs/README.md lists what still needs checking in-game.
