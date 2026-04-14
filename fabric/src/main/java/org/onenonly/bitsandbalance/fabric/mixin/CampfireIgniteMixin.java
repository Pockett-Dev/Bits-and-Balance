package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class CampfireIgniteMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void bitsandbalance$igniteOnCampfire(CallbackInfo ci) {
        if (!FabricTweaksConfig.enableCampfiresIgniteEntities) return;

        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;
        if (self.fireImmune()) return;
        if (self.hasEffect(MobEffects.FIRE_RESISTANCE)) return;

        BlockPos feet = self.blockPosition();
        if (isLitCampfire(level.getBlockState(feet)) || isLitCampfire(level.getBlockState(feet.below()))) {
            try {
                int current = self.getRemainingFireTicks();
                int next = Math.max(current, 20);
                self.setRemainingFireTicks(next);
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean isLitCampfire(BlockState state) {
        if (!(state.getBlock() instanceof CampfireBlock)) return false;
        try {
            return state.getValue(CampfireBlock.LIT);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
