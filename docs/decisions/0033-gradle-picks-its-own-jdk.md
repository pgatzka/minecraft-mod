# 0033. Gradle picks its own JDK 21; no JAVA_HOME or java.home needed

- Status: Accepted. It replaces [0003](0003-build-on-jdk-21.md).
- Date: 2026-10-20
- Decided by: user ("setup foojay resolver convention" instead of the machine-specific `org.gradle.java.home`),
  Claude (how)

## Context
Gradle 8.8 can't run on this machine's default Java 25. 0003 required setting `JAVA_HOME` to JDK 21 for every Gradle
command. Later, `org.gradle.java.home=C:/Program Files/...` was added to `gradle.properties`, a machine-specific path
that 0003 had ruled out and that breaks on any other machine (including CI). The user asked for the foojay resolver
convention instead.

## Decision
- `gradle/gradle-daemon-jvm.properties` contains `toolchainVersion=21`. This is Gradle 8.8's (incubating) daemon JVM
  criteria: whatever Java runs `./gradlew`, Gradle starts its build process on an installed JDK 21 that it finds by
  itself (on Windows, from the registry where the Adoptium installer registers it).
- `org.gradle.java.home` is removed from `gradle.properties`.
- The foojay resolver convention (already in `settings.gradle`) still downloads the Java 17 toolchain that compiles and
  runs Minecraft.
- CI installs JDK 17 and 21 and points Gradle at both with `org.gradle.java.installations.fromEnv`.

## Why
- Checked on this machine: with `JAVA_HOME` on JDK 25, `./gradlew --version` reports the launcher on 25, and
  `build` and `runGameTestServer` run on 21. All 50 GameTests passed.
- foojay can't provide Gradle's own JVM in Gradle 8.8. Downloading the daemon JVM automatically needs Gradle 8.13+.
  That would mean leaving the 1.20.1 MDK's Gradle version, which ForgeGradle 6 is tested with.

## Consequences
- Gradle commands work from any shell, IDE or CI without setting `JAVA_HOME`.
- A JDK 21 must be installed (it doesn't get downloaded). Without one, Gradle fails with a message about the daemon JVM
  criteria.
- The daemon JVM criteria are incubating in 8.8. Gradle may print a note about that.
