plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active file(".sc_active_version")

val maintainedTargets = listOf(
    "1.21.1-fabric",
    "1.21.1-neoforge",
    "1.21.11-fabric",
    "1.21.11-neoforge",
    "26.1-fabric",
    "26.1-neoforge",
    "26.2-fabric",
    "26.2-neoforge",
    "26.3-fabric",
)

// These 26.2 resources were deliberately replaced by native 26.3 APIs or schemas.
// Keep this exact-path allowlist narrow so accidental routing omissions still fail validation.
val fabric263ReplacementPaths = setOf(
    "java/org/onenonly/bitsandbalance/fabric/worldgen/FabricAzaleaTreeModifier.java",
    "java/org/onenonly/bitsandbalance/fabric/mixin/TreeConfigurationAccessor.java",
    "resources/data/bitsandbalance/worldgen/configured_feature/nether_gold_vein.json",
    "resources/data/bitsandbalance/worldgen/configured_feature/nether_quartz_vein.json",
    "java/org/onenonly/bitsandbalance/fabric/potions/FabricBrewingRecipeRegistrar.java",
    "java/org/onenonly/bitsandbalance/potions/BrewingRecipeRegistrar.java",
    "resources/data/bitsandbalance/recipe/brewing/displacement.json",
    "resources/data/bitsandbalance/recipe/brewing/haste.json",
    "resources/data/bitsandbalance/recipe/brewing/levitation.json",
    "resources/data/bitsandbalance/recipe/brewing/resurfacing.json",
    "resources/data/bitsandbalance/recipe/brewing/returning.json",
    "resources/data/bitsandbalance/recipe/brewing/withering.json",
    "java/org/onenonly/bitsandbalance/fabric/mixin/BrewingStandMenuMixin.java",
    "java/org/onenonly/bitsandbalance/fabric/mixin/PotionBrewingIngredientMixin.java",
)

tasks.register("verifyMaintainedTargets") {
    group = "verification"
    description = "Verifies the exact maintained target set and consecutive 26.x shared-file routing."

    doLast {
        val expectedTargets = maintainedTargets.toSet()
        val configuredTargets = project.subprojects.map { it.name }.toSet()
        if (configuredTargets != expectedTargets) {
            val missing = (expectedTargets - configuredTargets).sorted()
            val unexpected = (configuredTargets - expectedTargets).sorted()
            throw GradleException(
                "Maintained target mismatch. Missing: ${missing.ifEmpty { listOf("<none>") }}; " +
                    "unexpected: ${unexpected.ifEmpty { listOf("<none>") }}"
            )
        }

        val versionsDirectory = layout.projectDirectory.dir("versions").asFile
        val versionDirectories = versionsDirectory.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
        if (versionDirectories != expectedTargets) {
            val missing = (expectedTargets - versionDirectories).sorted()
            val unexpected = (versionDirectories - expectedTargets).sorted()
            throw GradleException(
                "Version directory mismatch. Missing: ${missing.ifEmpty { listOf("<none>") }}; " +
                    "unexpected: ${unexpected.ifEmpty { listOf("<none>") }}"
            )
        }

        maintainedTargets.forEach { target ->
            val propertiesFile = layout.projectDirectory.file("versions/$target/gradle.properties").asFile
            if (!propertiesFile.isFile) {
                throw GradleException(
                    "Missing maintained target properties: ${propertiesFile.relativeTo(project.projectDir)}"
                )
            }
        }

        val manifestFile = layout.projectDirectory.file("stonecutter-shared-files.properties").asFile
        if (!manifestFile.isFile) {
            throw GradleException("Missing shared-file manifest: ${manifestFile.relativeTo(project.projectDir)}")
        }

        val routingFailures = mutableListOf<String>()
        manifestFile.useLines { lines ->
            lines.forEachIndexed { index, rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) {
                    return@forEachIndexed
                }

                val separator = line.indexOf('=')
                if (separator <= 0 || separator == line.lastIndex) {
                    routingFailures += "line ${index + 1}: malformed entry"
                    return@forEachIndexed
                }

                val path = line.substring(0, separator).trim()
                val targets = line.substring(separator + 1)
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                val unknownTargets = targets - expectedTargets
                if (unknownTargets.isNotEmpty()) {
                    routingFailures += "$path: unknown targets ${unknownTargets.sorted()}"
                }
                if ("26.1-fabric" in targets && "26.2-fabric" !in targets) {
                    routingFailures += "$path: 26.1-fabric is routed without 26.2-fabric"
                }
                if ("26.1-neoforge" in targets && "26.2-neoforge" !in targets) {
                    routingFailures += "$path: 26.1-neoforge is routed without 26.2-neoforge"
                }
                if ("26.2-fabric" in targets
                    && "26.3-fabric" !in targets
                    && path !in fabric263ReplacementPaths
                ) {
                    routingFailures += "$path: 26.2-fabric is routed without 26.3-fabric"
                }
            }
        }

        if (routingFailures.isNotEmpty()) {
            throw GradleException(
                "Shared-file manifest maintenance invariant failed:\n" +
                    routingFailures.joinToString(separator = "\n") { " - $it" }
            )
        }

        logger.lifecycle("Verified ${maintainedTargets.size} maintained targets and consecutive 26.x shared-file routing.")
    }
}

tasks.register("buildMaintainedMatrix") {
    group = "build"
    description = "Builds all nine maintained Fabric and NeoForge targets."
    dependsOn("verifyMaintainedTargets")
    dependsOn(maintainedTargets.map { ":$it:build" })
}
