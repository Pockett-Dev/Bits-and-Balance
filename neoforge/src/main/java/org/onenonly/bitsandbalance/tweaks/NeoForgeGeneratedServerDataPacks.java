package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprint;
import org.onenonly.bitsandbalance.common.recipe.FamilyCompatRecipeGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgeGeneratedServerDataPacks {
    private static final String BOOTSTRAP_FINGERPRINT_FILE = ".bitsandbalance-bootstrap.sha256";
    private static final String DATAPACKS_DIR = "datapacks";
    private static final String BOOTSTRAP_STATE_DIR = ".bitsandbalance-bootstrap";
    private static final String BOOTSTRAP_FILE_SUFFIX = ".sha256";
    private static final Path CACHE_ROOT = FMLPaths.GAMEDIR.get().resolve("bitsandbalance-runtime-packs").resolve("server-data");
    private static final List<String> PACK_NAMES = List.of(
            AlternativeRecipes.packName(),
            WoodStonecuttingRecipes.packName(),
            VerticalSlabStonecuttingRecipes.staticPackName(),
            VerticalSlabStonecuttingRecipes.mirrorPackName(),
            StepStonecuttingRecipes.staticPackName(),
            StepStonecuttingRecipes.mirrorPackName()
    );
    private static final ConcurrentHashMap<Path, BootstrapState> BOOTSTRAP_STATES = new ConcurrentHashMap<>();
    private static final String PLACEHOLDER_PACK_MCMETA = """
            {
              \"pack\": {
                \"description\": \"Bits and Balance: Generated Server Data\",
                \"pack_format\": 88,
                \"min_format\": 88,
                \"max_format\": 88
              }
            }
            """;

    private NeoForgeGeneratedServerDataPacks() {
    }

    public static void initializeDirectories() {
        try {
            Files.createDirectories(CACHE_ROOT);
            for (String packName : PACK_NAMES) {
                ensurePackRoot(packName, PLACEHOLDER_PACK_MCMETA);
            }
        } catch (IOException e) {
            BitsAndBalance.LOGGER.warn("[{}] Failed initializing generated datapack cache root: {}", BitsAndBalance.MODID, e.toString());
        }
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        for (String packName : PACK_NAMES) {
            event.addRepositorySource(createSource(packName, getPackPath(packName)));
        }
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Path worldRoot = normalize(server.getWorldPath(LevelResource.ROOT));

        removeLegacyWorldDatapacks(server);
        if (NeoForgeIntegratedServerDatapackBootstrap.isPreparedWorld(worldRoot)) {
            return;
        }

        BootstrapState bootstrapState = buildBootstrapState(worldRoot);
        enableReusablePacks(server.getPackRepository(), bootstrapState);

        try {
            if (bootstrapState.woodPending()) {
                WoodStonecuttingRecipes.prepareWorldDatapack(server);
                bootstrapState = bootstrapState.withPackPending(WoodStonecuttingRecipes.packName(), false);
            }
            if (bootstrapState.verticalStaticPending()) {
                VerticalSlabStonecuttingRecipes.prepareStaticDatapack();
                bootstrapState = bootstrapState.withPackPending(VerticalSlabStonecuttingRecipes.staticPackName(), false);
            }
            if (bootstrapState.stepStaticPending()) {
                StepStonecuttingRecipes.prepareStaticDatapack();
                bootstrapState = bootstrapState.withPackPending(StepStonecuttingRecipes.staticPackName(), false);
            }
            enableReusablePacks(server.getPackRepository(), bootstrapState, Set.of(
                    WoodStonecuttingRecipes.packName(),
                    VerticalSlabStonecuttingRecipes.staticPackName(),
                    StepStonecuttingRecipes.staticPackName()
            ));
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[{}] Failed bootstrapping reusable NeoForge datapacks before load: {}", BitsAndBalance.MODID, e.toString());
        }

        storeState(worldRoot, bootstrapState);
    }

    public static BootstrapState buildBootstrapState() {
        return buildBootstrapState((Path) null);
    }

    public static BootstrapState buildBootstrapState(Path worldRoot) {
        return new BootstrapState(
                buildPackBootstrap(worldRoot, AlternativeRecipes.packName(), AlternativeRecipes.anyRecipesEnabledForBootstrap(), buildAlternativeBootstrapFingerprint(null)),
                buildPackBootstrap(worldRoot, WoodStonecuttingRecipes.packName(), true, buildWoodBootstrapFingerprint()),
                buildPackBootstrap(worldRoot, VerticalSlabStonecuttingRecipes.staticPackName(), Config.enhancedSlabsVerticalSlabs, buildVerticalStaticBootstrapFingerprint()),
                buildPackBootstrap(worldRoot, VerticalSlabStonecuttingRecipes.mirrorPackName(), Config.enhancedSlabsVerticalSlabs, buildVerticalMirrorBootstrapFingerprint(worldRoot)),
                buildPackBootstrap(worldRoot, StepStonecuttingRecipes.staticPackName(), Config.enhancedSlabsSteps, buildStepStaticBootstrapFingerprint()),
                buildPackBootstrap(worldRoot, StepStonecuttingRecipes.mirrorPackName(), Config.enhancedSlabsSteps, buildStepMirrorBootstrapFingerprint(worldRoot))
        );
    }

    public static BootstrapState consumeState(Path worldRoot) {
        return BOOTSTRAP_STATES.remove(normalize(worldRoot));
    }

    public static void storeState(Path worldRoot, BootstrapState state) {
        BOOTSTRAP_STATES.put(normalize(worldRoot), state);
    }

    public static void persistBootstrapState(Path worldRoot, BootstrapState state) throws IOException {
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

    public static Path getPackPath(String packName) {
        return CACHE_ROOT.resolve(packName);
    }

    public static void writeBootstrapFingerprint(String packName, String fingerprint) throws IOException {
        ensurePackRoot(packName, PLACEHOLDER_PACK_MCMETA);
        Files.writeString(
                getPackPath(packName).resolve(BOOTSTRAP_FINGERPRINT_FILE),
                fingerprint,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public static boolean matchesBootstrapFingerprint(String packName, String fingerprint) throws IOException {
        Path fingerprintPath = getPackPath(packName).resolve(BOOTSTRAP_FINGERPRINT_FILE);
        if (!Files.exists(fingerprintPath)) {
            return false;
        }
        return Files.readString(fingerprintPath, StandardCharsets.UTF_8).trim().equals(fingerprint);
    }

    public static boolean clearPack(String packName, String packMcmeta) throws IOException {
        Path packPath = getPackPath(packName);
        boolean changed = false;
        if (Files.exists(packPath)) {
            try (var stream = Files.walk(packPath)) {
                for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                    if (path.equals(packPath)) {
                        continue;
                    }
                    Files.deleteIfExists(path);
                    changed = true;
                }
            }
        }

        ensurePackRoot(packName, packMcmeta);
        return changed;
    }

    public static String buildAlternativeBootstrapFingerprint(MinecraftServer server) {
        DynamicRecipeInputFingerprint.Builder builder = baseBootstrapBuilder();
        builder.add("alt_repeater", Config.enableAlternativeRepeaterRecipe);
        builder.add("chest_from_logs", Config.enableChestFromLogsRecipe);
        builder.add("map_ink_sac", Config.enableMapInkSacRecipe);
        builder.add("recovery_compass", Config.enableRecoveryCompassRecipe);
        builder.add("ender_eye", Config.enableEnderEyeRecipe);
        builder.add("raw_iron_smelting", Config.enableRawIronSmeltingRecipe);
        builder.add("raw_gold_smelting", Config.enableRawGoldSmeltingRecipe);
        builder.add("raw_copper_smelting", Config.enableRawCopperSmeltingRecipe);
        builder.add("raw_iron_blasting", Config.enableRawIronBlastingRecipe);
        builder.add("raw_gold_blasting", Config.enableRawGoldBlastingRecipe);
        builder.add("raw_copper_blasting", Config.enableRawCopperBlastingRecipe);
        builder.add("stair_override", Config.enableStairRecipeOverride);
        builder.add("compact_stair", Config.enableCompactStairRecipe);
        builder.add("stair_families", FamilyCompatRecipeGenerator.buildStairFamilyFingerprint());
        return builder.build();
    }

    public static String buildWoodBootstrapFingerprint() {
        return baseBootstrapBuilder()
                .add("vertical_enabled", Config.enhancedSlabsVerticalSlabs)
                .add("steps_enabled", Config.enhancedSlabsSteps)
                .build();
    }

    public static String buildVerticalStaticBootstrapFingerprint() {
        return baseBootstrapBuilder()
                .add("generator_version", 1)
                .add("vertical_enabled", Config.enhancedSlabsVerticalSlabs)
                .add("slab_families", FamilyCompatRecipeGenerator.buildSlabFamilyFingerprint())
                .build();
    }

    public static String buildVerticalMirrorBootstrapFingerprint(MinecraftServer server) {
        return buildVerticalMirrorBootstrapFingerprint(server == null ? null : server.getWorldPath(LevelResource.ROOT));
    }

    public static String buildVerticalMirrorBootstrapFingerprint(Path worldRoot) {
        return baseBootstrapBuilder()
                .add("generator_version", 1)
                .add("vertical_enabled", Config.enhancedSlabsVerticalSlabs)
                .add("slab_families", FamilyCompatRecipeGenerator.buildSlabFamilyFingerprint())
                .add("world_datapacks", buildWorldDatapackFingerprint(worldRoot))
                .build();
    }

    public static String buildStepStaticBootstrapFingerprint() {
        return baseBootstrapBuilder()
                .add("generator_version", 1)
                .add("steps_enabled", Config.enhancedSlabsSteps)
                .add("slab_families", FamilyCompatRecipeGenerator.buildSlabFamilyFingerprint())
                .build();
    }

    public static String buildStepMirrorBootstrapFingerprint(MinecraftServer server) {
        return buildStepMirrorBootstrapFingerprint(server == null ? null : server.getWorldPath(LevelResource.ROOT));
    }

    public static String buildStepMirrorBootstrapFingerprint(Path worldRoot) {
        return baseBootstrapBuilder()
                .add("generator_version", 1)
                .add("steps_enabled", Config.enhancedSlabsSteps)
                .add("slab_families", FamilyCompatRecipeGenerator.buildSlabFamilyFingerprint())
                .add("world_datapacks", buildWorldDatapackFingerprint(worldRoot))
                .build();
    }

    private static DynamicRecipeInputFingerprint.Builder baseBootstrapBuilder() {
        return DynamicRecipeInputFingerprint.builder().add("mods", buildInstalledModsFingerprint());
    }

    static String buildInstalledModsFingerprint() {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        ModList.get().getMods().stream()
                .sorted(Comparator.comparing(mod -> mod.getModId()))
                .forEach(mod -> builder.add(mod.getModId() + "@" + mod.getVersion()));
        return builder.build();
    }

    private static String buildWorldDatapackFingerprint(Path worldRoot) {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        if (worldRoot == null) {
            return builder.build();
        }

        Path datapacksRoot = normalize(worldRoot).resolve(DATAPACKS_DIR);
        if (!Files.isDirectory(datapacksRoot)) {
            return builder.build();
        }

        try (var stream = Files.walk(datapacksRoot)) {
            for (Path path : stream.filter(Files::isRegularFile).sorted().toList()) {
                Path relativePath = datapacksRoot.relativize(path);
                if (relativePath.getNameCount() > 0) {
                    String topLevel = relativePath.getName(0).toString();
                    if (topLevel.equals(BOOTSTRAP_STATE_DIR) || PACK_NAMES.contains(topLevel)) {
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
        } catch (IOException ignored) {
        }

        return builder.build();
    }

    public static boolean removeLegacyWorldDatapacks(MinecraftServer server) {
        try {
            PackRepository repository = server.getPackRepository();
            List<String> selectedIds = new ArrayList<>(repository.getSelectedPacks().stream().map(Pack::getId).toList());
            List<String> legacyWorldPackNames = List.of(
                    "bitsandbalance_vertical_slabs",
                    "bitsandbalance_steps",
                    VerticalSlabStonecuttingRecipes.staticPackName(),
                    VerticalSlabStonecuttingRecipes.mirrorPackName(),
                    StepStonecuttingRecipes.staticPackName(),
                    StepStonecuttingRecipes.mirrorPackName()
            );

            boolean removedSelections = selectedIds.removeIf(id -> id != null && legacyWorldPackNames.stream().anyMatch(name -> id.equals("file/" + name)));
            boolean deletedPacks = false;
            Path datapacksRoot = server.getWorldPath(LevelResource.ROOT).resolve(DATAPACKS_DIR);
            for (String packName : legacyWorldPackNames) {
                deletedPacks |= deleteLegacyWorldPack(datapacksRoot.resolve(packName));
            }

            if (removedSelections) {
                repository.setSelected(selectedIds);
            }
            if (removedSelections || deletedPacks) {
                BitsAndBalance.LOGGER.info("[{}] Removed legacy NeoForge world datapacks for generated step and vertical slab recipes", BitsAndBalance.MODID);
            }
            return removedSelections || deletedPacks;
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[{}] Failed removing legacy dynamic world datapack selections: {}", BitsAndBalance.MODID, e.toString());
            return false;
        }
    }

    private static boolean deleteLegacyWorldPack(Path packPath) throws IOException {
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

    private static RepositorySource createSource(String packName, Path packPath) {
        return acceptor -> {
            Pack pack = Pack.readMetaAndCreate(
                    new PackLocationInfo(
                            generatedPackId(packName),
                            Component.literal("Bits and Balance: " + packName),
                            PackSource.BUILT_IN,
                            Optional.empty()
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

    public static boolean ensureGeneratedPacksSelected(MinecraftServer server) {
        PackRepository repository = server.getPackRepository();
        repository.reload();

        List<String> selectedIds = new ArrayList<>(repository.getSelectedPacks().stream().map(Pack::getId).toList());
        boolean changed = false;
        for (String packName : PACK_NAMES) {
            String packId = generatedPackId(packName);
            boolean available = repository.getAvailablePacks().stream().anyMatch(pack -> pack.getId().equals(packId));
            if (available && !selectedIds.contains(packId)) {
                selectedIds.add(packId);
                changed = true;
            }
        }

        if (changed) {
            repository.setSelected(selectedIds);
        }
        return changed;
    }

    public static boolean enableReusablePacks(PackRepository repository, BootstrapState state) {
        return enableReusablePacks(repository, state, null);
    }

    public static boolean enableReusablePacks(PackRepository repository, BootstrapState state, Set<String> allowedPackNames) {
        repository.reload();
        List<String> selectedIds = new ArrayList<>(repository.getSelectedPacks().stream().map(Pack::getId).toList());
        boolean changed = false;

        for (PackBootstrap pack : state.packs()) {
            if (allowedPackNames != null && !allowedPackNames.contains(pack.packName())) {
                continue;
            }
            if (!pack.enabled() || pack.pending()) {
                continue;
            }

            String packId = generatedPackId(pack.packName());
            boolean available = repository.getAvailablePacks().stream().anyMatch(candidate -> candidate.getId().equals(packId));
            if (available && !selectedIds.contains(packId)) {
                selectedIds.add(packId);
                changed = true;
            }
        }

        if (changed) {
            repository.setSelected(selectedIds);
        }
        return changed;
    }

    public static List<String> describeSelectedGeneratedPacks(PackRepository repository) {
        return repository.getSelectedPacks().stream()
                .map(Pack::getId)
                .filter(id -> id != null && id.startsWith("bitsandbalance/generated/"))
                .toList();
    }

    public static List<String> describeAvailableGeneratedPacks(PackRepository repository) {
        return repository.getAvailablePacks().stream()
                .map(Pack::getId)
                .filter(id -> id != null && id.startsWith("bitsandbalance/generated/"))
                .toList();
    }

    private static void ensurePackRoot(String packName, String packMcmeta) throws IOException {
        Path packPath = getPackPath(packName);
        Files.createDirectories(packPath);
        Files.writeString(
                packPath.resolve("pack.mcmeta"),
                packMcmeta,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
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

    private static PackBootstrap buildPackBootstrap(Path worldRoot, String packName, boolean enabled, String bootstrapFingerprint) {
        boolean fingerprintMatches = false;
        try {
            fingerprintMatches = matchesBootstrapFingerprint(worldRoot, packName, bootstrapFingerprint);
        } catch (IOException ignored) {
        }
        boolean packReady = !enabled || hasPackMetadata(packName);
        return new PackBootstrap(packName, enabled, !(fingerprintMatches && packReady), bootstrapFingerprint);
    }

    private static boolean matchesBootstrapFingerprint(Path worldRoot, String packName, String bootstrapFingerprint) throws IOException {
        if (worldRoot == null) {
            return false;
        }
        Path statePath = stateDir(normalize(worldRoot)).resolve(packName + BOOTSTRAP_FILE_SUFFIX);
        if (!Files.exists(statePath)) {
            return false;
        }
        return Files.readString(statePath, StandardCharsets.UTF_8).trim().equals(bootstrapFingerprint);
    }

    private static String generatedPackId(String packName) {
        return "bitsandbalance/generated/" + packName;
    }

    public record PackBootstrap(String packName, boolean enabled, boolean pending, String bootstrapFingerprint) {
    }

    public record BootstrapState(PackBootstrap alternative,
                                 PackBootstrap wood,
                                 PackBootstrap verticalStatic,
                                 PackBootstrap verticalMirror,
                                 PackBootstrap stepStatic,
                                 PackBootstrap stepMirror) {
        public static BootstrapState defaultState() {
            return new BootstrapState(
                    new PackBootstrap(AlternativeRecipes.packName(), AlternativeRecipes.anyRecipesEnabledForBootstrap(), true, ""),
                    new PackBootstrap(WoodStonecuttingRecipes.packName(), true, true, ""),
                    new PackBootstrap(VerticalSlabStonecuttingRecipes.staticPackName(), Config.enhancedSlabsVerticalSlabs, true, ""),
                    new PackBootstrap(VerticalSlabStonecuttingRecipes.mirrorPackName(), Config.enhancedSlabsVerticalSlabs, true, ""),
                    new PackBootstrap(StepStonecuttingRecipes.staticPackName(), Config.enhancedSlabsSteps, true, ""),
                    new PackBootstrap(StepStonecuttingRecipes.mirrorPackName(), Config.enhancedSlabsSteps, true, "")
            );
        }

        public boolean alternativePending() {
            return alternative.pending();
        }

        public boolean woodPending() {
            return wood.pending();
        }

        public boolean verticalStaticPending() {
            return verticalStatic.pending();
        }

        public boolean verticalMirrorPending() {
            return verticalMirror.pending();
        }

        public boolean stepStaticPending() {
            return stepStatic.pending();
        }

        public boolean stepMirrorPending() {
            return stepMirror.pending();
        }

        public List<PackBootstrap> reusablePacks() {
            return packs();
        }

        public List<PackBootstrap> packs() {
            return List.of(alternative, wood, verticalStatic, verticalMirror, stepStatic, stepMirror);
        }

        public BootstrapState withPackPending(String packName, boolean pending) {
            return new BootstrapState(
                    alternative.packName().equals(packName) ? new PackBootstrap(alternative.packName(), alternative.enabled(), pending, alternative.bootstrapFingerprint()) : alternative,
                    wood.packName().equals(packName) ? new PackBootstrap(wood.packName(), wood.enabled(), pending, wood.bootstrapFingerprint()) : wood,
                    verticalStatic.packName().equals(packName) ? new PackBootstrap(verticalStatic.packName(), verticalStatic.enabled(), pending, verticalStatic.bootstrapFingerprint()) : verticalStatic,
                    verticalMirror.packName().equals(packName) ? new PackBootstrap(verticalMirror.packName(), verticalMirror.enabled(), pending, verticalMirror.bootstrapFingerprint()) : verticalMirror,
                    stepStatic.packName().equals(packName) ? new PackBootstrap(stepStatic.packName(), stepStatic.enabled(), pending, stepStatic.bootstrapFingerprint()) : stepStatic,
                    stepMirror.packName().equals(packName) ? new PackBootstrap(stepMirror.packName(), stepMirror.enabled(), pending, stepMirror.bootstrapFingerprint()) : stepMirror
            );
        }
    }
}