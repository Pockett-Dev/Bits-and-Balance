package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveHooks;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveRuntime;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityRenderCache;

public final class PistonMovingBlockEntityRenderHelper {
    private PistonMovingBlockEntityRenderHelper() {
    }

    public static boolean handles(@Nullable BlockState state) {
        return PistonBlockEntityMoveRuntime.enabled
                && state != null
                && PistonBlockEntityMoveHooks.supports(state)
                && state.getBlock() instanceof EntityBlock;
    }

    public static boolean submitMoving(BlockPos renderPos,
                                       BlockPos destPos,
                                       BlockState movedState,
                                       PoseStack poseStack,
                                       SubmitNodeCollector collector,
                                       net.minecraft.client.renderer.state.CameraRenderState cameraRenderState) {
        if (!handles(movedState) || !(movedState.getBlock() instanceof EntityBlock entityBlock)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return false;
        }

        CompoundTag tag = bitsandbalance$resolveTag(renderPos, destPos);
        if (tag == null || tag.isEmpty()) {
            return false;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(destPos, movedState);
        if (blockEntity == null) {
            return false;
        }

        blockEntity.setLevel(level);
        blockEntity.clearRemoved();
        blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag.copy()));

        BlockEntityRenderDispatcher dispatcher = minecraft.getBlockEntityRenderDispatcher();
        BlockEntityRenderState renderState = dispatcher.tryExtractRenderState(blockEntity, 0.0F, null);
        if (renderState == null) {
            return false;
        }

        dispatcher.submit(renderState, poseStack, collector, cameraRenderState);
        return true;
    }

    private static @Nullable CompoundTag bitsandbalance$resolveTag(BlockPos renderPos, BlockPos destPos) {
        PistonBlockEntityRenderCache.Entry entry = PistonBlockEntityRenderCache.peekForMove(destPos.asLong(), renderPos.asLong());
        return entry == null ? null : entry.tag();
    }
}