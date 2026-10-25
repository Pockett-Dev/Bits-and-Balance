package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allow faster climbable descent without losing vanilla friction by replacing the
 * vanilla downward clamp constant inside LivingEntity.handleOnClimbable when the player is
 * looking down on or near any climbable.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelTailMixin {

    @Shadow
    protected boolean jumping;

    @Inject(method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$modifyUpwardResult(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (!FabricTweaksConfig.enableFastClimbableAscend) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player p)) return;
        if (p.getAbilities().flying || p.isFallFlying()) return;

        float pitch = -p.getXRot();
        float minDeg = (float) FabricTweaksConfig.fastClimbableLookUpMinDeg;
        float maxDeg = (float) FabricTweaksConfig.fastClimbableLookUpMaxDeg;
        if (pitch < minDeg) return;

        Level level = p.level();
        BlockPos pos = p.blockPosition();
        if (!(p.onClimbable() || isNearClimbableTravel(level, pos))) return;
        if (!hasUpwardClimbIntent(movement)) return;

        Vec3 result = cir.getReturnValue();
        if (result.y <= 0.0D) return;

        double maxConfigured = Math.max(result.y, FabricTweaksConfig.fastClimbableAscendSpeed);
        double adjustedY = maxDeg <= minDeg
                ? maxConfigured
                : Mth.clampedMap(pitch, minDeg, maxDeg, (float) result.y, (float) maxConfigured);
        if (adjustedY != result.y) {
            cir.setReturnValue(new Vec3(result.x, adjustedY, result.z));
        }
    }

    private boolean hasUpwardClimbIntent(Vec3 movement) {
        return this.jumping || movement.horizontalDistanceSqr() > 1.0E-4D;
    }

    @ModifyConstant(method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", constant = @Constant(doubleValue = -0.15000000596046448D))
    private double bitsandbalance$modifyDownwardClamp(double original) {
        if (!FabricTweaksConfig.enableFastClimbableSlide) return original;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player p)) return original;
        if (p.getAbilities().flying || p.isFallFlying()) return original;

        float pitch = p.getXRot();
        float minDeg = (float) FabricTweaksConfig.fastClimbableLookDownMinDeg;
        float maxDeg = (float) FabricTweaksConfig.fastClimbableLookDownMaxDeg;
        if (pitch < minDeg) return original;

        Level level = p.level();
        BlockPos pos = p.blockPosition();
        if (!(p.onClimbable() || isNearClimbableTravel(level, pos))) return original;

        double maxConfigured = -Math.max(0.05D, FabricTweaksConfig.fastClimbableSlideSpeed);
        if (maxDeg <= minDeg) {
            return maxConfigured;
        }
        return Mth.clampedMap(pitch, minDeg, maxDeg, original, maxConfigured);
    }

    private static boolean isClimbableTravel(BlockState state) {
        return state.is(BlockTags.CLIMBABLE) || state.getBlock() instanceof LadderBlock;
    }

    private static boolean isNearClimbableTravel(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos[] positions = new BlockPos[]{
                pos, above,
                pos.north(), above.north(),
                pos.south(), above.south(),
                pos.east(), above.east(),
                pos.west(), above.west()
        };
        for (BlockPos p : positions) {
            if (isClimbableTravel(level.getBlockState(p))) return true;
        }
        return false;
    }
}
