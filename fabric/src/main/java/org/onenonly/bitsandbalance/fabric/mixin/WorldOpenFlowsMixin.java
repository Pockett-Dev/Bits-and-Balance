package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {

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
        if (!FabricClientConfig.suppressExperimentalSettingsWarning) {
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
        if (!FabricClientConfig.suppressExperimentalSettingsWarning) {
            return;
        }

        boolean nonStableLifecycle = bitsandbalance$getWorldGenLifecycle(worldStem) != Lifecycle.stable();

        if (nonStableLifecycle) {
            this.openWorldLoadBundledResourcePack(levelStorage, worldStem, packRepository, onFail);
            ci.cancel();
        }
    }

    private static Lifecycle bitsandbalance$getWorldGenLifecycle(WorldStem worldStem) {
        if (worldStem == null) {
            return Lifecycle.stable();
        }

        try {
            Method worldDataAndGenSettings = worldStem.getClass().getMethod("worldDataAndGenSettings");
            Object combined = worldDataAndGenSettings.invoke(worldStem);
            if (combined != null) {
                Method dataMethod = combined.getClass().getMethod("data");
                Object worldData = dataMethod.invoke(combined);
                Lifecycle lifecycle = bitsandbalance$invokeLifecycle(worldData);
                if (lifecycle != null) {
                    return lifecycle;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method worldDataMethod = worldStem.getClass().getMethod("worldData");
            Object worldData = worldDataMethod.invoke(worldStem);
            Lifecycle lifecycle = bitsandbalance$invokeLifecycle(worldData);
            if (lifecycle != null) {
                return lifecycle;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return Lifecycle.stable();
    }

    private static Lifecycle bitsandbalance$invokeLifecycle(Object worldData) {
        if (worldData == null) {
            return null;
        }

        try {
            Method lifecycleMethod = worldData.getClass().getMethod("worldGenSettingsLifecycle");
            Object lifecycle = lifecycleMethod.invoke(worldData);
            if (lifecycle instanceof Lifecycle typedLifecycle) {
                return typedLifecycle;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }
}
