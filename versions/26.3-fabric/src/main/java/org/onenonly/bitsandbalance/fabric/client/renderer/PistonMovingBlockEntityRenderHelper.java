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

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PistonMovingBlockEntityRenderHelper {
    private static final int RENDER_STATE_CACHE_LIMIT = 256;
    private static final BoundedConcurrentCache<CacheKey, BlockEntityRenderState> RENDER_STATE_CACHE =
            new BoundedConcurrentCache<>(RENDER_STATE_CACHE_LIMIT);
    private static volatile WeakReference<Level> cacheLevel = new WeakReference<>(null);

    private PistonMovingBlockEntityRenderHelper() {
    }

    public static void clearCaches() {
        cacheLevel = new WeakReference<>(null);
        RENDER_STATE_CACHE.clear();
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
                                       net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        if (!handles(movedState) || !(movedState.getBlock() instanceof EntityBlock entityBlock)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return false;
        }
        if (cacheLevel.get() != level) {
            cacheLevel = new WeakReference<>(level);
            RENDER_STATE_CACHE.clear();
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
        BlockEntityRenderState cached = RENDER_STATE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(destPos, movedState);
        if (blockEntity == null) {
            return null;
        }
        blockEntity.setLevel(level);
        blockEntity.clearRemoved();
        blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entry.tag().copy()));

        BlockEntityRenderState renderState = dispatcher.tryExtractRenderState(blockEntity, 0.0F, null, false);
        if (renderState == null) {
            return null;
        }

        BlockEntityRenderState raced = RENDER_STATE_CACHE.putIfAbsent(key, renderState);
        return raced != null ? raced : renderState;
    }

    private static @Nullable PistonBlockEntityRenderCache.Entry bitsandbalance$resolveEntry(BlockPos renderPos, BlockPos destPos) {
        return PistonBlockEntityRenderCache.peekForMove(destPos.asLong(), renderPos.asLong());
    }

    private static final class CacheKey {
        private final WeakReference<Level> level;
        private final int levelIdentityHash;
        private final BlockState state;
        private final long sourcePosLong;
        private final long destPosLong;
        private final CompoundTag tag;

        private CacheKey(Level level, BlockState state, long sourcePosLong, long destPosLong, CompoundTag tag) {
            this.level = new WeakReference<>(level);
            this.levelIdentityHash = System.identityHashCode(level);
            this.state = state;
            this.sourcePosLong = sourcePosLong;
            this.destPosLong = destPosLong;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof CacheKey that)) {
                return false;
            }
            Level thisLevel = level.get();
            return thisLevel != null
                    && thisLevel == that.level.get()
                    && state.equals(that.state)
                    && sourcePosLong == that.sourcePosLong
                    && destPosLong == that.destPosLong
                    && tag.equals(that.tag);
        }

        @Override
        public int hashCode() {
            int result = levelIdentityHash;
            result = 31 * result + state.hashCode();
            result = 31 * result + Long.hashCode(sourcePosLong);
            result = 31 * result + Long.hashCode(destPosLong);
            return 31 * result + tag.hashCode();
        }
    }

    private static final class BoundedConcurrentCache<K, V> {
        private final int limit;
        private final ConcurrentHashMap<K, V> values = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<K> insertionOrder = new ConcurrentLinkedQueue<>();

        private BoundedConcurrentCache(int limit) {
            this.limit = limit;
        }

        private V get(K key) {
            return values.get(key);
        }

        private V putIfAbsent(K key, V value) {
            V existing = values.putIfAbsent(key, value);
            if (existing != null) {
                return existing;
            }
            insertionOrder.offer(key);
            while (values.size() > limit) {
                K eldest = insertionOrder.poll();
                if (eldest == null) {
                    break;
                }
                values.remove(eldest);
            }
            return null;
        }

        private void clear() {
            values.clear();
            insertionOrder.clear();
        }
    }
}
