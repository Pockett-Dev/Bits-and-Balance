package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Mobs: Spawn Swap
 * Replaces NATURAL overworld cave spider spawns underground.
 */
@Mixin(Spider.class)
public abstract class SpawnSwapSpiderMixin {

    @Unique
    private boolean bitsandbalance$spawnSwapPending;

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void bitsandbalance$spawnSwapCaveSpider(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!FabricMobsConfig.enableCaveSpidersInCaves) return;
        if (spawnReason != EntitySpawnReason.NATURAL) return;

        ServerLevel serverLevel = level.getLevel();
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return;

        Spider self = (Spider) (Object) this;
        BlockPos pos = self.blockPosition();
        if (serverLevel.canSeeSkyFromBelowWater(pos)) return;

        if (serverLevel.getRandom().nextDouble() >= FabricMobsConfig.caveSpiderReplacementChance) return;

        bitsandbalance$spawnSwapPending = true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void bitsandbalance$applySpawnSwap(CallbackInfo ci) {
        if (!bitsandbalance$spawnSwapPending) return;

        Spider self = (Spider) (Object) this;
        if (self.level().isClientSide()) return;

        // Only swap immediately after spawn.
        if (self.tickCount > 1) {
            bitsandbalance$spawnSwapPending = false;
            return;
        }

        if (!(self.level() instanceof ServerLevel serverLevel)) {
            bitsandbalance$spawnSwapPending = false;
            return;
        }

        CaveSpider caveSpider = EntityType.CAVE_SPIDER.create(serverLevel, EntitySpawnReason.NATURAL);
        if (caveSpider == null) {
            bitsandbalance$spawnSwapPending = false;
            return;
        }

        caveSpider.teleportTo(serverLevel, self.getX(), self.getY(), self.getZ(), Set.of(), self.getYRot(), self.getXRot(), true);
        serverLevel.addFreshEntity(caveSpider);
        bitsandbalance$spawnSwapPending = false;
        self.discard();
    }
}
