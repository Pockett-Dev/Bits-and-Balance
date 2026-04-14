package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.onenonly.bitsandbalance.fabric.recipe.FabricIntegratedServerDatapackBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Minecraft.class)
public class MinecraftDoWorldLoadDynamicDatapacksMixin {
    @Inject(method = "doWorldLoad", at = @At("HEAD"), require = 0)
    private void bitsandbalance$prepareDynamicDatapacks(
            LevelStorageSource.LevelStorageAccess levelStorage,
            PackRepository packRepository,
            WorldStem worldStem,
            Optional<?> loadingMessage,
            boolean newWorld,
            CallbackInfo ci
    ) {
        FabricIntegratedServerDatapackBootstrap.prepare(levelStorage, packRepository, worldStem);
    }
}