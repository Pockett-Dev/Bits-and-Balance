package org.onenonly.bitsandbalance.fabric.recipe;

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
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricConfigPaths;
import org.onenonly.bitsandbalance.fabric.config.FabricRecipesConfig;
import org.onenonly.bitsandbalance.fabric.mixin.PackRepositoryAccessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FabricGeneratedWorldDataPacks {
    private static final String DATAPACKS_DIR = "datapacks";
    private static final String BOOTSTRAP_STATE_DIR = ".bitsandbalance-bootstrap";
    private static final String BOOTSTRAP_FILE_SUFFIX = ".sha256";
    private static final Path LEGACY_CACHE_ROOT = FabricLoader.getInstance().getGameDir().resolve("bitsandbalance-runtime-packs").resolve("server-data");
    private static final Path CACHE_ROOT = FabricConfigPaths.configDir().resolve("runtime-packs").resolve("server-data");
    private static final List<String> PACK_NAMES = List.of(
            FabricAlternativeRecipes.packName(),
            FabricWoodStonecuttingRecipes.packName(),
            FabricVerticalSlabRecipes.packName(),
            FabricStepRecipes.packName()
    );
        private static final Map<String, String> PACK_MCMETAS = Map.of(
            FabricAlternativeRecipes.packName(), FabricAlternativeRecipes.packMcmeta(),
            FabricWoodStonecuttingRecipes.packName(), FabricWoodStonecuttingRecipes.packMcmeta(),
            FabricVerticalSlabRecipes.packName(), FabricVerticalSlabRecipes.packMcmeta(),
            FabricStepRecipes.packName(), FabricStepRecipes.packMcmeta()
        );
    private static final Map<String, RepositorySource> GENERATED_SOURCES = Map.of(
            FabricAlternativeRecipes.packName(), createSource(FabricAlternativeRecipes.packName(), getPackPath(FabricAlternativeRecipes.packName())),
            FabricWoodStonecuttingRecipes.packName(), createSource(FabricWoodStonecuttingRecipes.packName(), getPackPath(FabricWoodStonecuttingRecipes.packName())),
            FabricVerticalSlabRecipes.packName(), createSource(FabricVerticalSlabRecipes.packName(), getPackPath(FabricVerticalSlabRecipes.packName())),
            FabricStepRecipes.packName(), createSource(FabricStepRecipes.packName(), getPackPath(FabricStepRecipes.packName()))
    );

    private FabricGeneratedWorldDataPacks() {
    }

    public static void initializeDirectories() {
        try {
            migrateLegacyCacheRoot();
            Files.createDirectories(CACHE_ROOT);
            for (String packName : PACK_NAMES) {
                ensurePackRoot(packName);
            }
        } catch (IOException ignored) {
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

    private static void migrateLegacyCacheRoot() throws IOException {
        if (Files.exists(CACHE_ROOT) || !Files.exists(LEGACY_CACHE_ROOT)) {
            return;
        }

        Files.createDirectories(CACHE_ROOT.getParent());
        Files.move(LEGACY_CACHE_ROOT, CACHE_ROOT, StandardCopyOption.REPLACE_EXISTING);

        Path legacyParent = LEGACY_CACHE_ROOT.getParent();
        if (legacyParent != null && Files.isDirectory(legacyParent)) {
            try (var entries = Files.list(legacyParent)) {
                if (entries.findAny().isEmpty()) {
                    Files.deleteIfExists(legacyParent);
                }
            }
        }
    }

    static BootstrapState buildBootstrapState(Path worldRoot) throws IOException {
        Path normalizedWorldRoot = normalize(worldRoot);
        String worldDatapackFingerprint = buildWorldDatapackFingerprint(normalizedWorldRoot);

        PackBootstrap alternative = buildPackBootstrap(
                normalizedWorldRoot,
                FabricAlternativeRecipes.packName(),
                FabricRecipesConfig.anyEnabled(),
                buildAlternativeBootstrapFingerprint(worldDatapackFingerprint)
        );
        PackBootstrap wood = buildPackBootstrap(
                normalizedWorldRoot,
                FabricWoodStonecuttingRecipes.packName(),
                true,
                buildInstalledModsFingerprint()
        );
        PackBootstrap vertical = buildPackBootstrap(
                normalizedWorldRoot,
                FabricVerticalSlabRecipes.packName(),
                FabricBuildingConfig.enhancedSlabsVerticalSlabs,
            buildFeatureBootstrapFingerprint("vertical_enabled", FabricBuildingConfig.enhancedSlabsVerticalSlabs, worldDatapackFingerprint, 3)
        );
        PackBootstrap step = buildPackBootstrap(
                normalizedWorldRoot,
                FabricStepRecipes.packName(),
                FabricBuildingConfig.enhancedSlabsSteps,
            buildFeatureBootstrapFingerprint("steps_enabled", FabricBuildingConfig.enhancedSlabsSteps, worldDatapackFingerprint, 6)
        );

        return new BootstrapState(alternative, wood, vertical, step);
    }

    static void persistBootstrapState(Path worldRoot, BootstrapState state) throws IOException {
        Path stateDir = stateDir(normalize(worldRoot));
        Files.createDirectories(stateDir);

        for (PackBootstrap pack : state.packs()) {
            Files.writeString(
                    stateDir.resolve(pack.packName() + BOOTSTRAP_FILE_SUFFIX),
                    pack.bootstrapFingerprint(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    static boolean enableReusablePacks(PackRepository packRepository, BootstrapState state) {
        ensureRepositorySources(packRepository);
        boolean changed = false;
        for (PackBootstrap pack : state.packs()) {
            if (!pack.enabled() || pack.pending()) {
                continue;
            }
            changed |= enableGeneratedPack(packRepository, pack.packName(), false);
        }
        return changed;
    }

    static boolean restoreConfiguredPackSelections(Path worldRoot, PackRepository packRepository) throws IOException {
        ensureRepositorySources(packRepository);
        packRepository.reload();
        return enableReusablePacks(packRepository, buildBootstrapState(worldRoot));
    }

    public static void ensureRepositorySources(PackRepository packRepository) {
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
    }

    static boolean enableGeneratedPack(PackRepository packs, String packName, boolean reloadRepository) {
        ensureRepositorySources(packs);
        if (reloadRepository) {
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
        ensureRepositorySources(packs);
        if (reloadRepository) {
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

    record BootstrapState(PackBootstrap alternative, PackBootstrap wood, PackBootstrap vertical, PackBootstrap step) {
        boolean hasPending() {
            return packs().stream().anyMatch(PackBootstrap::pending);
        }

        List<PackBootstrap> packs() {
            return List.of(alternative, wood, vertical, step);
        }
    }

    record PackBootstrap(String packName, boolean enabled, boolean pending, String bootstrapFingerprint) {
    }

    private static PackBootstrap buildPackBootstrap(Path worldRoot, String packName, boolean enabled, String bootstrapFingerprint) throws IOException {
        boolean fingerprintMatches = matchesBootstrapFingerprint(worldRoot, packName, bootstrapFingerprint);
        boolean packReady = !enabled || hasPackMetadata(packName);
        return new PackBootstrap(packName, enabled, !(fingerprintMatches && packReady), bootstrapFingerprint);
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
        builder.add("generator_version", 3);
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
                            || topLevel.equals(FabricVerticalSlabRecipes.packName())
                            || topLevel.equals(FabricStepRecipes.packName())) {
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