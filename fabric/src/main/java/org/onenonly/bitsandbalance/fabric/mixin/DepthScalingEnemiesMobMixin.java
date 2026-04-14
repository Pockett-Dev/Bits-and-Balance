package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.onenonly.bitsandbalance.fabric.mobs.FabricDepthScalingEnemies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class DepthScalingEnemiesMobMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void bitsandbalance$applyDepthScalingEnemies(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        ServerLevel serverLevel = level.getLevel();
        Mob self = (Mob) (Object) this;
        FabricDepthScalingEnemies.applyDepthScalingForSpawn(self, serverLevel);
    }
}