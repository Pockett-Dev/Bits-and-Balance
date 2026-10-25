# Bits and Balance

Over 100 small mod ideas packed into one highly configurable mod. New features, balance changes, new gameplay mechanics, etc.

## Building

Use the checked-in Gradle wrapper with a JDK 21 or newer. Minecraft 1.21.x targets use Java 21 toolchains; 26.x targets use Java 25. Select Gradle's JVM through `JAVA_HOME` or your IDE.

```powershell
.\gradlew.bat verifyMaintainedTargets --console=plain
.\gradlew.bat :1.21.1-neoforge:build --console=plain
```

On Linux/macOS, use `./gradlew` with the same arguments. Artifacts are written to `versions/<target>/build/libs/`.

| Minecraft | Fabric target | NeoForge target |
| --- | --- | --- |
| 1.21.1 | `1.21.1-fabric` | `1.21.1-neoforge` |
| 1.21.11 | `1.21.11-fabric` | `1.21.11-neoforge` |
| 26.1 | `26.1-fabric` | `26.1-neoforge` |
| 26.2 | `26.2-fabric` | `26.2-neoforge` |
| 26.3 | `26.3-fabric` | Meh |

These are configured development targets; they do not all have the same release or runtime validation coverage. Fabric 26.x currently requires pre-existing Minecraft artifacts and metadata under `.gradle-user-home/caches/fabric-loom/`; its custom build path does not yet bootstrap an empty cache.

## Source layout

- `src/main/`: shared Java, resources, templates, and alias manifests.
- `src/generated/resources/`: resource inputs consumed by the build.
- `versions/<target>/`: target properties and source/resource overrides.
- `stonecutter-shared-files.properties`: routing rules for shared inputs.

Edit authored inputs rather than generated build output. Use target-qualified Gradle tasks such as `:<target>:build`, `:<target>:runClient`, and `:<target>:runServer`. `buildMaintainedMatrix` builds all nine targets when full build coverage is needed.

See [changelog.md](changelog.md) for changes. Current mod license metadata: **All Rights Reserved**.
