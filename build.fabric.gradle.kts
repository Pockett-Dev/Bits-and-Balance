import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipFile
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.language.jvm.tasks.ProcessResources
import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("java-library")
    id("fabric-loom") version "1.15-20260313.091358-5" apply false
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
val fabricMinecraftVersionRange = (findProperty("deps.fabric-minecraft-range") as String?) ?: mcVersion
val fabricLoaderVersion = property("deps.fabric-loader") as String
val fabricApiVersion = property("deps.fabric-api") as String
val jeiVersion = findProperty("deps.jei") as String?
val emiVersion = findProperty("deps.emi") as String?
val jeiMinecraftVersion = (findProperty("deps.jei-minecraft") as String?)?.takeUnless { it == "[VERSIONED]" } ?: mcVersion
val jadeVersion = findProperty("deps.jade-fabric") as String?
val modMenuVersion = findProperty("deps.modmenu") as String?
val yaclVersion = findProperty("deps.yacl") as String?
val javaVersion = ((findProperty("deps.java") as String?) ?: "21").toInt()
val quickPlayWorldId = ((findProperty("quickPlayWorldId") as String?) ?: "New World").trim().ifEmpty { "New World" }
val bnbDevFixtureRunner = (findProperty("bnbDevFixtureRunner") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevFixtureExit = ((findProperty("bnbDevFixtureExit") as String?) ?: "true").trim().ifEmpty { "true" }
val bnbDevFixtureAllowMutation = (findProperty("bnbDevFixtureAllowMutation") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevFixtureScenario = (findProperty("bnbDevFixtureScenario") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevFixturePhase = (findProperty("bnbDevFixturePhase") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val bnbDevFixtureBasePos = (findProperty("bnbDevFixtureBasePos") as String?)?.trim()?.takeIf { it.isNotEmpty() }
val useFabricLoom = ((findProperty("deps.fabric-loom-enabled") as String?)?.toBooleanStrictOrNull()) ?: mcVersion.startsWith("1.")

fun yaclDependencyNotation(version: String): String =
    if (version.count { it == ':' } >= 2) version else "dev.isxander:yet-another-config-lib:$version"

if (useFabricLoom) {
    apply(plugin = "fabric-loom")
}

val loomExtension = if (useFabricLoom) extensions.getByName("loom") as LoomGradleExtensionAPI else null

group = property("mod.group") as String
version = modVersion

val legacyArchiveBaseName = "$modId-fabric"
val priorVersionedArchiveBaseName = "$modId-$mcVersion-fabric"

base {
    archivesName.set("$modId-fabric-$mcVersion")
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

val configuredJavaLanguageVersion = JavaLanguageVersion.of(javaVersion)

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.blamejared.com")
    maven("https://api.modrinth.com/maven")
}

val targetSourceRoot = rootProject.file("versions/${project.name}/src/main")
val targetAccessWidenerFile = targetSourceRoot.resolve("resources/bitsandbalance.accesswidener")
val accessWidenerFile = if (targetAccessWidenerFile.isFile) {
    targetAccessWidenerFile
} else {
    rootProject.file("src/main/resources/bitsandbalance.accesswidener")
}
val externalSlabAliasManifest = rootProject.file("src/main/slab-aliases/external_slab_aliases.txt")
val useNamedMergedMinecraftJar = !mcVersion.startsWith("1.")
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
            if (mcVersion == "26.2" || mcVersion == "26.3") {
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

fun stepPathForSlabPath(slabPath: String): String {
    val path = slabPath.replace('/', '_')
    if (path.matches(Regex(""".*_slab_\d+$"""))) {
        return path.replace(Regex("""_slab_(\d+)$"""), "_step_$1")
    }

    return if (path.endsWith("_slab")) {
        path.removeSuffix("_slab") + "_step"
    } else {
        "${path}_step"
    }
}

fun parentModelForSourceSlab(sourceNamespace: String, sourceSlabPath: String, modId: String, localBlockModelsDir: java.io.File): String {
    if (localBlockModelsDir.resolve("$sourceSlabPath.json").isFile) {
        return "$modId:block/$sourceSlabPath"
    }

    if (sourceNamespace == "minecraft" && sourceSlabPath.startsWith("waxed_") && sourceSlabPath.endsWith("_cut_copper_slab")) {
        return "minecraft:block/${sourceSlabPath.removePrefix("waxed_")}"
    }

    return "$sourceNamespace:block/$sourceSlabPath"
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

fun writeGeneratedSlabAliases(
    resourcesRoot: java.io.File,
    modId: String,
    sourceNamespace: String,
    sourceSlabPath: String,
    includeNamespaceAliases: Boolean,
    parentModelOverride: String? = null
) {
    val blockstatesDir = resourcesRoot.resolve("assets/$modId/blockstates")
    val itemModelsDir = resourcesRoot.resolve("assets/$modId/models/item")
    val itemDefinitionsDir = resourcesRoot.resolve("assets/$modId/items")
    val sourceSlabRegistryPath = sourceSlabPath.replace('/', '_')
    val sourceStepPath = stepPathForSlabPath(sourceSlabPath)
    val sourceStepRegistryPath = stepPathForSlabPath(sourceSlabRegistryPath)
    val parentModel = parentModelOverride
        ?: parentModelForSourceSlab(sourceNamespace, sourceSlabPath, modId, resourcesRoot.resolve("assets/$modId/models/block"))
    val generatedModels = linkedMapOf(
        sourceStepRegistryPath to "step",
        "vertical_$sourceSlabRegistryPath" to "vertical_slab",
        "vertical_$sourceStepRegistryPath" to "vertical_step"
    )

    if (includeNamespaceAliases) {
        generatedModels["${sourceNamespace}_${sourceStepRegistryPath}"] = "step"
        generatedModels["vertical_${sourceNamespace}_${sourceSlabRegistryPath}"] = "vertical_slab"
        generatedModels["vertical_${sourceNamespace}_${sourceStepRegistryPath}"] = "vertical_step"
    }

    generatedModels.forEach { (generatedPath, modelPath) ->
        val itemParent = if ((mcVersion == "26.2" || mcVersion == "26.3") && sourceNamespace != modId && sourceNamespace != "minecraft") {
            genericItemParentForDynamicModel(generatedPath) ?: parentModel
        } else {
            parentModel
        }
        writeTextIfMissing(
            blockstatesDir.resolve("$generatedPath.json"),
            genericBlockstateForModel(modelPath)
        )
        writeTextIfMissing(
            itemModelsDir.resolve("$generatedPath.json"),
            """
            |{
            |  "parent": "$itemParent"
            |}
            """.trimMargin() + "\n"
        )
        writeTextIfMissing(
            itemDefinitionsDir.resolve("$generatedPath.json"),
            """
            |{
            |  "model": {
            |    "type": "minecraft:model",
            |    "model": "$modId:item/$generatedPath"
            |  }
            |}
            """.trimMargin() + "\n"
        )
    }
}

fun generateLocalSlabAliases(resourcesRoot: java.io.File, modId: String) {
    val blockstatesDir = resourcesRoot.resolve("assets/$modId/blockstates")
    if (!blockstatesDir.isDirectory) return

    blockstatesDir.walkTopDown()
        .filter { it.isFile && it.extension == "json" }
        .map { it.relativeTo(blockstatesDir).invariantSeparatorsPath.removeSuffix(".json") }
        .filter { it.endsWith("_slab") || Regex(""".*_slab_\d+$""").matches(it) }
        .filterNot { it.startsWith("vertical_") }
        .filterNot { it == "mixed_slab" }
        .forEach { sourceSlabPath ->
            writeGeneratedSlabAliases(resourcesRoot, modId, modId, sourceSlabPath, false)
        }
}

fun generateExternalSlabAliases(resourcesRoot: java.io.File, modId: String, aliasManifest: java.io.File) {
    if (!aliasManifest.isFile) return

    val aliasPattern = Regex("""^([a-z0-9_.-]+):([a-z0-9_./-]+_slab(?:_\d+)?)(?:\s*=\s*([a-z0-9_.-]+:[a-z0-9_./-]+))?$""")
    aliasManifest.readLines()
        .asSequence()
        .mapIndexed { index, line -> index + 1 to line.removePrefix("\uFEFF").substringBefore('#').trim() }
        .filter { (_, line) -> line.isNotEmpty() }
        .forEach { (lineNumber, line) ->
            val match = aliasPattern.matchEntire(line)
                ?: throw GradleException("Invalid external slab alias '$line' at ${aliasManifest.path}:$lineNumber")
            val sourceNamespace = match.groupValues[1]
            val sourceSlabPath = match.groupValues[2]
            val parentModel = match.groupValues[3].ifBlank { null }
            if (sourceNamespace != modId && sourceNamespace != "minecraft") {
                writeGeneratedSlabAliases(resourcesRoot, modId, sourceNamespace, sourceSlabPath, true, parentModel)
            }
        }
}

fun verticalStepPathForSlabPath(slabPath: String): String {
    val path = slabPath.replace('/', '_')
    return when {
        path.matches(Regex(""".*_slab_\d+$""")) -> "vertical_" + path.replace(Regex("""_slab_(\d+)$"""), "_step_$1")
        path.endsWith("_slab") -> "vertical_" + path.removeSuffix("_slab") + "_step"
        else -> "vertical_${path}_step"
    }
}

fun generateMinecraftSlabAliases(resourcesRoot: java.io.File, modId: String, minecraftClientJar: java.io.File) {
    if (!minecraftClientJar.isFile) return

    val blockstateEntryPattern = Regex("""^assets/minecraft/blockstates/(.+_slab(?:_\d+)?)\.json$""")

    ZipFile(minecraftClientJar).use { zip ->
        zip.entries().asSequence()
            .mapNotNull { entry -> blockstateEntryPattern.matchEntire(entry.name) }
            .forEach { match ->
                val sourceSlabPath = match.groupValues[1]
                writeGeneratedSlabAliases(resourcesRoot, modId, "minecraft", sourceSlabPath, false)
            }
    }
}

val namedMergedMinecraftJar = providers.provider {
    val mergedJarDir = rootProject.file(".gradle-user-home/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged")
    val matches = fileTree(mergedJarDir) {
        include("**/*.jar")
    }.files
        .filter { it.name.matches(Regex("minecraft-merged-.*\\.jar")) }
        .sortedBy { it.absolutePath }

    require(matches.isNotEmpty()) {
        "Missing named merged Minecraft jar in the Fabric Loom cache."
    }

    matches.last()
}

val preferredNamedMergedMinecraftJar = providers.provider {
    if (useNamedMergedMinecraftJar) {
        val versionedMatches = fileTree(rootProject.file(".gradle-user-home/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged")) {
            include("$mcVersion-*/*.jar")
        }.files
            .filter { it.name.matches(Regex("minecraft-merged-.*\\.jar")) }
            .sortedBy { it.absolutePath }

        if (versionedMatches.isNotEmpty()) {
            return@provider versionedMatches.last()
        }

        val cachedMergedJar = rootProject.file(".gradle-user-home/caches/fabric-loom/$mcVersion/minecraft-merged.jar")
        if (cachedMergedJar.isFile) {
            return@provider cachedMergedJar
        }

        throw GradleException("Missing named merged Minecraft jar for $mcVersion in the Fabric Loom cache.")
    }

    namedMergedMinecraftJar.get()
}

val nonLoomVersionCacheDir = rootProject.file(".gradle-user-home/caches/fabric-loom/$mcVersion")
val nonLoomNeedsMappingJar = !mcVersion.startsWith("26.")
val nonLoomMappingsJar = providers.provider {
    val matches = fileTree(nonLoomVersionCacheDir) {
        include("net.fabricmc.yarn.*/mappings.jar")
    }.files.sortedBy { it.absolutePath }

    require(matches.isNotEmpty()) {
        "Missing cached Yarn mappings jar for $mcVersion in ${nonLoomVersionCacheDir.absolutePath}."
    }

    matches.last()
}

val nonLoomIntermediaryMergedMinecraftJar = providers.provider {
    val mergedJar = nonLoomVersionCacheDir.resolve("minecraft-merged.jar")
    require(mergedJar.isFile) {
        "Missing cached intermediary merged Minecraft jar for $mcVersion in ${nonLoomVersionCacheDir.absolutePath}."
    }
    mergedJar
}

val nonLoomOfficialMinecraftClientJar = providers.provider {
    val clientJar = nonLoomVersionCacheDir.resolve("minecraft-client.jar")
    require(clientJar.isFile) {
        "Missing cached official Minecraft client jar for $mcVersion in ${nonLoomVersionCacheDir.absolutePath}."
    }
    clientJar
}

val nonLoomOfficialMinecraftServerJar = providers.provider {
    val serverJar = nonLoomVersionCacheDir.resolve("minecraft-server.jar")
    require(serverJar.isFile) {
        "Missing cached official Minecraft server jar for $mcVersion in ${nonLoomVersionCacheDir.absolutePath}."
    }
    serverJar
}

fun isNonLoomParentOnlyRuntime(file: File): Boolean {
    val path = file.invariantSeparatorsPath
    return path.contains("/net.fabricmc/fabric-loader/")
            || path.contains("/net.fabricmc/sponge-mixin/")
            || path.contains("/net.fabricmc/dev-launch-injector/")
            || path.contains("/io.github.llamalad7/mixinextras-fabric/")
            || path.contains("/org.ow2.asm/")
}

fun isNonLoomFabricApiRuntime(file: File): Boolean =
    file.invariantSeparatorsPath.contains("/net.fabricmc.fabric-api/")

val nonLoomRuntimeNamespace = "official"

fun isNonLoomLoaderRuntime(file: File): Boolean {
    val path = file.invariantSeparatorsPath
    return path.contains("/net.fabricmc/fabric-loader/")
            || path.contains("/net.fabricmc/sponge-mixin/")
            || path.contains("/net.fabricmc/dev-launch-injector/")
}

fun isFabricModPath(file: File): Boolean {
    if (file.isDirectory) {
        return file.resolve("fabric.mod.json").isFile
    }
    if (!file.isFile || !file.extension.equals("jar", ignoreCase = true)) {
        return false
    }
    return try {
        ZipFile(file).use { zip -> zip.getEntry("fabric.mod.json") != null }
    } catch (_: Exception) {
        false
    }
}

fun writeNonLoomPathingJar(pathingJar: File, classpathEntries: Collection<File>) {
    pathingJar.parentFile.mkdirs()
    val manifest = Manifest()
    manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
    manifest.mainAttributes[Attributes.Name.CLASS_PATH] = classpathEntries
        .filter { it.exists() }
        .distinctBy { it.absolutePath }
        .joinToString(" ") { it.toURI().toURL().toExternalForm() }
    JarOutputStream(pathingJar.outputStream(), manifest).use { }
}

val nonLoomAssetIndexName = providers.provider {
    val matches = rootProject.file(".gradle-user-home/caches/fabric-loom/assets/indexes")
        .listFiles()
        ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) && it.name.startsWith("$mcVersion-") }
        ?.sortedBy { it.name }
        .orEmpty()

    require(matches.isNotEmpty()) {
        "Missing cached asset index for $mcVersion in .gradle-user-home/caches/fabric-loom/assets/indexes."
    }

    matches.last().nameWithoutExtension
}

val currentOsName = System.getProperty("os.name").lowercase()

fun ruleMatchesCurrentOs(ruleOs: Map<*, *>?): Boolean {
    val osName = ruleOs?.get("name") as? String ?: return true
    return when (osName) {
        "windows" -> currentOsName.contains("windows")
        "osx" -> currentOsName.contains("mac")
        "linux" -> currentOsName.contains("linux")
        else -> false
    }
}

fun isLibraryAllowedOnCurrentOs(library: Map<*, *>): Boolean {
    val rules = (library["rules"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: return true
    var allowed = false

    rules.forEach { rule ->
        val action = rule["action"] as? String ?: return@forEach
        if (ruleMatchesCurrentOs(rule["os"] as? Map<*, *>)) {
            allowed = action == "allow"
        }
    }

    return allowed
}

val nonLoomRuntimeMinecraftLibraries = providers.provider {
    val minecraftInfoFile = nonLoomVersionCacheDir.resolve("mojang_minecraft_info.json")
    if (useFabricLoom || !minecraftInfoFile.isFile) {
        emptyList<String>()
    } else {
        val parsed = JsonSlurper().parse(minecraftInfoFile) as Map<*, *>
        val libraries = parsed["libraries"] as? List<*> ?: emptyList<Any>()
        libraries
            .filterIsInstance<Map<*, *>>()
            .filter(::isLibraryAllowedOnCurrentOs)
            .mapNotNull { it["name"] as? String }
            .distinct()
    }
}

val nonLoomClientRuntime = configurations.create("nonLoomClientRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val nonLoomLaunchSupport = configurations.create("nonLoomLaunchSupport") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val nonLoomLog4jConfig = """
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
	<Appenders>

		<Console name="SysOut" target="SYSTEM_OUT">
			<Filters>
				<RegexFilter regex="^Failed to verify authentication$" onMatch="DENY" onMismatch="NEUTRAL"/>
				<RegexFilter regex="^Failed to fetch user properties$" onMatch="DENY" onMismatch="NEUTRAL"/>
				<RegexFilter regex="^Couldn't connect to realms$" onMatch="DENY" onMismatch="NEUTRAL"/>
				<RegexFilter regex="^Failed to fetch Realms feature flags$" onMatch="DENY" onMismatch="NEUTRAL"/>
			</Filters>
            <PatternLayout pattern="%style{[%d{HH:mm:ss}]}{blue} %highlight{[%t/%level]}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=green, TRACE=blue} %style{(%logger{1})}{cyan} %highlight{%msg%n}{FATAL=red, ERROR=red, WARN=normal, INFO=normal, DEBUG=normal, TRACE=normal}" disableAnsi="${'$'}{sys:fabric.log.disableAnsi:-true}" />
		</Console>

		<Queue name="ServerGuiConsole" ignoreExceptions="true">
            <PatternLayout pattern="[%d{HH:mm:ss} %level] (%logger{1}) %msg{nolookups}%n" />
		</Queue>

		<RollingRandomAccessFile name="LatestFile" fileName="logs/latest.log" filePattern="logs/%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level] (%logger{1}) %msg{nolookups}%n" />
			<Policies>
				<TimeBasedTriggeringPolicy />
				<OnStartupTriggeringPolicy />
			</Policies>
		</RollingRandomAccessFile>

		<RollingRandomAccessFile name="DebugFile" fileName="logs/debug.log" filePattern="logs/debug-%i.log.gz">
			<PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level] (%logger) %msg{nolookups}%n" />
			<DefaultRolloverStrategy max="5" fileIndex="min"/>
			<Policies>
				<SizeBasedTriggeringPolicy size="200MB"/>
				<OnStartupTriggeringPolicy />
			</Policies>
		</RollingRandomAccessFile>
	</Appenders>
	<Loggers>
		<Logger level="${'$'}{sys:fabric.log.level:-info}" name="net.minecraft"/>
		<Root level="${'$'}{sys:fabric.log.debug.level:-debug}">
			<AppenderRef ref="DebugFile" level="${'$'}{sys:fabric.log.debug.level:-debug}"/>
			<AppenderRef ref="SysOut" level="${'$'}{sys:fabric.log.level:-info}"/>
			<AppenderRef ref="LatestFile" level="${'$'}{sys:fabric.log.level:-info}"/>
			<AppenderRef ref="ServerGuiConsole" level="${'$'}{sys:fabric.log.level:-info}"/>
		</Root>
	</Loggers>
</Configuration>
""".trimIndent() + "\n"

val nonLoomMinecraftLibraries = listOf(
    "com.google.code.gson:gson:2.13.2",
    "com.google.guava:failureaccess:1.0.3",
    "com.google.guava:guava:33.5.0-jre",
    "com.mojang:authlib:7.0.63",
    "com.mojang:brigadier:1.3.10",
    "com.mojang:datafixerupper:9.0.19",
    "com.mojang:logging:1.6.11",
    "it.unimi.dsi:fastutil:8.5.18",
    "org.apache.logging.log4j:log4j-api:2.25.2",
    "org.apache.logging.log4j:log4j-core:2.25.2",
    "org.joml:joml:1.10.8",
    "io.netty:netty-buffer:4.2.7.Final",
    "io.netty:netty-common:4.2.7.Final",
    "org.lwjgl:lwjgl:3.4.1",
    "org.lwjgl:lwjgl-glfw:3.4.1",
    "org.slf4j:slf4j-api:2.0.17",
    "io.github.llamalad7:mixinextras-fabric:0.5.4",
    "net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7",
    "org.jetbrains:annotations:26.0.2"
)

val mergeVersionedJava = tasks.register<Sync>("mergeVersionedJava") {
    val targetJavaDir = targetSourceRoot.resolve("java")
    val targetJavaPaths = versionedFilePaths(targetJavaDir)
    from(rootProject.file("src/main/java")) {
        exclude { it.path in targetJavaPaths }
        exclude { !isStonecutterSharedFileAllowed("java/${it.path.replace('\\', '/')}") }
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
    if (!useFabricLoom) {
        exclude("org/onenonly/bitsandbalance/fabric/client/FabricMixedSlabModelPlugin.java")
        exclude("org/onenonly/bitsandbalance/fabric/client/renderer/AzaleaBoatRenderer.java")
        exclude("org/onenonly/bitsandbalance/fabric/mixin/ItemStackRenderStateLayerRenderStateMixin.java")
        exclude("org/onenonly/bitsandbalance/fabric/mixin/SheetsMixin.java")
        exclude("org/onenonly/bitsandbalance/fabric/recipe/impl/**")
        if (project.name !in setOf("26.2-fabric", "26.3-fabric")) {
            exclude("org/onenonly/bitsandbalance/common/compat/jade/**")
            exclude("org/onenonly/bitsandbalance/fabric/compat/jade/**")
        }
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val mergeVersionedResources = tasks.register<Sync>("mergeVersionedResources") {
    inputs.file(externalSlabAliasManifest)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .optional()

    val targetResourcePaths = versionedFilePaths(targetSourceRoot.resolve("resources"))

    from(rootProject.file("src/main/resources")) {
        if (project.name == "26.3-fabric") {
            exclude { it.path in targetResourcePaths }
        }
        exclude { !isStonecutterSharedFileAllowed("resources/${it.path.replace('\\', '/')}") }
    }
    from(rootProject.file("src/generated/resources")) {
        if (project.name == "26.3-fabric") {
            exclude { it.path in targetResourcePaths }
        }
    }
    from(targetSourceRoot.resolve("resources"))
    into(mergedResourcesDir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    doLast {
        val resourcesRoot = mergedResourcesDir.get().asFile
        val blockstatesDir = resourcesRoot.resolve("assets/$modId/blockstates")
        val itemModelsDir = resourcesRoot.resolve("assets/$modId/models/item")
        val localBlockModelsDir = resourcesRoot.resolve("assets/$modId/models/block")

        if (!useFabricLoom) {
            val mixinConfigFile = resourcesRoot.resolve("bitsandbalance.fabric.mixins.json")
            if (mixinConfigFile.isFile) {
                val excludedNonLoomClientMixins = setOf(
                    "ItemStackRenderStateLayerRenderStateMixin",
                    "SheetsMixin"
                )
                val parsedMixinConfig = LinkedHashMap(JsonSlurper().parse(mixinConfigFile) as Map<*, *>)
                val clientMixins = (parsedMixinConfig["client"] as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
                parsedMixinConfig["client"] = clientMixins.filterNot(excludedNonLoomClientMixins::contains)
                mixinConfigFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(parsedMixinConfig)) + "\n")
            }
        }

        generateLocalSlabAliases(resourcesRoot, modId)
        generateExternalSlabAliases(resourcesRoot, modId, externalSlabAliasManifest)
        if (!useFabricLoom) {
            generateMinecraftSlabAliases(resourcesRoot, modId, nonLoomOfficialMinecraftClientJar.get())
        }

        if (!blockstatesDir.isDirectory) return@doLast

        blockstatesDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .forEach { blockstateFile ->
                val modelPath = blockstateFile.relativeTo(blockstatesDir).invariantSeparatorsPath.removeSuffix(".json")
                val sourceSlabPath = sourceSlabPathForDynamicModel(modelPath) ?: return@forEach
                val targetFile = itemModelsDir.resolve("$modelPath.json")

                if (targetFile.isFile) return@forEach

                val localSourceModel = localBlockModelsDir.resolve("$sourceSlabPath.json")
                val itemParent = if (
                    resourcesRoot.resolve("assets/$modId/items/$modelPath.json").isFile &&
                    !localSourceModel.isFile
                ) {
                    genericItemParentForDynamicModel(modelPath)
                } else {
                    parentModelForSourceSlab("minecraft", sourceSlabPath, modId, localBlockModelsDir)
                }

                if (itemParent == null) return@forEach

                targetFile.parentFile.mkdirs()
                targetFile.writeText(
                    """
                    |{
                    |  "parent": "$itemParent"
                    |}
                    """.trimMargin() + "\n"
                )
            }
    }
}

dependencies {
    if (useFabricLoom) {
        add("minecraft", "com.mojang:minecraft:$mcVersion")
        add("mappings", loomExtension!!.officialMojangMappings())

        add("modImplementation", "net.fabricmc:fabric-loader:$fabricLoaderVersion")
        add("modImplementation", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    } else {
        compileOnly(files(preferredNamedMergedMinecraftJar))
        nonLoomMinecraftLibraries.forEach { compileOnly(it) }
        compileOnly("net.fabricmc:fabric-loader:$fabricLoaderVersion")
        implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

        nonLoomRuntimeMinecraftLibraries.get().forEach { add(nonLoomClientRuntime.name, it) }
        add(nonLoomClientRuntime.name, "net.fabricmc:fabric-loader:$fabricLoaderVersion")
        add(nonLoomClientRuntime.name, "net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
        add(nonLoomClientRuntime.name, "io.github.llamalad7:mixinextras-fabric:0.5.4")
        add(nonLoomLaunchSupport.name, "net.fabricmc:dev-launch-injector:0.2.1+build.8")
    }

    if (!jeiVersion.isNullOrBlank()) {
        compileOnly("mezz.jei:jei-$jeiMinecraftVersion-common-api:$jeiVersion")
        if (useFabricLoom && project.name == "1.21.11-fabric") {
            add("modLocalRuntime", "mezz.jei:jei-$mcVersion-fabric:$jeiVersion")
        } else if (!useFabricLoom && project.name == "26.2-fabric") {
            add(nonLoomClientRuntime.name, "mezz.jei:jei-$mcVersion-fabric:$jeiVersion")
        }
    }
    if (!emiVersion.isNullOrBlank()) {
        if (useFabricLoom) {
            add("modCompileOnly", "dev.emi:emi-fabric:$emiVersion:api")
        } else {
            compileOnly("dev.emi:emi-fabric:$emiVersion:api")
        }
    }
    if (!jadeVersion.isNullOrBlank()) {
        if (useFabricLoom) {
            add("modCompileOnly", "maven.modrinth:jade:$jadeVersion")
        } else {
            compileOnly("maven.modrinth:jade:$jadeVersion")
            if (project.name in setOf("26.2-fabric", "26.3-fabric")) {
                add(nonLoomClientRuntime.name, "maven.modrinth:jade:$jadeVersion")
            }
        }
    }
    if (!modMenuVersion.isNullOrBlank()) {
        if (useFabricLoom) {
            add("modCompileOnly", "com.terraformersmc:modmenu:$modMenuVersion")
            add("modLocalRuntime", "com.terraformersmc:modmenu:$modMenuVersion")
        } else {
            compileOnly("com.terraformersmc:modmenu:$modMenuVersion")
            add(nonLoomClientRuntime.name, "com.terraformersmc:modmenu:$modMenuVersion")
        }
    }
    if (!yaclVersion.isNullOrBlank()) {
        val yaclDependency = yaclDependencyNotation(yaclVersion)
        if (useFabricLoom) {
            add("modCompileOnly", yaclDependency)
            add("modLocalRuntime", yaclDependency)
        } else {
            compileOnly(yaclDependency)
            add(nonLoomClientRuntime.name, yaclDependency)
        }
    }
    if (useFabricLoom && project.name == "1.21.1-fabric") {
        add("modLocalRuntime", "net.fabricmc:fabric-language-kotlin:1.13.7+kotlin.2.2.21")
        add("modLocalRuntime", "maven.modrinth:frozenlib:2.2.4-mc1.21.1")
        add("modLocalRuntime", "maven.modrinth:wilder-wild:4.2.1-mc1.21.1")
    }
}

if (useFabricLoom) {
    extensions.configure<LoomGradleExtensionAPI>("loom") {
        accessWidenerPath.set(accessWidenerFile)

        mixin {
            useLegacyMixinAp = true
            defaultRefmapName = "bitsandbalance-fabric-refmap.json"
        }

        runs {
            named("client") {
                client()
                ideConfigGenerated(true)
                runDir = "run-client-fabric"
                configName = "Fabric Client (${stonecutter.current.version})"
                programArgs("--username=Dev")
            }
            register("clientQuickPlay") {
                client()
                ideConfigGenerated(true)
                runDir = "run-client-fabric"
                configName = "Fabric Client QuickPlay (${stonecutter.current.version})"
                programArgs("--username=Dev", "--quickPlaySingleplayer", quickPlayWorldId)
                if (bnbDevFixtureRunner != null) {
                    vmArg("-Dbitsandbalance.fixtureRunner=$bnbDevFixtureRunner")
                    vmArg("-Dbitsandbalance.fixtureTarget=${project.name}")
                    vmArg("-Dbitsandbalance.fixtureExit=$bnbDevFixtureExit")
                    bnbDevFixtureAllowMutation?.let { vmArg("-Dbitsandbalance.fixtureAllowMutation=$it") }
                    bnbDevFixtureScenario?.let { vmArg("-Dbitsandbalance.fixtureScenario=$it") }
                    bnbDevFixturePhase?.let { vmArg("-Dbitsandbalance.fixturePhase=$it") }
                    bnbDevFixtureBasePos?.let { vmArg("-Dbitsandbalance.fixtureBasePos=$it") }
                }
            }
            named("server") {
                server()
                ideConfigGenerated(true)
                runDir = "run-server"
                configName = "Fabric Server (${stonecutter.current.version})"
            }
        }
    }
} else {
    val nonLoomLaunchDir = layout.buildDirectory.dir("nonloom-run")
    val nonLoomLaunchConfigFile = nonLoomLaunchDir.map { it.file("launch.cfg") }
    val nonLoomServerLaunchConfigFile = nonLoomLaunchDir.map { it.file("launch-server.cfg") }
    val nonLoomClientPathingJar = nonLoomLaunchDir.map { it.file("client-classpath.jar") }
    val nonLoomServerPathingJar = nonLoomLaunchDir.map { it.file("server-classpath.jar") }
    val nonLoomAddModsFile = nonLoomLaunchDir.map { it.file("addMods.txt") }
    val nonLoomExternalLaunchDir = providers.provider {
        File(System.getProperty("user.home"), ".bitsandbalance-nonloom/${project.name}")
    }
    val nonLoomLaunchMappingsJar = providers.provider {
        nonLoomExternalLaunchDir.get().resolve("mappings.jar")
    }

    val prepareNonLoomFabricRunClient = tasks.register("prepareNonLoomFabricRunClient") {
        dependsOn("classes")
        if (nonLoomNeedsMappingJar) {
            inputs.file(nonLoomMappingsJar)
        }
        inputs.file(nonLoomIntermediaryMergedMinecraftJar)
        inputs.files(sourceSets["main"].runtimeClasspath)
        inputs.files(nonLoomClientRuntime)
        inputs.property("assetIndexName", nonLoomAssetIndexName)
        outputs.dir(nonLoomLaunchDir)

        doLast {
            val launchDir = nonLoomLaunchDir.get().asFile
            val remapClasspathFile = launchDir.resolve("remapClasspath.txt")
            val launchMappingsFile = nonLoomLaunchMappingsJar.get()
            val log4jFile = launchDir.resolve("log4j.xml")
            val launchConfigFile = launchDir.resolve("launch.cfg")
            val devModDir = launchDir.resolve("bitsandbalance-dev")
            val modClasspathGroup = (sourceSets["main"].output.classesDirs.files + setOfNotNull(sourceSets["main"].output.resourcesDir))
                .filter { it.exists() }
                .distinctBy { it.absolutePath }
                .joinToString(File.pathSeparator) { it.absolutePath }
            val mappingClasspathEntries = if (nonLoomNeedsMappingJar) setOf(nonLoomMappingsJar.get()) else emptySet()
            val pathingClasspathEntries = (sourceSets["main"].runtimeClasspath.files + nonLoomClientRuntime.files + nonLoomLaunchSupport.files + mappingClasspathEntries + setOf(nonLoomOfficialMinecraftClientJar.get()))
                .filter { it.exists() }
                .distinctBy { it.absolutePath }
                .sortedBy { it.absolutePath }
            val externalAddModEntries = (sourceSets["main"].runtimeClasspath.files.filter { it.isFile } + nonLoomClientRuntime.files)
                .filter(::isFabricModPath)
                .filterNot(::isNonLoomLoaderRuntime)
                .distinctBy { it.absolutePath }
                .sortedBy { it.absolutePath }
            val remapEntries = (sourceSets["main"].runtimeClasspath.files + nonLoomClientRuntime.files.filterNot(::isNonLoomFabricApiRuntime) + setOf(nonLoomIntermediaryMergedMinecraftJar.get()))
                .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
                .filterNot(::isNonLoomParentOnlyRuntime)
                .distinctBy { it.absolutePath }
                .sortedBy { it.absolutePath }

            launchDir.mkdirs()
            launchMappingsFile.parentFile.mkdirs()
            devModDir.deleteRecursively()
            devModDir.mkdirs()
            sourceSets["main"].output.classesDirs.files
                .filter { it.exists() }
                .forEach { classesDir ->
                    copy {
                        from(classesDir)
                        into(devModDir)
                    }
                }
            sourceSets["main"].output.resourcesDir
                ?.takeIf { it.exists() }
                ?.let { resourcesDir ->
                    copy {
                        from(resourcesDir)
                        into(devModDir)
                    }
                }
            if (nonLoomNeedsMappingJar) {
                nonLoomMappingsJar.get().copyTo(launchMappingsFile, overwrite = true)
            }
            writeNonLoomPathingJar(launchDir.resolve("client-classpath.jar"), pathingClasspathEntries)
            val addModEntries = listOf(devModDir) + externalAddModEntries
            launchDir.resolve("addMods.txt").writeText(addModEntries.joinToString("\n") { it.absolutePath } + "\n")
            remapClasspathFile.writeText(remapEntries.joinToString(File.pathSeparator) { it.absolutePath })
            log4jFile.writeText(nonLoomLog4jConfig)
            launchConfigFile.writeText(buildString {
                appendLine("commonProperties")
                appendLine("\tfabric.development=true")
                appendLine("\tfabric.runtimeMappingNamespace=$nonLoomRuntimeNamespace")
                appendLine("\tlog4j.configurationFile=${log4jFile.absolutePath}")
                appendLine("\tlog4j2.formatMsgNoLookups=true")
                appendLine("\tfabric.defaultModDistributionNamespace=$nonLoomRuntimeNamespace")
                appendLine("\tfabric.defaultMixinRemapType=mixin")
                appendLine("\tfabric.remapClasspathFile=${remapClasspathFile.absolutePath}")
                if (modClasspathGroup.isNotEmpty()) {
                    appendLine("\tfabric.classPathGroups=$modClasspathGroup")
                }
                appendLine("clientArgs")
                appendLine("\t--assetIndex")
                appendLine("\t${nonLoomAssetIndexName.get()}")
                appendLine("\t--assetsDir")
                appendLine("\t${rootProject.file(".gradle-user-home/caches/fabric-loom/assets").absolutePath}")
            })
        }
    }

    val prepareNonLoomFabricRunServer = tasks.register("prepareNonLoomFabricRunServer") {
        dependsOn("classes")
        if (nonLoomNeedsMappingJar) {
            inputs.file(nonLoomMappingsJar)
        }
        inputs.file(nonLoomIntermediaryMergedMinecraftJar)
        inputs.files(sourceSets["main"].runtimeClasspath)
        inputs.files(nonLoomClientRuntime)
        outputs.dir(nonLoomLaunchDir)

        doLast {
            val launchDir = nonLoomLaunchDir.get().asFile
            val remapClasspathFile = launchDir.resolve("remapClasspath.txt")
            val launchMappingsFile = nonLoomLaunchMappingsJar.get()
            val log4jFile = launchDir.resolve("log4j.xml")
            val launchConfigFile = launchDir.resolve("launch-server.cfg")
            val devModDir = launchDir.resolve("bitsandbalance-dev")
            val modClasspathGroup = (sourceSets["main"].output.classesDirs.files + setOfNotNull(sourceSets["main"].output.resourcesDir))
                .filter { it.exists() }
                .distinctBy { it.absolutePath }
                .joinToString(File.pathSeparator) { it.absolutePath }
            val mappingClasspathEntries = if (nonLoomNeedsMappingJar) setOf(nonLoomMappingsJar.get()) else emptySet()
            val pathingClasspathEntries = (sourceSets["main"].runtimeClasspath.files + nonLoomClientRuntime.files + nonLoomLaunchSupport.files + mappingClasspathEntries + setOf(nonLoomOfficialMinecraftServerJar.get()))
                .filter { it.exists() }
                .distinctBy { it.absolutePath }
                .sortedBy { it.absolutePath }
            val externalAddModEntries = (sourceSets["main"].runtimeClasspath.files.filter { it.isFile } + nonLoomClientRuntime.files)
                .filter(::isFabricModPath)
                .filterNot(::isNonLoomLoaderRuntime)
                .distinctBy { it.absolutePath }
                .sortedBy { it.absolutePath }
            val remapEntries = (sourceSets["main"].runtimeClasspath.files + nonLoomClientRuntime.files.filterNot(::isNonLoomFabricApiRuntime) + setOf(nonLoomIntermediaryMergedMinecraftJar.get()))
                .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
                .filterNot(::isNonLoomParentOnlyRuntime)
                .distinctBy { it.absolutePath }
                .sortedBy { it.absolutePath }

            launchDir.mkdirs()
            launchMappingsFile.parentFile.mkdirs()
            devModDir.deleteRecursively()
            devModDir.mkdirs()
            sourceSets["main"].output.classesDirs.files
                .filter { it.exists() }
                .forEach { classesDir ->
                    copy {
                        from(classesDir)
                        into(devModDir)
                    }
                }
            sourceSets["main"].output.resourcesDir
                ?.takeIf { it.exists() }
                ?.let { resourcesDir ->
                    copy {
                        from(resourcesDir)
                        into(devModDir)
                    }
                }
            if (nonLoomNeedsMappingJar) {
                nonLoomMappingsJar.get().copyTo(launchMappingsFile, overwrite = true)
            }
            writeNonLoomPathingJar(launchDir.resolve("server-classpath.jar"), pathingClasspathEntries)
            val addModEntries = listOf(devModDir) + externalAddModEntries
            launchDir.resolve("addMods.txt").writeText(addModEntries.joinToString("\n") { it.absolutePath } + "\n")
            remapClasspathFile.writeText(remapEntries.joinToString(File.pathSeparator) { it.absolutePath })
            log4jFile.writeText(nonLoomLog4jConfig)
            launchConfigFile.writeText(buildString {
                appendLine("commonProperties")
                appendLine("\tfabric.development=true")
                appendLine("\tfabric.runtimeMappingNamespace=$nonLoomRuntimeNamespace")
                appendLine("\tlog4j.configurationFile=${log4jFile.absolutePath}")
                appendLine("\tlog4j2.formatMsgNoLookups=true")
                appendLine("\tfabric.defaultModDistributionNamespace=$nonLoomRuntimeNamespace")
                appendLine("\tfabric.defaultMixinRemapType=mixin")
                appendLine("\tfabric.remapClasspathFile=${remapClasspathFile.absolutePath}")
                if (modClasspathGroup.isNotEmpty()) {
                    appendLine("\tfabric.classPathGroups=$modClasspathGroup")
                }
            })
        }
    }

    fun JavaExec.configureNonLoomFabricRunTask(includeQuickPlay: Boolean) {
        dependsOn(prepareNonLoomFabricRunClient)
        group = "fabric"
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(configuredJavaLanguageVersion)
        })
        workingDir = file("run-client-fabric")
        mainClass.set("net.fabricmc.devlaunchinjector.Main")
        classpath = if (mcVersion.startsWith("26.")) {
            files({ nonLoomClientRuntime.files.filter(::isNonLoomParentOnlyRuntime) }) + nonLoomLaunchSupport
        } else {
            files({ sourceSets["main"].runtimeClasspath.files.filterNot(::isNonLoomParentOnlyRuntime) }) + files(nonLoomMappingsJar, nonLoomOfficialMinecraftClientJar) + files({ nonLoomClientRuntime.files.filterNot(::isNonLoomFabricApiRuntime) }) + nonLoomLaunchSupport
        }
        systemProperty("fabric.dli.config", nonLoomLaunchConfigFile.get().asFile.absolutePath)
        systemProperty("fabric.dli.env", "client")
        systemProperty("fabric.dli.main", "net.fabricmc.loader.impl.launch.knot.KnotClient")
        systemProperty("fabric.gameJarPath.client", nonLoomOfficialMinecraftClientJar.get().absolutePath)
        if (mcVersion.startsWith("26.")) {
            systemProperty("fabric.debug.disableModIds", "fabric-crash-report-info-v1")
            systemProperty("fabric.debug.throwDirectly", "true")
            systemProperty("fabric.loader.useCompatibilityClassLoader", "true")
            systemProperty("fabric.noGui", "true")
            systemProperty("fabric.debug.disableClassPathIsolation", "true")
            systemProperty("fabric.addMods", "@${nonLoomAddModsFile.get().asFile.absolutePath}")
            if (nonLoomNeedsMappingJar) {
                systemProperty("fabric.mappingPath", nonLoomLaunchMappingsJar.get().absolutePath)
            }
            systemProperty("fabric.gameLibraries", nonLoomClientRuntime.files
                .filter { it.isFile }
                .filterNot(::isNonLoomParentOnlyRuntime)
                .filterNot(::isFabricModPath)
                .distinctBy { it.absolutePath }
                .joinToString(File.pathSeparator) { it.absolutePath })
            systemProperty("java.awt.headless", "true")
            systemProperty("fabric.systemLibraries", (nonLoomClientRuntime.files.filter(::isNonLoomParentOnlyRuntime) + nonLoomLaunchSupport.files).distinctBy { it.absolutePath }.joinToString(File.pathSeparator) { it.absolutePath })
        }
        systemProperty("file.encoding", "UTF-8")
        systemProperty("user.country", "US")
        systemProperty("user.language", "en")
        systemProperty("user.variant", "")
        args("--username=Dev")

        if (includeQuickPlay) {
            args("--quickPlaySingleplayer", quickPlayWorldId)
            if (bnbDevFixtureRunner != null) {
                systemProperty("bitsandbalance.fixtureRunner", bnbDevFixtureRunner)
                systemProperty("bitsandbalance.fixtureTarget", project.name)
                systemProperty("bitsandbalance.fixtureExit", bnbDevFixtureExit)
                bnbDevFixtureAllowMutation?.let { systemProperty("bitsandbalance.fixtureAllowMutation", it) }
                bnbDevFixtureScenario?.let { systemProperty("bitsandbalance.fixtureScenario", it) }
                bnbDevFixturePhase?.let { systemProperty("bitsandbalance.fixturePhase", it) }
                bnbDevFixtureBasePos?.let { systemProperty("bitsandbalance.fixtureBasePos", it) }
            }
        }

        doFirst {
            workingDir.mkdirs()
        }
    }

    fun JavaExec.configureNonLoomFabricRunServerTask() {
        dependsOn(prepareNonLoomFabricRunServer)
        group = "fabric"
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(configuredJavaLanguageVersion)
        })
        workingDir = file("run-server")
        mainClass.set("net.fabricmc.devlaunchinjector.Main")
        classpath = if (mcVersion.startsWith("26.")) {
            files({ nonLoomClientRuntime.files.filter(::isNonLoomParentOnlyRuntime) }) + nonLoomLaunchSupport
        } else {
            files({ sourceSets["main"].runtimeClasspath.files.filterNot(::isNonLoomParentOnlyRuntime) }) + files(nonLoomMappingsJar, nonLoomOfficialMinecraftServerJar) + files({ nonLoomClientRuntime.files.filterNot(::isNonLoomFabricApiRuntime) }) + nonLoomLaunchSupport
        }
        standardInput = System.`in`
        systemProperty("fabric.dli.config", nonLoomServerLaunchConfigFile.get().asFile.absolutePath)
        systemProperty("fabric.dli.env", "server")
        systemProperty("fabric.dli.main", "net.fabricmc.loader.impl.launch.knot.KnotServer")
        systemProperty("fabric.gameJarPath.server", nonLoomOfficialMinecraftServerJar.get().absolutePath)
        if (mcVersion.startsWith("26.")) {
            systemProperty("fabric.debug.disableModIds", "fabric-crash-report-info-v1")
            systemProperty("fabric.debug.throwDirectly", "true")
            systemProperty("fabric.loader.useCompatibilityClassLoader", "true")
            systemProperty("fabric.noGui", "true")
            systemProperty("fabric.debug.disableClassPathIsolation", "true")
            systemProperty("fabric.addMods", "@${nonLoomAddModsFile.get().asFile.absolutePath}")
            if (nonLoomNeedsMappingJar) {
                systemProperty("fabric.mappingPath", nonLoomLaunchMappingsJar.get().absolutePath)
            }
            systemProperty("fabric.gameLibraries", nonLoomClientRuntime.files
                .filter { it.isFile }
                .filterNot(::isNonLoomParentOnlyRuntime)
                .filterNot(::isFabricModPath)
                .distinctBy { it.absolutePath }
                .joinToString(File.pathSeparator) { it.absolutePath })
            systemProperty("java.awt.headless", "true")
            systemProperty("fabric.systemLibraries", (nonLoomClientRuntime.files.filter(::isNonLoomParentOnlyRuntime) + nonLoomLaunchSupport.files).distinctBy { it.absolutePath }.joinToString(File.pathSeparator) { it.absolutePath })
        }
        systemProperty("file.encoding", "UTF-8")
        systemProperty("user.country", "US")
        systemProperty("user.language", "en")
        systemProperty("user.variant", "")
        args("nogui")

        doFirst {
            workingDir.mkdirs()
        }
    }

    tasks.register<JavaExec>("runClient") {
        description = "Runs the Fabric client without Loom using cached $mcVersion mappings and assets."
        configureNonLoomFabricRunTask(includeQuickPlay = false)
    }

    tasks.register<JavaExec>("runClientQuickPlay") {
        description = "Runs the Fabric client without Loom and quick-plays into a singleplayer world."
        configureNonLoomFabricRunTask(includeQuickPlay = true)
    }

    tasks.register<JavaExec>("runServer") {
        description = "Runs the Fabric dedicated server without Loom using cached $mcVersion mappings."
        configureNonLoomFabricRunServerTask()
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

val sourceSets = extensions.getByType<SourceSetContainer>()
sourceSets.named("main") {
    java.setSrcDirs(listOf(mergedJavaDir))
    resources.setSrcDirs(listOf(mergedResourcesDir))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    dependsOn(mergeVersionedJava)
}

tasks.named("sourcesJar") {
    dependsOn(mergeVersionedJava, mergeVersionedResources)
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
        "mod_license" to modLicense,
        "minecraft_version" to mcVersion,
        "fabric_minecraft_version_range" to fabricMinecraftVersionRange,
        "fabric_loader_version" to fabricLoaderVersion,
        "fabric_api_version" to fabricApiVersion
    )

    dependsOn(mergeVersionedResources)
    inputs.properties(replaceProperties)

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

    filesMatching("fabric.mod.json") {
        expand(replaceProperties)
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    exclude(".cache/**", "**/*.backup")
    if (mcVersion != "26.3") {
        exclude("data/bitsandbalance/recipe/brewing/**")
    }
    if (mcVersion == "26.3") {
        exclude("bitsandbalance.accesswidener")
        exclude(
            "data/bitsandbalance/advancement/recipe/azalea_chest.json",
            "data/bitsandbalance/advancement/recipe/azalea_log.json",
            "data/bitsandbalance/advancement/recipe/azalea_sticks.json"
        )
    }
}
