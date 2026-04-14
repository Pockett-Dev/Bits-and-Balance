package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.tweaks.NeoForgeIntegratedServerDatapackBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftDoWorldLoadDynamicDatapacksMixin {
    @Inject(method = "doWorldLoad", at = @At("HEAD"), require = 0)
    private void bitsandbalance$prepareDynamicDatapacks(
            LevelStorageSource.LevelStorageAccess levelStorage,
            PackRepository packRepository,
            WorldStem worldStem,
            boolean newWorld,
            CallbackInfo ci
    ) {
        try {
            NeoForgeIntegratedServerDatapackBootstrap.prepare(levelStorage, packRepository, worldStem);
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("[{}] Failed to prepare NeoForge world datapacks before world load: {}",
                    BitsAndBalance.MODID,
                    e.toString());
        }
    }
}
