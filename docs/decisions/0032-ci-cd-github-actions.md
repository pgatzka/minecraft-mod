# 0032. CI/CD with GitHub Actions

- Status: Accepted
- Date: 2026-10-20
- Decided by: user ("setup comprehensive ci cd"), Claude (design)

## Decision
- **CI** (`.github/workflows/ci.yml`): on every push to `main`, every pull request, and on demand. On `ubuntu-latest`
  it validates the Gradle wrapper, runs `./gradlew build` and `./gradlew runGameTestServer`, and uploads the jar.
  It always uploads the GameTest server log (`run/logs/`), even when tests fail. A newer push to the same pull request
  cancels the older run.
- **CD** (`.github/workflows/release.yml`): pushing a tag `v<mod_version>` runs the same build and tests, then creates
  a GitHub release with the jar and generated release notes. Tags with a `-` (for example `v1.1.0-beta.1`) become
  pre-releases. The workflow fails if the tag doesn't match `mod_version` in `gradle.properties`. If
  `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` secrets exist (with `MODRINTH_PROJECT_ID` / `CURSEFORGE_PROJECT_ID`
  variables), it also publishes to those sites with `Kir-Antipov/mc-publish`. Without them, that step is skipped.
- **Shared setup** (`.github/actions/setup-build`): installs Temurin 17 and 21, sets up `gradle/actions/setup-gradle`
  (caches Gradle and the ForgeGradle/Minecraft downloads), and writes `org.gradle.java.home` (JDK 21) plus
  `org.gradle.java.installations.fromEnv=JAVA_HOME_17_X64` into the runner's `~/.gradle/gradle.properties`.
- **Dependabot** keeps only the GitHub Actions up to date, grouped into one weekly pull request.
- `gradlew` is now executable in git (mode 100755), which Linux runners need.

## Why
- The GameTests are the project's only automatic check ([0008](0008-automated-tests-only.md)), so every change runs
  them. They run headless in about 30 seconds, so a separate test job wasn't worth a second Minecraft setup.
- Settings in the Gradle user home take priority over the project's `gradle.properties`. That makes CI independent of
  the machine-specific `org.gradle.java.home` line that is currently in the project file, without touching that file.
- Gradle, ForgeGradle and Forge are pinned to the 1.20.1 MDK's tested setup ([0001](0001-minecraft-1.20.1-forge.md),
  [0003](0003-build-on-jdk-21.md)). Automated bumps (for example, Gradle 9, which ForgeGradle 6 doesn't support)
  would only produce broken pull requests.
- Publishing to mod sites is optional, because it needs accounts and project ids that only the user can create.
- The workflows were checked with `actionlint` 1.7.12.

## Consequences
- The workflows run only after the repository is pushed to GitHub. Branch protection (require "Build and GameTests"
  before merging) has to be turned on in the repository settings.
- `mod_license` is "All Rights Reserved". Publishing releases under that license is the user's call.
- Releasing is: bump `mod_version` in `gradle.properties`, commit, tag `v<version>`, then push the tag.
