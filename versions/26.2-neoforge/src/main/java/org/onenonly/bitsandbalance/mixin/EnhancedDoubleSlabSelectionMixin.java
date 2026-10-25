package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla double slabs outline/target as two halves.
 * This fixes the "two same slabs are bound as one block" selection/hitbox feel.
 */
@Mixin(SlabBlock.class)
public abstract class EnhancedDoubleSlabSelectionMixin {

    private static final VoxelShape BOTTOM_HALF_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final VoxelShape TOP_HALF_SHAPE = Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0);

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$doubleSlabHalfOutline(BlockState state, BlockGetter level, BlockPos pos,
                                                     CollisionContext context,
                                                     CallbackInfoReturnable<VoxelShape> cir) {
        if (!Config.enableEnhancedSlabs) return;
        if (!Config.enhancedSlabsKneeSlabMining) return;
        if (state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE) return;

        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player) {
            if (!player.isShiftKeyDown()) return; // Only trigger when crouching
            SlabType half = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
            cir.setReturnValue(half == SlabType.TOP ? TOP_HALF_SHAPE : BOTTOM_HALF_SHAPE);
        }
    }
}
