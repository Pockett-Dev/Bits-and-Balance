package org.onenonly.bitsandbalance.fabric.client.renderer;

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

import java.util.LinkedHashMap;
import java.util.Map;

public final class PistonMovingBlockEntityRenderHelper {
    private static final int RENDER_STATE_CACHE_LIMIT = 256;
    private static final Object RENDER_STATE_CACHE_LOCK = new Object();
    private static final Map<CacheKey, BlockEntityRenderState> RENDER_STATE_CACHE = new LinkedHashMap<>(RENDER_STATE_CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, BlockEntityRenderState> eldest) {
            return size() > RENDER_STATE_CACHE_LIMIT;
        }
    };

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

        PistonBlockEntityRenderCache.Entry entry = bitsandbalance$resolveEntry(renderPos, destPos);
        if (entry == null || entry.tag() == null || entry.tag().isEmpty()) {
            return false;
        }

        BlockEntityRenderDispatcher dispatcher = minecraft.getBlockEntityRenderDispatcher();
        BlockEntityRenderState renderState = bitsandbalance$getOrCreateRenderState(
                entityBlock,
                movedState,
                destPos,
                level,
                dispatcher,
                entry
        );
        if (renderState == null) {
            return false;
        }

        dispatcher.submit(renderState, poseStack, collector, cameraRenderState);
        return true;
    }

    private static @Nullable BlockEntityRenderState bitsandbalance$getOrCreateRenderState(EntityBlock entityBlock,
                                                                                           BlockState movedState,
                                                                                           BlockPos destPos,
                                                                                           Level level,
                                                                                           BlockEntityRenderDispatcher dispatcher,
                                                                                           PistonBlockEntityRenderCache.Entry entry) {
        CacheKey key = new CacheKey(level, movedState, entry.sourcePosLong(), entry.destPosLong(), entry.tag());
        synchronized (RENDER_STATE_CACHE_LOCK) {
            BlockEntityRenderState cached = RENDER_STATE_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(destPos, movedState);
        if (blockEntity == null) {
            return null;
        }

        blockEntity.setLevel(level);
        blockEntity.clearRemoved();
        blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entry.tag().copy()));

        BlockEntityRenderState renderState = dispatcher.tryExtractRenderState(blockEntity, 0.0F, null);
        if (renderState == null) {
            return null;
        }

        synchronized (RENDER_STATE_CACHE_LOCK) {
            RENDER_STATE_CACHE.put(key, renderState);
        }
        return renderState;
    }

    private static @Nullable PistonBlockEntityRenderCache.Entry bitsandbalance$resolveEntry(BlockPos renderPos, BlockPos destPos) {
        return PistonBlockEntityRenderCache.peekForMove(destPos.asLong(), renderPos.asLong());
    }

    private record CacheKey(Level level, BlockState state, long sourcePosLong, long destPosLong, CompoundTag tag) {
    }
}
