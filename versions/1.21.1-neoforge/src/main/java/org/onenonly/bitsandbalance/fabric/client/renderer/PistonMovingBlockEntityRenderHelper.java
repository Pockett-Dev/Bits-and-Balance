package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveHooks;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveRuntime;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityRenderCache;
import org.onenonly.bitsandbalance.common.mixin.PistonBlockEntityMovingAccess;

@SuppressWarnings("unchecked")
public final class PistonMovingBlockEntityRenderHelper {
    private PistonMovingBlockEntityRenderHelper() {
    }

    public static boolean handles(@Nullable BlockState state) {
        return PistonBlockEntityMoveRuntime.enabled
                && state != null
                && PistonBlockEntityMoveHooks.supports(state)
                && state.getBlock() instanceof EntityBlock;
    }

    public static boolean renderMoving(Level level,
                                       BlockPos renderPos,
                                       BlockPos destPos,
                                       BlockState movedState,
                                       Object movingBlockEntity,
                                       float partialTick,
                                       PoseStack poseStack,
                                       MultiBufferSource buffer,
                                       int packedLight,
                                       int packedOverlay) {
        if (!handles(movedState) || !(movedState.getBlock() instanceof EntityBlock entityBlock)) {
            return false;
        }

        CompoundTag tag = bitsandbalance$resolveTag(renderPos, destPos, movingBlockEntity);
        if (tag == null || tag.isEmpty()) {
            return false;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(destPos, movedState);
        if (blockEntity == null) {
            return false;
        }

        blockEntity.setLevel(level);
        blockEntity.clearRemoved();
        blockEntity.loadWithComponents(tag.copy(), level.registryAccess());

        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        BlockEntityRenderer<BlockEntity> renderer = (BlockEntityRenderer<BlockEntity>) dispatcher.getRenderer(blockEntity);
        if (renderer == null) {
            return false;
        }

        renderer.render(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
        return true;
    }

    private static @Nullable CompoundTag bitsandbalance$resolveTag(BlockPos renderPos,
                                                                   BlockPos destPos,
                                                                   Object movingBlockEntity) {
        if (movingBlockEntity instanceof PistonBlockEntityMovingAccess access) {
            CompoundTag directTag = access.bitsandbalance$getMovedBlockEntityTag();
            if (directTag != null && !directTag.isEmpty()) {
                return directTag;
            }
        }

        PistonBlockEntityRenderCache.Entry entry = PistonBlockEntityRenderCache.peekForMove(destPos.asLong(), renderPos.asLong());
        return entry == null ? null : entry.tag();
    }
}