# 0003. Run Gradle on JDK 21; Java 17 toolchain downloaded automatically

- Status: Superseded by [0033](0033-gradle-picks-its-own-jdk.md)
- Date: 2026-10-20
- Decided by: Claude

## Context
The machine's default Java is 25, and Gradle 8.8 (from the MDK) can't run on it. JDK 21 is also installed. Minecraft
1.20.1 needs Java 17.

## Decision
Run Gradle with `JAVA_HOME` set to `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`. The MDK's
foojay toolchain resolver downloads Java 17 into `~/.gradle/jdks` for compiling and running Minecraft.
`org.gradle.java.home` is **not** put in `gradle.properties`.

## Why
This works without installing anything. A machine-specific path in `gradle.properties` would break the build for anyone
else. Upgrading Gradle to run on Java 25 would have meant leaving the MDK's tested setup.

## Consequences
Every Gradle command needs `JAVA_HOME` set to JDK 21 (see docs/README.md). If the user's run configuration lacks it,
`./gradlew` fails on Java 25.
