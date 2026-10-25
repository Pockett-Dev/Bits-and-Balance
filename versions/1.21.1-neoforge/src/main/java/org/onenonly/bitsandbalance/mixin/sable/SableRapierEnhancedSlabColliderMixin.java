package org.onenonly.bitsandbalance.mixin.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
public abstract class SableRapierEnhancedSlabColliderMixin {
    @Shadow(remap = false)
    @Final
    private ServerLevel level;

    @Coerce
    @WrapOperation(
            method = "handleChunkSectionAddition",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/physics/impl/rapier/collider/RapierVoxelColliderBakery;getPhysicsDataForBlock(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/physics/impl/rapier/collider/RapierVoxelColliderData;"
            ),
            require = 0
    )
    private Object bitsandbalance$usePositionColliderDuringSectionUpload(@Coerce Object bakery,
                                                                         BlockState state,
                                                                         Operation<Object> original,
                                                                         @Local(index = 12) BlockPos pos) {
        Object fallback = original.call(bakery, state);
        return EnhancedSlabCreateCompat.createSableRapierCollider(bakery, fallback, this.level, pos, state);
    }

    @Coerce
    @WrapOperation(
            method = "handleBlockChange",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/physics/impl/rapier/collider/RapierVoxelColliderBakery;getPhysicsDataForBlock(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/physics/impl/rapier/collider/RapierVoxelColliderData;",
                    ordinal = 0
            ),
            require = 0
    )
    private Object bitsandbalance$usePositionColliderForChangedNeighbor(@Coerce Object bakery,
                                                                        BlockState state,
                                                                        Operation<Object> original,
                                                                        @Local(index = 13) BlockPos pos) {
        Object fallback = original.call(bakery, state);
        return EnhancedSlabCreateCompat.createSableRapierCollider(bakery, fallback, this.level, pos, state);
    }

    @Coerce
    @WrapOperation(
            method = "handleBlockChange",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/physics/impl/rapier/collider/RapierVoxelColliderBakery;getPhysicsDataForBlock(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/physics/impl/rapier/collider/RapierVoxelColliderData;",
                    ordinal = 1
            ),
            require = 0
    )
    private Object bitsandbalance$usePositionColliderForChangedBlock(@Coerce Object bakery,
                                                                     BlockState state,
                                                                     Operation<Object> original,
                                                                     @Local(index = 8) BlockPos pos) {
        Object fallback = original.call(bakery, state);
        return EnhancedSlabCreateCompat.createSableRapierCollider(bakery, fallback, this.level, pos, state);
    }
}
