package org.onenonly.bitsandbalance.mixin;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.onenonly.bitsandbalance.ClientConfig;
import org.onenonly.bitsandbalance.tweaks.NeoForgeIntegratedServerDatapackBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {
    @Unique
    private static final ThreadLocal<LevelStorageSource.LevelStorageAccess> bitsandbalance$levelStorageForStemLoad = new ThreadLocal<>();

    @ModifyVariable(method = "loadWorldStem", at = @At("HEAD"), argsOnly = true, require = 0)
    private Dynamic<?> bitsandbalance$prepareGeneratedPackSelections(
            Dynamic<?> dataTag,
            Dynamic<?> originalDataTag,
            boolean safeMode,
            PackRepository packRepository
    ) {
        LevelStorageSource.LevelStorageAccess levelStorage = bitsandbalance$levelStorageForStemLoad.get();
        bitsandbalance$levelStorageForStemLoad.remove();
        if (levelStorage == null) {
            return dataTag;
        }
        return NeoForgeIntegratedServerDatapackBootstrap.prepareBeforeWorldStemDataTag(levelStorage, packRepository, dataTag);
    }

    @Inject(
            method = "openWorldLoadLevelStem",
            at = @At("HEAD"),
            require = 0
    )
    private void bitsandbalance$rememberLevelStorageForStemLoad(
            LevelStorageSource.LevelStorageAccess levelStorage,
            com.mojang.serialization.Dynamic<?> dynamic,
            boolean safeMode,
            Runnable onFail,
            CallbackInfo ci
    ) {
        bitsandbalance$levelStorageForStemLoad.set(levelStorage);
    }

    @Shadow
    private void openWorldLoadBundledResourcePack(
            LevelStorageSource.LevelStorageAccess levelStorage,
            WorldStem worldStem,
            PackRepository packRepository,
            Runnable onFail
    ) {
        throw new AssertionError("mixin");
    }

    @Inject(
            method = "confirmWorldCreation",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void bitsandbalance$suppressExperimentalWorldCreationWarning(
            net.minecraft.client.Minecraft minecraft,
            net.minecraft.client.gui.screens.worldselection.CreateWorldScreen screen,
            Lifecycle lifecycle,
            Runnable loadWorld,
            boolean skipWarnings,
            CallbackInfo ci
    ) {
        if (!ClientConfig.suppressExperimentalSettingsWarning) {
            return;
        }

        if (skipWarnings || lifecycle == Lifecycle.stable()) {
            return;
        }

        if (lifecycle == Lifecycle.experimental()) {
            loadWorld.run();
            ci.cancel();
        }
    }

    @Inject(
            method = "openWorldCheckWorldStemCompatibility",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void bitsandbalance$skipExperimentalWorldBackupWarning(
            LevelStorageSource.LevelStorageAccess levelStorage,
            WorldStem worldStem,
            PackRepository packRepository,
            Runnable onFail,
            CallbackInfo ci
    ) {
        if (!ClientConfig.suppressExperimentalSettingsWarning) {
            return;
        }

        WorldData worldData = worldStem.worldData();
        boolean customized = worldData.worldGenOptions().isOldCustomizedWorld();
        boolean nonStableLifecycle = worldData.worldGenSettingsLifecycle() != Lifecycle.stable();

        if (!customized && worldData instanceof PrimaryLevelData pld) {
            pld.withConfirmedWarning(true);
        }

        if (customized || nonStableLifecycle) {
            this.openWorldLoadBundledResourcePack(levelStorage, worldStem, packRepository, onFail);
            ci.cancel();
        }
    }
}
