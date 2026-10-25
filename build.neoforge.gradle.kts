import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.zip.ZipFile

plugins {
    id("java-library")
    id("idea")
    id("net.neoforged.moddev") version "2.0.134"
}

val modId = property("mod.id") as String
val modName = property("mod.name") as String
val modVersion = buildString {
    append(property("mod.version") as String)
    append(findProperty("mod.channel_tag") as String? ?: "")
}
val modAuthors = property("mod.authors") as String
val modDescription = property("mod.description") as String
val modLicense = property("mod.license") as String
val mcVersion = property("deps.minecraft") as String
val minecraftVersionRange = (findProperty("deps.minecraft-range") as String?) ?: "[$mcVersion,1.22)"
val neoforgeVersion = property("deps.neoforge") as String
val neoforgeVersionRange = (findProperty("deps.neoforge-range") as String?) ?: "[21,)"
val loaderVersionRange = (findProperty("deps.loader-range") as String?) ?: "[4,)"
val parchmentVersion = findProperty("deps.parchment") as String?
val jeiVersion = findProperty("deps.jei") as String?
val emiVersion = findProperty("deps.emi") as String?
val jadeVersion = findProperty("deps.jade-neoforge") as String?
val yaclVersion = (findProperty("deps.yacl") as String?)
    ?.trim()
    ?.takeIf { it.isNotEmpty() && it != "[VERSIONED]" }
val javaVersion = ((findProperty("deps.java") as String?) ?: "21").toInt()
val quickPlayWorldId = ((findProperty("quickPlayWorldId") as String?) ?: "New World").trim().ifEmpty { "New World" }
val bnbDevFixtureRunner = (findProperty("bnbDevFixtureRunner") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevFixtureExit = ((findProperty("bnbDevFixtureExit") as String?) ?: "true").trim().ifEmpty { "true" }
val bnbDevFixtureAllowMutation = (findProperty("bnbDevFixtureAllowMutation") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevFixtureScenario = (findProperty("bnbDevFixtureScenario") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevFixturePhase = (findProperty("bnbDevFixturePhase") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevFixtureBasePos = (findProperty("bnbDevFixtureBasePos") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevExternalMods = (findProperty("bnbDevExternalMods") as String?)
    ?.split(';')
    ?.map { it.trim().trim('"') }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

fun yaclDependencyNotation(version: String): String =
    if (version.count { it == ':' } >= 2) version else "dev.isxander:yet-another-config-lib:$version"

group = property("mod.group") as String
version = modVersion

val legacyArchiveBaseName = "$modId-neoforge"
val priorVersionedArchiveBaseName = "$modId-$mcVersion-neoforge"

base {
    archivesName.set("$modId-neoforge-$mcVersion")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    doFirst {
        delete(layout.buildDirectory.file("libs/$legacyArchiveBaseName-$modVersion.jar"))
        delete(layout.buildDirectory.file("libs/$legacyArchiveBaseName-$modVersion-sources.jar"))
        delete(layout.buildDirectory.file("libs/$priorVersionedArchiveBaseName-$modVersion.jar"))
        delete(layout.buildDirectory.file("libs/$priorVersionedArchiveBaseName-$modVersion-sources.jar"))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.blamejared.com")
    maven("https://api.modrinth.com/maven")
}

val targetSourceRoot = rootProject.file("versions/${project.name}/src/main")
val mergedSourceRoot = layout.buildDirectory.dir("generated/stonecutter/${project.name}/main")
val mergedJavaDir = mergedSourceRoot.map { it.dir("java") }
val mergedResourcesDir = mergedSourceRoot.map { it.dir("resources") }

fun versionedFilePaths(root: java.io.File): Set<String> {
    if (!root.isDirectory) return emptySet()
    return root.walkTopDown()
        .filter { it.isFile }
        .map { it.relativeTo(root).invariantSeparatorsPath }
        .toSet()
}

val stonecutterSharedFileTargets: Map<String, Set<String>> =
    rootProject.file("stonecutter-shared-files.properties")
        .takeIf { it.isFile }
        ?.readLines()
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() && !it.startsWith("#") }
        ?.associate { line ->
            val path = line.substringBefore("=").trim()
            val targets = line.substringAfter("=", "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            path to targets
        }
        ?: emptyMap()

fun isStonecutterSharedFileAllowed(rootRelativePath: String): Boolean {
    val allowedTargets = stonecutterSharedFileTargets[rootRelativePath.replace('\\', '/')] ?: return true
    return project.name in allowedTargets
}

fun evaluateStonecutterDirectiveCondition(expression: String): Boolean {
    fun evaluateAtom(atom: String): Boolean {
        val expr = atom.trim()
        val targetMinecraftLine = project.name.substringBeforeLast("-")
        val targetLoader = project.name.substringAfterLast("-")

        if (expr == "fabric") return targetLoader == "fabric"
        if (expr == "neoforge") return targetLoader == "neoforge"

        Regex("""mc\s*==\s*([A-Za-z0-9_.-]+)""").matchEntire(expr)?.let {
            return targetMinecraftLine == it.groupValues[1]
        }
        Regex("""mc\s*!=\s*([A-Za-z0-9_.-]+)""").matchEntire(expr)?.let {
            return targetMinecraftLine != it.groupValues[1]
        }
        Regex("""loader\s*==\s*(fabric|neoforge)""").matchEntire(expr)?.let {
            return targetLoader == it.groupValues[1]
        }
        Regex("""loader\s*!=\s*(fabric|neoforge)""").matchEntire(expr)?.let {
            return targetLoader != it.groupValues[1]
        }
        Regex("""project\s*==\s*([A-Za-z0-9_.-]+)""").matchEntire(expr)?.let {
            return project.name == it.groupValues[1]
        }
        Regex("""project\s*!=\s*([A-Za-z0-9_.-]+)""").matchEntire(expr)?.let {
            return project.name != it.groupValues[1]
        }

        throw GradleException("Unknown Stonecutter directive condition '$expression' in ${project.path}")
    }

    return expression.split("||").any { orPart ->
        orPart.split("&&").all { evaluateAtom(it) }
    }
}

fun preprocessStonecutterDirectives(source: String): String {
    val activeStack = mutableListOf<Boolean>()
    val matchedStack = mutableListOf<Boolean>()
    val output = StringBuilder()

    fun currentActive(): Boolean = activeStack.all { it }
    fun parentActive(): Boolean = activeStack.dropLast(1).all { it }

    source.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("//?")) {
            val directive = trimmed.removePrefix("//?").trim()
            when {
                directive.startsWith("if ") -> {
                    val condition = evaluateStonecutterDirectiveCondition(directive.removePrefix("if ").trim())
                    activeStack.add(currentActive() && condition)
                    matchedStack.add(condition)
                }
                directive == "else" -> {
                    if (activeStack.isEmpty() || matchedStack.isEmpty()) {
                        throw GradleException("Stonecutter directive 'else' without matching if in ${project.path}")
                    }
                    val matched = matchedStack.removeLast()
                    activeStack.removeLast()
                    activeStack.add(parentActive() && !matched)
                    matchedStack.add(true)
                }
                directive == "endif" -> {
                    if (activeStack.isEmpty() || matchedStack.isEmpty()) {
                        throw GradleException("Stonecutter directive 'endif' without matching if in ${project.path}")
                    }
                    activeStack.removeLast()
                    matchedStack.removeLast()
                }
                currentActive() -> output.appendLine(line)
            }
        } else if (currentActive()) {
            output.appendLine(line)
        }
    }

    if (activeStack.isNotEmpty() || matchedStack.isNotEmpty()) {
        throw GradleException("Unclosed Stonecutter directive block in ${project.path}")
    }

    return output.toString()
}

fun rewriteMergedJavaFor26_2(source: String): String {
    var rewritten = source
        .replace("EntityType.HORSE", "net.minecraft.world.entity.EntityTypes.HORSE")
        .replace("EntityType.CAVE_SPIDER", "net.minecraft.world.entity.EntityTypes.CAVE_SPIDER")
        .replace("EntityType.CREEPER", "net.minecraft.world.entity.EntityTypes.CREEPER")
        .replace("EntityType.PLAYER", "net.minecraft.world.entity.EntityTypes.PLAYER")
        .replace("EntityType.BLOCK_DISPLAY", "net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY")
        .replace("BlockEntityType.SIGN", "net.minecraft.world.level.block.entity.BlockEntityTypes.SIGN")
        .replace("BlockEntityType.HANGING_SIGN", "net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN")
        .replace("BlockEntityType.SHELF", "net.minecraft.world.level.block.entity.BlockEntityTypes.SHELF")
        .replace("BlockEntityType.BELL", "net.minecraft.world.level.block.entity.BlockEntityTypes.BELL")
        .replace("Blocks.WHITE_WOOL", "Blocks.WOOL.white()")
        .replace("Blocks.EXPOSED_CUT_COPPER_SLAB.defaultBlockState()", "Blocks.CUT_COPPER_SLAB.weathering().exposed().defaultBlockState()")
        .replace("Blocks.CUT_COPPER_SLAB.defaultBlockState()", "Blocks.CUT_COPPER_SLAB.weathering().unaffected().defaultBlockState()")
        .replace("Blocks.EXPOSED_COPPER_DOOR", "Blocks.COPPER_DOOR.weathering().exposed()")
        .replace("Blocks.WEATHERED_COPPER_DOOR", "Blocks.COPPER_DOOR.weathering().weathered()")
        .replace("Blocks.OXIDIZED_COPPER_DOOR", "Blocks.COPPER_DOOR.weathering().oxidized()")
        .replace("isInstantenous()", "isInstantaneous()")
        .replace("getMainCamera()", "mainCamera()")
        .replace("getToastManager()", "gui.toastManager()")
        .replace("dispatcher.tryExtractRenderState(blockEntity, 0.0F, null)", "dispatcher.tryExtractRenderState(blockEntity, 0.0F, null, false)")
        .replace("net.minecraft.world.entity.net.minecraft.world.entity.EntityTypes.", "net.minecraft.world.entity.EntityTypes.")

    rewritten = Regex("""\bBlocks\.COPPER_DOOR\b(?!\.weathering\(\))""")
        .replace(rewritten, "Blocks.COPPER_DOOR.weathering().unaffected()")
    rewritten = rewritten
        .replace("Minecraft.getInstance().screen", "Minecraft.getInstance().gui.screen()")
        .replace("minecraft.screen", "minecraft.gui.screen()")
        .replace("client.screen", "client.gui.screen()")
        .replace("mc.screen", "mc.gui.screen()")
    rewritten = Regex("""\b(Minecraft\.getInstance\(\)|[A-Za-z_][A-Za-z0-9_]*)\.setScreen\(""")
        .replace(rewritten, "${'$'}1.gui.setScreen(")
    rewritten = rewritten.replace(".levelRenderer.setBlockDirty(", ".levelExtractor.setBlockDirty(")

    return rewritten
}

fun rewriteMergedJavaForTarget(mergedJavaRoot: java.io.File) {
    if (!mergedJavaRoot.isDirectory) return

    val registryHolderLookupPattern = Regex(
        """BuiltInRegistries\.(BLOCK|ITEM)\.get\(([^)\r\n]+)\)\s*\.map\(\s*(?:holder|h)\s*->\s*(?:holder|h)\.value\(\)\s*\)\s*\.orElse\(([^)\r\n]+)\)"""
    )
    val blockOrItemSetIdPattern = Regex(
        """(?m)^\s*\.setId\(ResourceKey\.create\(Registries\.(?:BLOCK|ITEM), [^)]+\)\)\r?\n"""
    )

    mergedJavaRoot.walkTopDown()
        .filter { it.isFile && it.extension == "java" }
        .forEach { file ->
            val original = file.readText(Charsets.UTF_8)
            var rewritten = preprocessStonecutterDirectives(original)
            if (mcVersion == "26.2") {
                rewritten = rewriteMergedJavaFor26_2(rewritten)
            }
            if (mcVersion != "1.21.1") {
                if (rewritten != original) {
                    file.writeText(rewritten, Charsets.UTF_8)
                }
                return@forEach
            }

            rewritten = rewritten
                .replace("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
                .replace("net.minecraft.world.entity.animal.equine.AbstractHorse", "net.minecraft.world.entity.animal.horse.AbstractHorse")
                .replace(Regex("""new Item\.Properties\(\)\.setId\(ResourceKey\.create\(Registries\.ITEM, [^)]+\)\)"""), "new Item.Properties()")
                .replace("BuiltInRegistries.ITEM.getValue(", "BuiltInRegistries.ITEM.get(")
                .replace("BuiltInRegistries.BLOCK.getValue(", "BuiltInRegistries.BLOCK.get(")
                .replace("horse.getOwnerReference()", "horse.getOwnerUUID()")
                .replace("ownerRef.matches(player)", "ownerRef.equals(player.getUUID())")
            rewritten = registryHolderLookupPattern.replace(rewritten) { match ->
                val registry = match.groupValues[1]
                val idExpression = match.groupValues[2]
                val fallbackExpression = match.groupValues[3]
                if (fallbackExpression == "null") {
                    "BuiltInRegistries.$registry.get($idExpression)"
                } else {
                    "java.util.Objects.requireNonNullElse(BuiltInRegistries.$registry.get($idExpression), $fallbackExpression)"
                }
            }
            rewritten = blockOrItemSetIdPattern.replace(rewritten, "")
            rewritten = Regex("""\bIdentifier\b""").replace(rewritten, "ResourceLocation")
            if (rewritten != original) {
                file.writeText(rewritten, Charsets.UTF_8)
            }
        }
}

fun sourceSlabPathForDynamicModel(modelPath: String): String? {
    if (modelPath == "step" || modelPath == "vertical_slab" || modelPath == "vertical_step") return null
    if (modelPath == "quad_step" || modelPath == "quad_vertical_step" || modelPath == "mixed_slab") return null
    if (modelPath.endsWith("_crack_proxy")) return null

    fun stepPathToSlabPath(path: String): String {
        if (Regex(""".*_step_\d+$""").matches(path)) {
            return path.replace(Regex("""_step_(\d+)$"""), "_slab_$1")
        }

        return if (path.endsWith("_step")) {
            path.removeSuffix("_step") + "_slab"
        } else {
            "${path}_slab"
        }
    }

    return when {
        modelPath.startsWith("vertical_") && modelPath.endsWith("_slab") -> modelPath.removePrefix("vertical_")
        modelPath.startsWith("vertical_") && modelPath.endsWith("_step") -> stepPathToSlabPath(modelPath.removePrefix("vertical_"))
        modelPath.endsWith("_step") -> stepPathToSlabPath(modelPath)
        else -> null
    }
}

fun parentModelForSourceSlab(sourceSlabPath: String, modId: String, localBlockModelsDir: java.io.File): String {
    if (localBlockModelsDir.resolve("$sourceSlabPath.json").isFile) {
        return "$modId:block/$sourceSlabPath"
    }

    if (sourceSlabPath.startsWith("waxed_") && sourceSlabPath.endsWith("_cut_copper_slab")) {
        return "minecraft:block/${sourceSlabPath.removePrefix("waxed_")}"
    }

    return "minecraft:block/$sourceSlabPath"
}

fun genericBlockstateForModel(modelPath: String): String =
    """
    |{
    |  "multipart": [
    |    {
    |      "apply": {
    |        "model": "bitsandbalance:block/$modelPath"
    |      }
    |    }
    |  ]
    |}
    """.trimMargin() + "\n"

fun writeTextIfMissing(targetFile: java.io.File, text: String) {
    if (targetFile.isFile) return
    targetFile.parentFile.mkdirs()
    targetFile.writeText(text)
}

fun stepPathForSlabPath(slabPath: String): String {
    val path = slabPath.replace('/', '_')
    return when {
        path.matches(Regex(""".*_slab_\d+$""")) -> path.replace(Regex("""_slab_(\d+)$"""), "_step_$1")
        path.endsWith("_slab") -> path.removeSuffix("_slab") + "_step"
        else -> "${path}_step"
    }
}

fun generateMinecraftSlabAliases(resourcesRoot: java.io.File, modId: String, minecraftClientJar: java.io.File) {
    if (!minecraftClientJar.isFile) return

    val blockstatesDir = resourcesRoot.resolve("assets/$modId/blockstates")
    val blockstateEntryPattern = Regex("""^assets/minecraft/blockstates/(.+_slab(?:_\d+)?)\.json$""")
    ZipFile(minecraftClientJar).use { zip ->
        zip.entries().asSequence()
            .mapNotNull { entry -> blockstateEntryPattern.matchEntire(entry.name) }
            .forEach { match ->
                val sourceSlabPath = match.groupValues[1]
                val sourceSlabRegistryPath = sourceSlabPath.replace('/', '_')
                val sourceStepRegistryPath = stepPathForSlabPath(sourceSlabRegistryPath)
                mapOf(
                    sourceStepRegistryPath to "step",
                    "vertical_$sourceSlabRegistryPath" to "vertical_slab",
                    "vertical_$sourceStepRegistryPath" to "vertical_step"
                ).forEach { (generatedPath, modelPath) ->
                    writeTextIfMissing(
                        blockstatesDir.resolve("$generatedPath.json"),
                        genericBlockstateForModel(modelPath)
                    )
                }
            }
    }
}

fun writeGeneratedItemDefinitions(resourcesRoot: java.io.File, modId: String) {
    val blockstatesDir = resourcesRoot.resolve("assets/$modId/blockstates")
    val itemModelsDir = resourcesRoot.resolve("assets/$modId/models/item")
    val itemDefinitionsDir = resourcesRoot.resolve("assets/$modId/items")
    if (!blockstatesDir.isDirectory) return

    blockstatesDir.walkTopDown()
        .filter { it.isFile && it.extension == "json" }
        .map { it.relativeTo(blockstatesDir).invariantSeparatorsPath.removeSuffix(".json") }
        .filter { sourceSlabPathForDynamicModel(it) != null }
        .filter { itemModelsDir.resolve("$it.json").isFile }
        .forEach { modelPath ->
            writeTextIfMissing(
                itemDefinitionsDir.resolve("$modelPath.json"),
                """
                |{
                |  "model": {
                |    "type": "minecraft:model",
                |    "model": "$modId:item/$modelPath"
                |  }
                |}
                """.trimMargin() + "\n"
            )
        }
}

fun genericItemParentForDynamicModel(modelPath: String): String? {
    return when {
        modelPath.startsWith("vertical_") && modelPath.endsWith("_slab") -> "bitsandbalance:item/vertical_slab"
        modelPath.startsWith("vertical_") && modelPath.endsWith("_step") -> "bitsandbalance:item/vertical_step"
        modelPath.endsWith("_step") -> "bitsandbalance:item/step"
        else -> null
    }
}

fun writeGeneratedItemModels(resourcesRoot: java.io.File, modId: String, useGenericDynamicParents: Boolean) {
    val blockstatesDir = resourcesRoot.resolve("assets/$modId/blockstates")
    val itemModelsDir = resourcesRoot.resolve("assets/$modId/models/item")
    val localBlockModelsDir = resourcesRoot.resolve("assets/$modId/models/block")
    if (!blockstatesDir.isDirectory) return

    blockstatesDir.walkTopDown()
        .filter { it.isFile && it.extension == "json" }
        .forEach { blockstateFile ->
            val modelPath = blockstateFile.relativeTo(blockstatesDir).invariantSeparatorsPath.removeSuffix(".json")
            val sourceSlabPath = sourceSlabPathForDynamicModel(modelPath) ?: return@forEach
            val localSourceModel = localBlockModelsDir.resolve("$sourceSlabPath.json")
            val itemParent = if (useGenericDynamicParents && !localSourceModel.isFile) {
                genericItemParentForDynamicModel(modelPath)
            } else {
                parentModelForSourceSlab(sourceSlabPath, modId, localBlockModelsDir)
            } ?: return@forEach
            writeTextIfMissing(
                itemModelsDir.resolve("$modelPath.json"),
                """
                |{
                |  "parent": "$itemParent"
                |}
                """.trimMargin() + "\n"
            )
        }
}

fun migrateGlobalLootModifierOrderFor26_2(resourcesRoot: java.io.File) {
    val globalFile = resourcesRoot.resolve("data/neoforge/loot_modifiers/global_loot_modifiers.json")
    if (!globalFile.isFile) return

    val entries = Regex(""""([a-z0-9_.-]+):([a-z0-9_./-]+)"""")
        .findAll(globalFile.readText())
        .map { match -> match.groupValues[1] to match.groupValues[2] }
        .toList()
    entries.forEachIndexed { index, (namespace, path) ->
        val modifierFile = resourcesRoot.resolve("data/$namespace/loot_modifiers/$path.json")
        if (!modifierFile.isFile) return@forEachIndexed
        val original = modifierFile.readText()
        if (Regex(""""priority"\s*:""").containsMatchIn(original)) return@forEachIndexed
        val priority = entries.size - index
        modifierFile.writeText(original.replaceFirst("{", "{\n  \"priority\": $priority,"))
    }

    globalFile.delete()
}

val mergeVersionedJava = tasks.register<Sync>("mergeVersionedJava") {
    val targetJavaDir = targetSourceRoot.resolve("java")
    val targetJavaPaths = versionedFilePaths(targetJavaDir)
    from(rootProject.file("src/main/java")) {
        exclude { it.path in targetJavaPaths }
        exclude { !isStonecutterSharedFileAllowed("java/${it.path.replace('\\', '/')}") }
        exclude {
            val javaPath = it.path.replace('\\', '/')
            javaPath.startsWith("org/onenonly/bitsandbalance/fabric/") &&
                project.name !in stonecutterSharedFileTargets["java/$javaPath"].orEmpty()
        }
    }
    from(targetJavaDir)
    into(mergedJavaDir)
    includeEmptyDirs = false
    doFirst {
        delete(mergedJavaDir.get().asFile)
    }
    doLast {
        rewriteMergedJavaForTarget(mergedJavaDir.get().asFile)
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val mergeVersionedResources = tasks.register<Sync>("mergeVersionedResources") {
    from(rootProject.file("src/main/resources")) {
        exclude { !isStonecutterSharedFileAllowed("resources/${it.path.replace('\\', '/')}") }
    }
    from(rootProject.file("src/generated/resources"))
    from(targetSourceRoot.resolve("resources"))
    into(mergedResourcesDir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    doLast {
        val resourcesRoot = mergedResourcesDir.get().asFile
        val blockstatesDir = resourcesRoot.resolve("assets/$modId/blockstates")

        if (mcVersion == "26.2") {
            migrateGlobalLootModifierOrderFor26_2(resourcesRoot)
        }

        if (!blockstatesDir.isDirectory) return@doLast

        writeGeneratedItemModels(resourcesRoot, modId, mcVersion == "26.2")

        if (mcVersion == "26.2") {
            writeGeneratedItemDefinitions(resourcesRoot, modId)
        }
    }
}

val generateNeo26_2DynamicClientResources = tasks.register("generateNeo26_2DynamicClientResources") {
    onlyIf { mcVersion == "26.2" }
    dependsOn(mergeVersionedResources, "createMinecraftArtifacts")
    doLast {
        val resourcesRoot = mergedResourcesDir.get().asFile
        val minecraftClientJar = rootProject.file(
            ".gradle-user-home/caches/neoformruntime/artifacts/minecraft_${mcVersion}_client.jar"
        )
        if (!minecraftClientJar.isFile) {
            throw GradleException("Missing NeoForm Minecraft client jar for $mcVersion at ${minecraftClientJar.path}")
        }
        generateMinecraftSlabAliases(resourcesRoot, modId, minecraftClientJar)
        writeGeneratedItemModels(resourcesRoot, modId, true)
        writeGeneratedItemDefinitions(resourcesRoot, modId)
    }
}

val sourceSets = extensions.getByType<SourceSetContainer>()

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to mcVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoforgeVersion,
        "neo_version_range" to neoforgeVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription
    )

    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.file("src/main/templates"))
    from(targetSourceRoot.resolve("templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

neoForge {
    version = neoforgeVersion

    if (!parchmentVersion.isNullOrBlank()) {
        parchment {
            val parts = parchmentVersion.split(':', limit = 2)
            minecraftVersion = parts[0]
            mappingsVersion = parts[1]
        }
    }

    runs {
        register("client") {
            client()
            gameDirectory = project.file("run-client")
            ideName = "NeoForge Client (${stonecutter.current.version})"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            programArgument("--username=Dev")
        }
        register("clientQuickPlay") {
            client()
            gameDirectory = project.file("run-client")
            ideName = "NeoForge Client QuickPlay (${stonecutter.current.version})"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            if (bnbDevFixtureRunner != null) {
                systemProperty("bitsandbalance.fixtureRunner", bnbDevFixtureRunner)
                systemProperty("bitsandbalance.fixtureTarget", project.name)
                systemProperty("bitsandbalance.fixtureExit", bnbDevFixtureExit)
                bnbDevFixtureAllowMutation?.let { systemProperty("bitsandbalance.fixtureAllowMutation", it) }
                bnbDevFixtureScenario?.let { systemProperty("bitsandbalance.fixtureScenario", it) }
                bnbDevFixturePhase?.let { systemProperty("bitsandbalance.fixturePhase", it) }
                bnbDevFixtureBasePos?.let { systemProperty("bitsandbalance.fixtureBasePos", it) }
            }
            programArguments.addAll(
                listOf(
                    "--username=Dev",
                    "--quickPlaySingleplayer",
                    quickPlayWorldId
                )
            )
        }
        register("server") {
            server()
            gameDirectory = project.file("run-server")
            ideName = "NeoForge Server (${stonecutter.current.version})"
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("data") {
            type = "serverData"
            gameDirectory = project.file("run-data")
            ideName = "NeoForge Datagen (${stonecutter.current.version})"
            programArguments.addAll(
                listOf(
                    "--mod", modId,
                    "--all",
                    "--output", rootProject.file("src/generated/resources").absolutePath,
                    "--existing", rootProject.file("src/main/resources").absolutePath,
                    "--existing", targetSourceRoot.resolve("resources").absolutePath
                )
            )
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

sourceSets.named("main") {
    java.setSrcDirs(listOf(mergedJavaDir))
    resources.setSrcDirs(listOf(mergedResourcesDir))
    resources.srcDir(tasks.named("generateModMetadata"))
}

val sodiumNeoForgeCompat by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val sodiumJarjarApiDir = layout.buildDirectory.dir("sodium-jarjar-api")
val extractSodiumJarjarApi by tasks.registering(Sync::class) {
    onlyIf { mcVersion == "1.21.1" }
    val sodiumJar = sodiumNeoForgeCompat.elements.map { elements -> elements.single().asFile }
    from(sodiumJar.map { zipTree(it) }) {
        include("META-INF/jarjar/fabric-api-base-*.jar")
        include("META-INF/jarjar/fabric-block-view-api-v2-*.jar")
        include("META-INF/jarjar/fabric-renderer-api-v1-*.jar")
        include("META-INF/jarjar/fabric-rendering-data-attachment-v1-*.jar")
    }
    into(sodiumJarjarApiDir)
}

dependencies {
    if (mcVersion == "1.21.1") {
        add(sodiumNeoForgeCompat.name, "maven.modrinth:sodium:mc1.21.1-0.8.12-alpha.4-neoforge")
        compileOnly(files(
            sodiumJarjarApiDir.map { it.file("META-INF/jarjar/fabric-api-base-0.4.42+d1308ded19.jar") },
            sodiumJarjarApiDir.map { it.file("META-INF/jarjar/fabric-block-view-api-v2-1.0.10+9afaaf8c19.jar") },
            sodiumJarjarApiDir.map { it.file("META-INF/jarjar/fabric-renderer-api-v1-3.4.1+9125b6dc19.jar") },
            sodiumJarjarApiDir.map { it.file("META-INF/jarjar/fabric-rendering-data-attachment-v1-0.3.48+73761d2e19.jar") }
        ))
    }
    if (!jeiVersion.isNullOrBlank()) {
        compileOnly("mezz.jei:jei-$mcVersion-neoforge-api:$jeiVersion")
        if (mcVersion == "26.2") {
            runtimeOnly("mezz.jei:jei-$mcVersion-neoforge:$jeiVersion")
        }
    }
    if (!emiVersion.isNullOrBlank()) {
        compileOnly("dev.emi:emi-neoforge:$emiVersion:api")
    }
    if (!jadeVersion.isNullOrBlank()) {
        compileOnly("maven.modrinth:jade:$jadeVersion")
        if (mcVersion == "26.2") {
            runtimeOnly("maven.modrinth:jade:$jadeVersion")
        }
    }
    if (!yaclVersion.isNullOrBlank()) {
        val yaclDependency = yaclDependencyNotation(yaclVersion)
        compileOnly(yaclDependency)
        runtimeOnly(yaclDependency)
    }
    if (bnbDevExternalMods.isNotEmpty()) {
        runtimeOnly(files(bnbDevExternalMods.map { file(it) }))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    dependsOn(mergeVersionedJava)
    if (mcVersion == "1.21.1") {
        dependsOn(extractSodiumJarjarApi)
    }
}

tasks.named("sourcesJar") {
    dependsOn(mergeVersionedJava, mergeVersionedResources, generateNeo26_2DynamicClientResources)
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(mergeVersionedResources)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(mergeVersionedResources, generateNeo26_2DynamicClientResources)
    from(sourceSets["main"].resources) {
        include("data/**/tags/blocks/**")
        eachFile {
            path = path.replace("/tags/blocks/", "/tags/block/")
        }
    }
    from(sourceSets["main"].resources) {
        include("data/**/tags/items/**")
        eachFile {
            path = path.replace("/tags/items/", "/tags/item/")
        }
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    exclude(".cache/**", "**/*.backup")
}
