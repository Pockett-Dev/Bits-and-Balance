package org.onenonly.bitsandbalance.fabric.mixin.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
        targets = {
                "com.simibubi.create.content.contraptions.ContraptionCollider",
                "com.zurrtum.create.content.contraptions.ContraptionCollider"
        },
        remap = false
)
public abstract class CreateContraptionColliderEnhancedSlabCollisionMixin {
    // Create-Fly ships in intermediary names in normal Fabric runtimes, while dev runs use named mappings.
    @Redirect(
            method = "getPotentiallyCollidedShapes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
            ),
            remap = false,
            require = 0
    )
    private static VoxelShape bitsandbalance$useStoredQuadCollisionShapeNamed(BlockState state,
                                                                              BlockGetter blockGetter,
                                                                              BlockPos pos,
                                                                              Level level,
                                                                              @Coerce Object contraption,
                                                                              AABB bounds,
                                                                              Shapes.DoubleLineConsumer consumer) {
        return bitsandbalance$useStoredQuadCollisionShape(state, blockGetter, pos, contraption);
    }

    @Redirect(
            method = "getPotentiallyCollidedShapes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_2680;method_26220(Lnet/minecraft/class_1922;Lnet/minecraft/class_2338;)Lnet/minecraft/class_265;"
            ),
            remap = false,
            require = 0
    )
    private static VoxelShape bitsandbalance$useStoredQuadCollisionShapeIntermediary(BlockState state,
                                                                                     BlockGetter blockGetter,
                                                                                     BlockPos pos,
                                                                                     Level level,
                                                                                     @Coerce Object contraption,
                                                                                     AABB bounds,
                                                                                     Shapes.DoubleLineConsumer consumer) {
        return bitsandbalance$useStoredQuadCollisionShape(state, blockGetter, pos, contraption);
    }

    @Unique
    private static VoxelShape bitsandbalance$useStoredQuadCollisionShape(BlockState state,
                                                                         BlockGetter blockGetter,
                                                                         BlockPos pos,
                                                                         Object contraption) {
        VoxelShape contraptionShape = EnhancedSlabCreateCompat.getContraptionCollisionShape(contraption, blockGetter, pos, state);
        return contraptionShape.isEmpty() ? state.getCollisionShape(blockGetter, pos) : contraptionShape;
    }
}
