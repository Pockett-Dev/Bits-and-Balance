package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.serialization.Dynamic;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprint;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprints;
import org.onenonly.bitsandbalance.common.recipe.LegacyRecipeJsonCompat;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricRecipesConfig;
import org.onenonly.bitsandbalance.fabric.mixin.PackRepositoryAccessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

public final class FabricGeneratedWorldDataPacks {
    private static final String DATAPACKS_DIR = "datapacks";
    private static final String BOOTSTRAP_STATE_DIR = ".bitsandbalance-bootstrap";
    private static final String BOOTSTRAP_FILE_SUFFIX = ".sha256";
    private static final Path CACHE_ROOT = FabricLoader.getInstance().getGameDir().resolve(".runtime-packs").resolve("server-data");
    private static final Path LEGACY_CONFIG_CACHE_ROOT = FabricLoader.getInstance().getConfigDir().resolve("Bits and Balance").resolve("runtime-packs").resolve("server-data");
    private static final Path LEGACY_GAME_CACHE_ROOT = FabricLoader.getInstance().getGameDir().resolve("bitsandbalance-runtime-packs").resolve("server-data");
    private static final List<String> PACK_NAMES = List.of(
            FabricAlternativeRecipes.packName(),
            FabricWoodStonecuttingRecipes.packName(),
            FabricVerticalSlabRecipes.staticPackName(),
            FabricVerticalSlabRecipes.mirrorPackName(),
            FabricStepRecipes.staticPackName(),
            FabricStepRecipes.mirrorPackName()
    );
        private static final Map<String, String> PACK_MCMETAS = Map.of(
            FabricAlternativeRecipes.packName(), FabricAlternativeRecipes.packMcmeta(),
            FabricWoodStonecuttingRecipes.packName(), FabricWoodStonecuttingRecipes.packMcmeta(),
            FabricVerticalSlabRecipes.staticPackName(), FabricVerticalSlabRecipes.staticPackMcmeta(),
            FabricVerticalSlabRecipes.mirrorPackName(), FabricVerticalSlabRecipes.mirrorPackMcmeta(),
            FabricStepRecipes.staticPackName(), FabricStepRecipes.staticPackMcmeta(),
            FabricStepRecipes.mirrorPackName(), FabricStepRecipes.mirrorPackMcmeta()
        );
    private static final Map<String, RepositorySource> GENERATED_SOURCES = Map.of(
            FabricAlternativeRecipes.packName(), createSource(FabricAlternativeRecipes.packName(), getPackPath(FabricAlternativeRecipes.packName())),
            FabricWoodStonecuttingRecipes.packName(), createSource(FabricWoodStonecuttingRecipes.packName(), getPackPath(FabricWoodStonecuttingRecipes.packName())),
            FabricVerticalSlabRecipes.staticPackName(), createSource(FabricVerticalSlabRecipes.staticPackName(), getPackPath(FabricVerticalSlabRecipes.staticPackName())),
            FabricVerticalSlabRecipes.mirrorPackName(), createSource(FabricVerticalSlabRecipes.mirrorPackName(), getPackPath(FabricVerticalSlabRecipes.mirrorPackName())),
            FabricStepRecipes.staticPackName(), createSource(FabricStepRecipes.staticPackName(), getPackPath(FabricStepRecipes.staticPackName())),
            FabricStepRecipes.mirrorPackName(), createSource(FabricStepRecipes.mirrorPackName(), getPackPath(FabricStepRecipes.mirrorPackName()))
    );
    private static final Set<PackRepository> SERVER_DATA_REPOSITORIES = Collections.newSetFromMap(new WeakHashMap<>());

    private FabricGeneratedWorldDataPacks() {
    }

    public static void initializeDirectories() {
        try {
            migrateLegacyCacheRoot();
            Files.createDirectories(CACHE_ROOT);
            for (String packName : PACK_NAMES) {
                ensurePackRoot(packName);
            }
            normalizeExistingRecipePacks(CACHE_ROOT);
            pruneConfigIncompatibleCachedPacks();
        } catch (IOException ignored) {
        }
    }

    private static void migrateLegacyCacheRoot() throws IOException {
        migrateLegacyCacheRoot(LEGACY_CONFIG_CACHE_ROOT);
        migrateLegacyCacheRoot(LEGACY_GAME_CACHE_ROOT);
    }

    private static void migrateLegacyCacheRoot(Path legacyRoot) throws IOException {
        if (Files.exists(CACHE_ROOT) || !Files.exists(legacyRoot)) {
            return;
        }

        Files.createDirectories(CACHE_ROOT.getParent());
        Files.move(legacyRoot, CACHE_ROOT, StandardCopyOption.REPLACE_EXISTING);

        Path legacyParent = legacyRoot.getParent();
        if (legacyParent != null && Files.isDirectory(legacyParent)) {
            try (var entries = Files.list(legacyParent)) {
                if (entries.findAny().isEmpty()) {
                    Files.deleteIfExists(legacyParent);
                }
            }
        }
    }

    private static void normalizeExistingRecipePacks(Path root) throws IOException {
        if (root == null || !Files.isDirectory(root)) {
            return;
        }

        for (String packName : PACK_NAMES) {
            LegacyRecipeJsonCompat.normalizeDatapackInPlace(root.resolve(packName));
        }
    }

    private static void pruneConfigIncompatibleCachedPacks() throws IOException {
        if (!FabricBuildingConfig.enhancedSlabsVerticalSlabs) {
            pruneWoodPackVerticalDependentRecipes();
            prunePackToMetadataOnly(FabricVerticalSlabRecipes.staticPackName());
            pruneStepPackVerticalDependentRecipes();
        }

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            prunePackToMetadataOnly(FabricStepRecipes.staticPackName());
        }
    }

    private static void prunePackToMetadataOnly(String packName) throws IOException {
        Path packPath = getPackPath(packName);
        if (!Files.isDirectory(packPath)) {
            ensurePackRoot(packName);
            return;
        }

        try (var stream = Files.walk(packPath)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                if (path.equals(packPath)) {
                    continue;
                }

                if (Files.isRegularFile(path) && path.getFileName().toString().equals("pack.mcmeta")) {
                    continue;
                }

                Files.deleteIfExists(path);
            }
        }

        ensurePackRoot(packName);
    }

    private static void pruneStepPackVerticalDependentRecipes() throws IOException {
        Path packPath = getPackPath(FabricStepRecipes.staticPackName());
        if (!Files.isDirectory(packPath)) {
            ensurePackRoot(FabricStepRecipes.staticPackName());
            return;
        }

        try (var stream = Files.walk(packPath)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                String relative = packPath.relativize(path).toString().replace('\\', '/');
                if (relative.equals("pack.mcmeta")) {
                    continue;
                }

                boolean remove = relative.equals(".bitsandbalance-pack.sha256")
                        || relative.equals(".bitsandbalance-input.sha256")
                        || relative.startsWith("data/bitsandbalance/recipe/crafting/vertical_steps/")
                        || relative.startsWith("data/bitsandbalance/recipe/crafting/vertical_slabs/from_vertical_step/")
                        || relative.startsWith("data/bitsandbalance/recipe/60_vertical_step_");
                if (remove) {
                    Files.deleteIfExists(path);
                }
            }
        }

        deleteEmptyDirectories(packPath);
        ensurePackRoot(FabricStepRecipes.staticPackName());
    }

    private static void pruneWoodPackVerticalDependentRecipes() throws IOException {
        Path packPath = getPackPath(FabricWoodStonecuttingRecipes.packName());
        if (!Files.isDirectory(packPath)) {
            ensurePackRoot(FabricWoodStonecuttingRecipes.packName());
            return;
        }

        try (var stream = Files.walk(packPath)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                String relative = packPath.relativize(path).toString().replace('\\', '/');
                if (relative.equals("pack.mcmeta")) {
                    continue;
                }

                boolean remove = relative.equals(".bitsandbalance-pack.sha256")
                        || relative.equals(".bitsandbalance-input.sha256")
                        || relative.startsWith("data/bitsandbalance/recipe/40_vertical_slab_wood_sc_");
                if (remove) {
                    Files.deleteIfExists(path);
                }
            }
        }

        deleteEmptyDirectories(packPath);
        ensurePackRoot(FabricWoodStonecuttingRecipes.packName());
    }

    private static void deleteEmptyDirectories(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                if (path.equals(root) || !Files.isDirectory(path)) {
                    continue;
                }

                try (var children = Files.list(path)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(path);
                    }
                }
            }
        }
    }

    static Path getPackPath(String packName) {
        return CACHE_ROOT.resolve(packName);
    }

    static void ensurePackRoot(String packName) throws IOException {
        Path packPath = getPackPath(packName);
        Files.createDirectories(packPath);

        String packMcmeta = PACK_MCMETAS.get(packName);
        if (packMcmeta != null && !packMcmeta.isBlank()) {
            Files.writeString(
                    packPath.resolve("pack.mcmeta"),
                    packMcmeta,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    static BootstrapState buildBootstrapState(Path worldRoot) throws IOException {
        return buildBootstrapState(worldRoot, Set.of());
    }

    static BootstrapState buildBootstrapState(Path worldRoot, Set<String> refreshedPackNames) throws IOException {
        Path normalizedWorldRoot = normalize(worldRoot);
        String worldDatapackFingerprint = buildWorldDatapackFingerprint(normalizedWorldRoot);

        PackBootstrap alternative = buildPackBootstrap(
                normalizedWorldRoot,
                FabricAlternativeRecipes.packName(),
                FabricRecipesConfig.anyEnabled(),
                buildAlternativeBootstrapFingerprint(worldDatapackFingerprint),
            refreshedPackNames.contains(FabricAlternativeRecipes.packName())
        );
        PackBootstrap wood = buildPackBootstrap(
                normalizedWorldRoot,
                FabricWoodStonecuttingRecipes.packName(),
                true,
            buildWoodBootstrapFingerprint(worldDatapackFingerprint),
            refreshedPackNames.contains(FabricWoodStonecuttingRecipes.packName())
        );
        PackBootstrap vertical = buildPackBootstrap(
                normalizedWorldRoot,
                FabricVerticalSlabRecipes.staticPackName(),
                FabricBuildingConfig.enhancedSlabsVerticalSlabs,
            buildVerticalSlabStaticBootstrapFingerprint(worldDatapackFingerprint),
            refreshedPackNames.contains(FabricVerticalSlabRecipes.staticPackName())
        );
        PackBootstrap verticalMirror = buildPackBootstrap(
            normalizedWorldRoot,
            FabricVerticalSlabRecipes.mirrorPackName(),
            false,
            buildVerticalSlabMirrorBootstrapFingerprint(worldDatapackFingerprint),
            refreshedPackNames.contains(FabricVerticalSlabRecipes.mirrorPackName())
        );
        PackBootstrap step = buildPackBootstrap(
            normalizedWorldRoot,
            FabricStepRecipes.staticPackName(),
            FabricBuildingConfig.enhancedSlabsSteps,
            buildStepStaticBootstrapFingerprint(worldDatapackFingerprint),
            refreshedPackNames.contains(FabricStepRecipes.staticPackName())
        );
        PackBootstrap stepMirror = buildPackBootstrap(
            normalizedWorldRoot,
            FabricStepRecipes.mirrorPackName(),
            false,
            buildStepMirrorBootstrapFingerprint(worldDatapackFingerprint),
            refreshedPackNames.contains(FabricStepRecipes.mirrorPackName())
        );

        return new BootstrapState(alternative, wood, vertical, verticalMirror, step, stepMirror);
    }

    static void persistBootstrapState(Path worldRoot, BootstrapState state) throws IOException {
        Path stateDir = stateDir(normalize(worldRoot));
        Files.createDirectories(stateDir);

        for (PackBootstrap pack : state.packs()) {
            Path statePath = stateDir.resolve(pack.packName() + BOOTSTRAP_FILE_SUFFIX);
            if (pack.pending()) {
                Files.deleteIfExists(statePath);
                continue;
            }
            Files.writeString(
                    statePath,
                    pack.bootstrapFingerprint(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    static boolean enableReusablePacks(PackRepository packRepository, BootstrapState state) {
        boolean sourcesChanged = ensureRepositorySources(packRepository);
        if (sourcesChanged) {
            packRepository.reload();
        }
        boolean changed = false;
        for (PackBootstrap pack : state.packs()) {
            if (!pack.enabled() || pack.pending()) {
                continue;
            }
            changed |= enableGeneratedPack(packRepository, pack.packName(), false);
        }
        return changed;
    }

    static void resetGeneratedPackSelections(PackRepository packRepository) {
        boolean sourcesChanged = ensureRepositorySources(packRepository);
        if (sourcesChanged) {
            packRepository.reload();
        }

        List<String> selected = new ArrayList<>(packRepository.getSelectedPacks().stream().map(Pack::getId).toList());
        boolean removedAny = selected.removeIf(id -> id != null && PACK_NAMES.stream().anyMatch(name -> id.equals(generatedPackId(name))));
        if (removedAny) {
            packRepository.setSelected(selected);
        }
    }

    static boolean restoreConfiguredPackSelections(Path worldRoot, PackRepository packRepository) throws IOException {
        boolean sourcesChanged = ensureRepositorySources(packRepository);
        if (sourcesChanged) {
            packRepository.reload();
        }
        return enableReusablePacks(packRepository, buildBootstrapState(worldRoot));
    }

    static boolean applyBootstrapSelections(PackRepository packRepository, BootstrapState state) {
        ensureRepositorySources(packRepository);
        packRepository.reload();

        List<String> selected = new ArrayList<>(packRepository.getSelectedPacks().stream().map(Pack::getId).toList());
        boolean changed = selected.removeIf(id -> id != null && PACK_NAMES.stream().anyMatch(name -> id.equals(generatedPackId(name))));

        for (PackBootstrap pack : state.packs()) {
            if (!pack.enabled() || pack.pending()) {
                continue;
            }

            String packId = generatedPackId(pack.packName());
            boolean available = packRepository.getAvailablePacks().stream().anyMatch(packEntry -> packId.equals(packEntry.getId()));
            if (available && !selected.contains(packId)) {
                selected.add(packId);
                changed = true;
            }
        }

        if (changed) {
            packRepository.setSelected(selected);
        }

        return changed;
    }

    static <T> Dynamic<T> applyBootstrapSelectionsToDataTag(Dynamic<T> dataTag, BootstrapState state) {
        Dynamic<T> sanitized = sanitizePersistedPackSelections(dataTag);
        Optional<Dynamic<T>> dataPacks = sanitized.get("DataPacks").result();
        if (dataPacks.isEmpty()) {
            return sanitized;
        }

        Dynamic<T> dataPacksSection = dataPacks.get();
        Stream<Dynamic<T>> existingEnabled = dataPacksSection.get("Enabled")
                .asStreamOpt()
                .result()
                .orElseGet(Stream::empty)
                .filter(entry -> !isSanitizedPackId(entry.asString("")));

        Set<String> generatedEnabled = state.packs().stream()
                .filter(pack -> pack.enabled() && !pack.pending())
                .map(pack -> generatedPackId(pack.packName()))
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Stream<Dynamic<T>> existingDisabled = dataPacksSection.get("Disabled")
            .asStreamOpt()
            .result()
            .orElseGet(Stream::empty)
            .filter(entry -> !isSanitizedPackId(entry.asString("")));

        Stream<Dynamic<T>> generatedDisabled = PACK_NAMES.stream()
            .map(FabricGeneratedWorldDataPacks::generatedPackId)
            .filter(packId -> !generatedEnabled.contains(packId))
            .map(dataPacksSection::createString);

        Stream<Dynamic<T>> generatedEntries = generatedEnabled.stream().map(dataPacksSection::createString);
        Dynamic<T> enabled = dataPacksSection.createList(Stream.concat(existingEnabled, generatedEntries));
        Dynamic<T> disabled = dataPacksSection.createList(Stream.concat(existingDisabled, generatedDisabled));
        return sanitized.set("DataPacks", dataPacksSection.set("Enabled", enabled).set("Disabled", disabled));
    }

    public static boolean ensureRepositorySources(PackRepository packRepository) {
        synchronized (SERVER_DATA_REPOSITORIES) {
            SERVER_DATA_REPOSITORIES.add(packRepository);
        }
        return addGeneratedRepositorySources(packRepository);
    }

    public static boolean ensureKnownRepositorySources(PackRepository packRepository) {
        synchronized (SERVER_DATA_REPOSITORIES) {
            if (!SERVER_DATA_REPOSITORIES.contains(packRepository)) {
                return false;
            }
        }
        return addGeneratedRepositorySources(packRepository);
    }

    private static boolean addGeneratedRepositorySources(PackRepository packRepository) {
        PackRepositoryAccessor accessor = (PackRepositoryAccessor) packRepository;
        Set<RepositorySource> current = new LinkedHashSet<>(accessor.bitsandbalance$getSources());
        boolean changed = false;

        for (String packName : PACK_NAMES) {
            RepositorySource source = GENERATED_SOURCES.get(packName);
            if (source != null && current.add(source)) {
                changed = true;
            }
        }

        if (changed) {
            accessor.bitsandbalance$setSources(current);
        }
        return changed;
    }

    static boolean enableGeneratedPack(PackRepository packs, String packName, boolean reloadRepository) {
        boolean sourcesChanged = ensureRepositorySources(packs);
        if (reloadRepository || sourcesChanged) {
            packs.reload();
        }

        String packId = generatedPackId(packName);
        boolean exists = packs.getAvailablePacks().stream().anyMatch(pack -> pack.getId().equals(packId));
        if (!exists) {
            return false;
        }

        boolean alreadySelected = packs.getSelectedPacks().stream().anyMatch(pack -> pack.getId().equals(packId));
        if (alreadySelected) {
            return false;
        }

        List<String> selected = new ArrayList<>(packs.getSelectedPacks().stream().map(Pack::getId).toList());
        selected.add(packId);
        packs.setSelected(selected);
        return true;
    }

    static boolean disableGeneratedPack(PackRepository packs, String packName, boolean reloadRepository) {
        boolean sourcesChanged = ensureRepositorySources(packs);
        if (reloadRepository || sourcesChanged) {
            packs.reload();
        }

        String packId = generatedPackId(packName);
        List<String> selected = new ArrayList<>(packs.getSelectedPacks().stream().map(Pack::getId).toList());
        if (selected.remove(packId)) {
            packs.setSelected(selected);
            return true;
        }
        return false;
    }

    static boolean sanitizeLegacyWorldDatapacks(Path worldRoot, PackRepository packRepository) {
        try {
            Path datapacksRoot = normalize(worldRoot).resolve(DATAPACKS_DIR);
            boolean deleted = false;
            for (String packName : PACK_NAMES) {
                deleted |= deletePack(datapacksRoot.resolve(packName));
            }

            List<String> selected = new ArrayList<>(packRepository.getSelectedPacks().stream().map(Pack::getId).toList());
            boolean removedSelections = selected.removeIf(id -> id != null && PACK_NAMES.stream().anyMatch(name -> id.equals("file/" + name)));
            if (removedSelections) {
                packRepository.setSelected(selected);
            }

            if (deleted || removedSelections) {
                packRepository.reload();
            }
            return deleted || removedSelections;
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean disableRuntimeMirrorPacks(PackRepository packRepository) {
        boolean changed = false;
        changed |= disableGeneratedPack(packRepository, FabricVerticalSlabRecipes.mirrorPackName(), false);
        changed |= disableGeneratedPack(packRepository, FabricStepRecipes.mirrorPackName(), false);
        return changed;
    }

    public static <T> Dynamic<T> sanitizePersistedPackSelections(Dynamic<T> dataTag) {
        Optional<Dynamic<T>> dataPacks = dataTag.get("DataPacks").result();
        if (dataPacks.isEmpty()) {
            return dataTag;
        }

        return dataTag.set("DataPacks", sanitizePersistedPackSelectionsSection(dataPacks.get()));
    }

    private static <T> Dynamic<T> sanitizePersistedPackSelectionsSection(Dynamic<T> dataPacks) {
        Dynamic<T> sanitized = dataPacks;

        Optional<Dynamic<T>> enabled = sanitized.get("Enabled").result();
        if (enabled.isPresent()) {
            sanitized = sanitized.set("Enabled", sanitizePersistedPackList(enabled.get()));
        }

        Optional<Dynamic<T>> disabled = sanitized.get("Disabled").result();
        if (disabled.isPresent()) {
            sanitized = sanitized.set("Disabled", sanitizePersistedPackList(disabled.get()));
        }

        return sanitized;
    }

    private static <T> Dynamic<T> sanitizePersistedPackList(Dynamic<T> packList) {
        return packList.asStreamOpt()
                .result()
                .map(stream -> packList.createList(stream.filter(entry -> !isSanitizedPackId(entry.asString("")))))
                .orElse(packList);
    }

    private static boolean isSanitizedPackId(String packId) {
        if (packId == null || packId.isBlank()) {
            return false;
        }

        return PACK_NAMES.stream().anyMatch(name -> packId.equals(generatedPackId(name)) || packId.equals("file/" + name));
    }

    record BootstrapState(PackBootstrap alternative, PackBootstrap wood, PackBootstrap vertical, PackBootstrap verticalMirror, PackBootstrap step, PackBootstrap stepMirror) {
        boolean hasPending() {
            return packs().stream().anyMatch(PackBootstrap::pending);
        }

        List<PackBootstrap> packs() {
            return List.of(alternative, wood, vertical, verticalMirror, step, stepMirror);
        }
    }

    record PackBootstrap(String packName, boolean enabled, boolean pending, String bootstrapFingerprint) {
    }

    private static PackBootstrap buildPackBootstrap(Path worldRoot, String packName, boolean enabled, String bootstrapFingerprint) throws IOException {
        return buildPackBootstrap(worldRoot, packName, enabled, bootstrapFingerprint, false);
    }

    private static PackBootstrap buildPackBootstrap(Path worldRoot, String packName, boolean enabled, String bootstrapFingerprint, boolean refreshed) throws IOException {
        boolean fingerprintMatches = matchesBootstrapFingerprint(worldRoot, packName, bootstrapFingerprint);
        boolean packReady = !enabled || hasPackMetadata(packName);
        boolean current = !enabled || (packReady && (fingerprintMatches || refreshed));
        return new PackBootstrap(packName, enabled, !current, bootstrapFingerprint);
    }

    private static boolean matchesBootstrapFingerprint(Path worldRoot, String packName, String bootstrapFingerprint) throws IOException {
        Path statePath = stateDir(worldRoot).resolve(packName + BOOTSTRAP_FILE_SUFFIX);
        if (!Files.exists(statePath)) {
            return false;
        }
        return Files.readString(statePath, StandardCharsets.UTF_8).trim().equals(bootstrapFingerprint);
    }

    private static String buildAlternativeBootstrapFingerprint(String worldDatapackFingerprint) {
        DynamicRecipeInputFingerprint.Builder builder = baseBootstrapBuilder(worldDatapackFingerprint);
        builder.add("generator_version", 2);
        builder.add("alt_repeater", FabricRecipesConfig.enableAlternativeRepeaterRecipe);
        builder.add("chest_from_logs", FabricRecipesConfig.enableChestFromLogsRecipe);
        builder.add("raw_iron_smelting", FabricRecipesConfig.enableRawIronSmeltingRecipe);
        builder.add("raw_gold_smelting", FabricRecipesConfig.enableRawGoldSmeltingRecipe);
        builder.add("raw_copper_smelting", FabricRecipesConfig.enableRawCopperSmeltingRecipe);
        builder.add("raw_iron_blasting", FabricRecipesConfig.enableRawIronBlastingRecipe);
        builder.add("raw_gold_blasting", FabricRecipesConfig.enableRawGoldBlastingRecipe);
        builder.add("raw_copper_blasting", FabricRecipesConfig.enableRawCopperBlastingRecipe);
        builder.add("recovery_compass", FabricRecipesConfig.enableRecoveryCompassRecipe);
        builder.add("map_ink_sac", FabricRecipesConfig.enableMapInkSacRecipe);
        builder.add("ender_eye", FabricRecipesConfig.enableEnderEyeRecipe);
        builder.add("stair_override", FabricRecipesConfig.enableStairRecipeOverride);
        builder.add("compact_stair", FabricRecipesConfig.enableCompactStairRecipe);
        return builder.build();
    }

    private static String buildFeatureBootstrapFingerprint(String key, boolean enabled, String worldDatapackFingerprint) {
        return baseBootstrapBuilder(worldDatapackFingerprint)
                .add(key, enabled)
                .build();
    }

    private static String buildFeatureBootstrapFingerprint(String key, boolean enabled, String worldDatapackFingerprint, int generatorVersion) {
        return baseBootstrapBuilder(worldDatapackFingerprint)
                .add("generator_version", generatorVersion)
                .add(key, enabled)
                .build();
    }

    private static String buildStepBootstrapFingerprint(String worldDatapackFingerprint) {
        return baseBootstrapBuilder(worldDatapackFingerprint)
                .add("generator_version", 7)
                .add("steps_enabled", FabricBuildingConfig.enhancedSlabsSteps)
                .add("vertical_slabs_enabled", FabricBuildingConfig.enhancedSlabsVerticalSlabs)
                .build();
    }

    private static String buildVerticalSlabStaticBootstrapFingerprint(String worldDatapackFingerprint) {
        return baseBootstrapBuilder(worldDatapackFingerprint)
                .add("generator_version", 11)
                .add("vertical_enabled", FabricBuildingConfig.enhancedSlabsVerticalSlabs)
                .add("static_input", DynamicRecipeInputFingerprints.buildVerticalSlabStaticFingerprint())
                .build();
    }

    private static String buildVerticalSlabMirrorBootstrapFingerprint(String worldDatapackFingerprint) {
        return baseBootstrapBuilder(worldDatapackFingerprint)
                .add("generator_version", 5)
                .add("vertical_enabled", FabricBuildingConfig.enhancedSlabsVerticalSlabs)
                .build();
    }

    private static String buildStepStaticBootstrapFingerprint(String worldDatapackFingerprint) {
        return baseBootstrapBuilder(worldDatapackFingerprint)
                .add("generator_version", 15)
                .add("steps_enabled", FabricBuildingConfig.enhancedSlabsSteps)
                .add("vertical_slabs_enabled", FabricBuildingConfig.enhancedSlabsVerticalSlabs)
                .add("static_input", DynamicRecipeInputFingerprints.buildStepStaticFingerprint())
                .build();
    }

    private static String buildStepMirrorBootstrapFingerprint(String worldDatapackFingerprint) {
        return baseBootstrapBuilder(worldDatapackFingerprint)
                .add("generator_version", 6)
                .add("steps_enabled", FabricBuildingConfig.enhancedSlabsSteps)
                .add("vertical_slabs_enabled", FabricBuildingConfig.enhancedSlabsVerticalSlabs)
                .build();
    }

    private static String buildWoodBootstrapFingerprint(String worldDatapackFingerprint) {
        return baseBootstrapBuilder(worldDatapackFingerprint)
                .add("generator_version", 6)
                .add("vertical_slabs_enabled", FabricBuildingConfig.enhancedSlabsVerticalSlabs)
                .build();
    }

    private static DynamicRecipeInputFingerprint.Builder baseBootstrapBuilder(String worldDatapackFingerprint) {
        return DynamicRecipeInputFingerprint.builder()
                .add("mods", buildInstalledModsFingerprint())
                .add("world_datapacks", worldDatapackFingerprint);
    }

    private static String buildInstalledModsFingerprint() {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        FabricLoader.getInstance().getAllMods().stream()
                .sorted(Comparator.comparing(mod -> mod.getMetadata().getId()))
                .forEach(mod -> builder.add(mod.getMetadata().getId() + "@" + mod.getMetadata().getVersion().getFriendlyString()));
        return builder.build();
    }

    private static String buildWorldDatapackFingerprint(Path worldRoot) throws IOException {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        Path datapacksRoot = worldRoot.resolve(DATAPACKS_DIR);
        if (!Files.isDirectory(datapacksRoot)) {
            return builder.build();
        }

        try (var stream = Files.walk(datapacksRoot)) {
            for (Path path : stream.filter(Files::isRegularFile).sorted().toList()) {
                Path relativePath = datapacksRoot.relativize(path);
                if (relativePath.getNameCount() > 0) {
                    String topLevel = relativePath.getName(0).toString();
                    if (topLevel.equals(BOOTSTRAP_STATE_DIR)
                            || topLevel.equals(FabricAlternativeRecipes.packName())
                            || topLevel.equals(FabricWoodStonecuttingRecipes.packName())
                            || topLevel.equals(FabricVerticalSlabRecipes.staticPackName())
                            || topLevel.equals(FabricVerticalSlabRecipes.mirrorPackName())
                            || topLevel.equals(FabricStepRecipes.staticPackName())
                            || topLevel.equals(FabricStepRecipes.mirrorPackName())) {
                        continue;
                    }
                }

                try {
                    builder.add(relativePath.toString().replace('\\', '/'));
                    builder.add("size", Files.size(path));
                    builder.add("modified", Files.getLastModifiedTime(path).toMillis());
                } catch (IOException ignored) {
                }
            }
        }

        return builder.build();
    }

    private static Path stateDir(Path worldRoot) {
        return worldRoot.resolve(DATAPACKS_DIR).resolve(BOOTSTRAP_STATE_DIR);
    }

    private static Path normalize(Path worldRoot) {
        return worldRoot.toAbsolutePath().normalize();
    }

    private static boolean hasPackMetadata(String packName) {
        Path packPath = getPackPath(packName);
        return Files.isDirectory(packPath) && Files.isRegularFile(packPath.resolve("pack.mcmeta"));
    }

    private static RepositorySource createSource(String packName, Path packPath) {
        return acceptor -> {
            Pack pack = Pack.readMetaAndCreate(
                    new PackLocationInfo(
                            generatedPackId(packName),
                            Component.literal("Bits and Balance: " + packName),
                            PackSource.BUILT_IN,
                            java.util.Optional.empty()
                    ),
                    new PathPackResources.PathResourcesSupplier(packPath),
                    PackType.SERVER_DATA,
                    new PackSelectionConfig(false, Pack.Position.TOP, false)
            );
            if (pack != null) {
                acceptor.accept(pack);
            }
        };
    }

    private static String generatedPackId(String packName) {
        return "bitsandbalance/generated/" + packName;
    }

    private static boolean deletePack(Path packPath) throws IOException {
        if (!Files.exists(packPath)) {
            return false;
        }

        try (var stream = Files.walk(packPath)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
        return true;
    }
}
