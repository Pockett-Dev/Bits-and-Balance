package org.onenonly.bitsandbalance.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgeIntegratedServerDatapackBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Path, PreparedWorldState> PREPARED_WORLDS = new ConcurrentHashMap<>();

    private NeoForgeIntegratedServerDatapackBootstrap() {
    }

    public static void prepare(LevelStorageSource.LevelStorageAccess levelStorage, PackRepository packRepository, WorldStem worldStem) {
        Path worldRoot = normalize(levelStorage.getLevelPath(LevelResource.ROOT));

        try {
            Path datapacksRoot = worldRoot.resolve("datapacks");
            Files.createDirectories(datapacksRoot);

            NeoForgeGeneratedServerDataPacks.BootstrapState bootstrapState = NeoForgeGeneratedServerDataPacks.buildBootstrapState(worldRoot);
            Map<String, Boolean> desiredSelections = new LinkedHashMap<>();
            boolean changed = NeoForgeGeneratedServerDataPacks.enableReusablePacks(packRepository, bootstrapState);

            if (bootstrapState.verticalStaticPending()) {
                changed |= VerticalSlabStonecuttingRecipes.prepareStaticDatapack();
                bootstrapState = bootstrapState.withPackPending(VerticalSlabStonecuttingRecipes.staticPackName(), false);
            }
            if (bootstrapState.stepStaticPending()) {
                changed |= StepStonecuttingRecipes.prepareStaticDatapack();
                bootstrapState = bootstrapState.withPackPending(StepStonecuttingRecipes.staticPackName(), false);
            }
            changed |= NeoForgeGeneratedServerDataPacks.enableReusablePacks(
                    packRepository,
                    bootstrapState,
                    Set.of(
                            VerticalSlabStonecuttingRecipes.staticPackName(),
                            VerticalSlabStonecuttingRecipes.mirrorPackName(),
                            StepStonecuttingRecipes.staticPackName(),
                            StepStonecuttingRecipes.mirrorPackName()
                    )
            );

            changed |= syncWorldPack(
                    datapacksRoot,
                    AlternativeRecipes.packName(),
                    AlternativeRecipes.packMcmeta(),
                    AlternativeRecipes.anyRecipesEnabledForBootstrap(),
                    AlternativeRecipes.buildDatapackFilesForBootstrap(),
                    AlternativeRecipes.inputFingerprintForBootstrap(),
                    desiredSelections
            );
            changed |= syncWorldPack(
                    datapacksRoot,
                    WoodStonecuttingRecipes.packName(),
                    WoodStonecuttingRecipes.packMcmeta(),
                    WoodStonecuttingRecipes.anyRecipesEnabledForBootstrap(),
                    WoodStonecuttingRecipes.buildDatapackFilesForBootstrap(),
                    WoodStonecuttingRecipes.inputFingerprintForBootstrap(),
                    desiredSelections
            );
            changed |= disableWorldPack(datapacksRoot, "bitsandbalance_vertical_slabs", desiredSelections);
            changed |= disableWorldPack(datapacksRoot, "bitsandbalance_steps", desiredSelections);
            changed |= disableWorldPack(datapacksRoot, VerticalSlabStonecuttingRecipes.staticPackName(), desiredSelections);
            changed |= disableWorldPack(datapacksRoot, VerticalSlabStonecuttingRecipes.mirrorPackName(), desiredSelections);
            changed |= disableWorldPack(datapacksRoot, StepStonecuttingRecipes.staticPackName(), desiredSelections);
            changed |= disableWorldPack(datapacksRoot, StepStonecuttingRecipes.mirrorPackName(), desiredSelections);
            changed |= applySelections(packRepository, desiredSelections);

            NeoForgeGeneratedServerDataPacks.storeState(worldRoot, bootstrapState);

            PREPARED_WORLDS.put(worldRoot, new PreparedWorldState(changed));
            LOGGER.info("[{}] Prepared integrated-world datapacks before load: selected={}, changed={}",
                    BitsAndBalance.MODID,
                    describeSelectedWorldPacks(packRepository),
                    changed);
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to prepare integrated-world datapacks before server load: {}",
                    BitsAndBalance.MODID,
                    e.toString());
        }
    }

    public static boolean isPreparedWorld(Path worldRoot) {
        return PREPARED_WORLDS.containsKey(normalize(worldRoot));
    }

    public static PreparedWorldConsumption consumePreparedWorld(Path worldRoot) {
        PreparedWorldState state = PREPARED_WORLDS.remove(normalize(worldRoot));
        return state == null ? new PreparedWorldConsumption(false, false) : new PreparedWorldConsumption(true, state.changed());
    }

    private static boolean disableWorldPack(Path datapacksRoot, String packName, Map<String, Boolean> desiredSelections) throws IOException {
        desiredSelections.put(packName, false);
        return deletePack(datapacksRoot.resolve(packName));
    }

    private static boolean syncWorldPack(
            Path datapacksRoot,
            String packName,
            String packMcmeta,
            boolean enabled,
            Map<String, String> files,
            String inputFingerprint,
            Map<String, Boolean> desiredSelections
    ) throws IOException {
        Path packPath = datapacksRoot.resolve(packName);
        boolean shouldExist = enabled && !files.isEmpty();
        desiredSelections.put(packName, shouldExist);

        if (!shouldExist) {
            return deletePack(packPath);
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(packPath, packMcmeta, files, inputFingerprint);
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    private static boolean applySelections(PackRepository packRepository, Map<String, Boolean> desiredSelections) {
        packRepository.reload();

        List<String> selectedIds = new ArrayList<>(packRepository.getSelectedPacks().stream().map(Pack::getId).toList());
        boolean changed = false;

        for (Map.Entry<String, Boolean> entry : desiredSelections.entrySet()) {
            String packId = "file/" + entry.getKey();
            boolean selected = selectedIds.contains(packId);
            if (entry.getValue()) {
                boolean available = packRepository.getAvailablePacks().stream().anyMatch(pack -> pack.getId().equals(packId));
                if (available && !selected) {
                    selectedIds.add(packId);
                    changed = true;
                }
            } else if (selectedIds.remove(packId)) {
                changed = true;
            }
        }

        if (changed) {
            packRepository.setSelected(selectedIds);
        }

        return changed;
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

    private static List<String> describeSelectedWorldPacks(PackRepository packRepository) {
        return packRepository.getSelectedPacks().stream()
                .map(Pack::getId)
                .filter(id -> id != null && id.startsWith("file/bitsandbalance_"))
                .toList();
    }

    private static Path normalize(Path worldRoot) {
        return worldRoot.toAbsolutePath().normalize();
    }

    public record PreparedWorldConsumption(boolean prepared, boolean changed) {
    }

    private record PreparedWorldState(boolean changed) {
    }
}