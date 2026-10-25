package org.onenonly.bitsandbalance.mixin.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;

/** Uses live plot storage for Step-family centers of mass and full inertia tensors. */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.physics.mass.MassTracker", remap = false)
public abstract class SableMassTrackerPositionAwareQuadCenterMixin {
    /**
     * Sable samples buoyancy around the owning voxel rather than around the voxel collider boxes.
     * Keep the accurate geometric center for Steps in larger bodies, but center a body made from one
     * standalone Step on that voxel so the native buoyancy force cannot act through a permanent
     * quarter-block lever arm. Quad containers are deliberately excluded: their two-, three-, and
     * four-part balance remains position-aware.
     */
    @Unique
    private static final ThreadLocal<Boolean> bitsandbalance$loneVoxelBody =
            ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "build(Lnet/minecraft/world/level/BlockGetter;Ldev/ryanhcode/sable/companion/math/BoundingBox3ic;)Ldev/ryanhcode/sable/api/physics/mass/MassTracker;",
            at = @At("HEAD"),
            remap = false
    )
    private static void bitsandbalance$captureLoneVoxelBody(BlockGetter level,
                                                             @Coerce Object bounds,
                                                             CallbackInfoReturnable<Object> cir) {
        bitsandbalance$loneVoxelBody.set(bitsandbalance$isSingleVoxel(bounds));
    }

    @Inject(
            method = "build(Lnet/minecraft/world/level/BlockGetter;Ldev/ryanhcode/sable/companion/math/BoundingBox3ic;)Ldev/ryanhcode/sable/api/physics/mass/MassTracker;",
            at = @At("RETURN"),
            remap = false
    )
    private static void bitsandbalance$clearLoneVoxelBody(BlockGetter level,
                                                           @Coerce Object bounds,
                                                           CallbackInfoReturnable<Object> cir) {
        bitsandbalance$loneVoxelBody.remove();
    }

    @Unique
    private static boolean bitsandbalance$isSingleVoxel(Object bounds) {
        if (bounds == null) {
            return false;
        }
        try {
            int minX = ((Number) bounds.getClass().getMethod("minX").invoke(bounds)).intValue();
            int minY = ((Number) bounds.getClass().getMethod("minY").invoke(bounds)).intValue();
            int minZ = ((Number) bounds.getClass().getMethod("minZ").invoke(bounds)).intValue();
            int maxX = ((Number) bounds.getClass().getMethod("maxX").invoke(bounds)).intValue();
            int maxY = ((Number) bounds.getClass().getMethod("maxY").invoke(bounds)).intValue();
            int maxZ = ((Number) bounds.getClass().getMethod("maxZ").invoke(bounds)).intValue();
            return minX == maxX && minY == maxY && minZ == maxZ;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    @WrapOperation(
            method = "build(Lnet/minecraft/world/level/BlockGetter;Ldev/ryanhcode/sable/companion/math/BoundingBox3ic;)Ldev/ryanhcode/sable/api/physics/mass/MassTracker;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            ),
            require = 2,
            remap = false
    )
    private static Object bitsandbalance$usePositionAwareQuadCenter(BiFunction<?, ?, ?> centerFunction,
                                                                    @Coerce Object levelArgument,
                                                                    @Coerce Object stateArgument,
                                                                    Operation<Object> original,
                                                                    @Local(index = 6) BlockPos.MutableBlockPos pos) {
        if (levelArgument instanceof BlockGetter level && stateArgument instanceof BlockState state && pos != null) {
            if (Boolean.TRUE.equals(bitsandbalance$loneVoxelBody.get())
                    && (state.getBlock() instanceof StepBlock
                    || state.getBlock() instanceof VerticalStepBlock)) {
                return new Vector3d(0.5D, 0.5D, 0.5D);
            }
            Vector3dc center = EnhancedSlabCreateCompat.getSablePositionAwareQuadCenterOfMass(level, pos, state);
            if (center != null) {
                return center;
            }
        }
        return original.call(centerFunction, levelArgument, stateArgument);
    }

    @WrapOperation(
            method = "build(Lnet/minecraft/world/level/BlockGetter;Ldev/ryanhcode/sable/companion/math/BoundingBox3ic;)Ldev/ryanhcode/sable/api/physics/mass/MassTracker;",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/physics/mass/MassTracker;addBlockInertia(Lorg/joml/Vector3d;DLorg/joml/Matrix3d;Lnet/minecraft/world/phys/Vec3;)Lorg/joml/Matrix3d;"
            ),
            require = 1,
            remap = false
    )
    private static Matrix3d bitsandbalance$usePositionAwareStepInertia(Vector3d ownerToBodyCenterOffset,
                                                                       double ownerMass,
                                                                       Matrix3d accumulator,
                                                                       Vec3 configuredInertia,
                                                                       Operation<Matrix3d> original,
                                                                       @Local(argsOnly = true) BlockGetter level,
                                                                       @Local(index = 6) BlockPos.MutableBlockPos pos,
                                                                       @Local(index = 12) BlockState state) {
        // Explicit Sable block-property overrides remain authoritative. The geometric tensor only
        // replaces Sable's generic full-cube default when no inertia override was configured.
        if (configuredInertia == null && EnhancedSlabCreateCompat.addSablePositionAwareStepInertia(
                level,
                pos,
                state,
                ownerToBodyCenterOffset,
                ownerMass,
                accumulator
        )) {
            return accumulator;
        }
        return original.call(ownerToBodyCenterOffset, ownerMass, accumulator, configuredInertia);
    }
}
