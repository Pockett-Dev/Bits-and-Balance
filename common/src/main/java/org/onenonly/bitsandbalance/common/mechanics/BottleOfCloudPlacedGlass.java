package org.onenonly.bitsandbalance.common.mechanics;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.WeakHashMap;
import java.util.UUID;

public final class BottleOfCloudPlacedGlass {
    public static final int FADE_SENTINEL_RGB = 0x00FFFFFE;
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-bottle-cloud-fade");

    private static final WeakHashMap<ServerLevel, State> STATES = new WeakHashMap<>();

    private BottleOfCloudPlacedGlass() {
    }

    public static void track(ServerLevel level, BlockPos pos, int durationTicks, int fadeLastTicks) {
        track(level, pos, durationTicks, fadeLastTicks, null);
    }

    public static void track(ServerLevel level, BlockPos pos, int durationTicks, int fadeLastTicks, UUID displayId) {
        if (durationTicks <= 0) {
            return;
        }

        long now = level.getGameTime();
        long expiresAt = now + durationTicks;
        long fadeStartsAt = expiresAt - Math.max(0, Math.min(fadeLastTicks, durationTicks));

        State state = STATES.computeIfAbsent(level, ignored -> new State());
        long key = pos.asLong();
        state.expiresAt.put(key, expiresAt);
        state.fadeStartsAt.put(key, fadeStartsAt);
        if (displayId != null) {
            state.displayByPos.put(key, displayId);
        }
    }

    public static void cleanup(ServerLevel level, BlockPos pos) {
        State state = STATES.get(level);
        if (state == null) {
            cleanupNearbyDisplays(level, pos);
            return;
        }

        long key = pos.asLong();
        UUID displayId = state.displayByPos.remove(key);
        state.expiresAt.remove(key);
        state.fadeStartsAt.remove(key);
        state.lastDebugAlphaBucket.remove(key);

        if (displayId == null) {
            cleanupNearbyDisplays(level, pos);
            return;
        }

        Entity ent = level.getEntity(displayId);
        if (ent != null) {
            ent.discard();
        }
        cleanupNearbyDisplays(level, pos);
    }

    public static void cleanupNearbyDisplays(ServerLevel level, BlockPos pos) {
        State state = STATES.get(level);
        if (state != null) {
            state.displayByPos.remove(pos.asLong());
        }

        AABB searchBox = new AABB(
            pos.getX() - 0.5D,
            pos.getY() - 0.5D,
            pos.getZ() - 0.5D,
            pos.getX() + 1.5D,
            pos.getY() + 2.5D,
            pos.getZ() + 1.5D
        );

        for (Display.BlockDisplay display : level.getEntitiesOfClass(Display.BlockDisplay.class, searchBox, display -> true)) {
            display.discard();
        }
    }

    public static void tick(ServerLevel level) {
        State state = STATES.get(level);
        if (state == null || state.expiresAt.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        LongSet toRemove = new LongOpenHashSet();

        LongIterator it = state.expiresAt.keySet().iterator();
        while (it.hasNext()) {
            long posLong = it.nextLong();
            long expiresAt = state.expiresAt.get(posLong);
            long fadeStartsAt = state.fadeStartsAt.get(posLong);

            BlockPos pos = BlockPos.of(posLong);
            UUID displayId = state.displayByPos.get(posLong);

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            if (now >= expiresAt) {
                var stateAtPos = level.getBlockState(pos);
                if (stateAtPos.getBlock() == ModBottleOfCloud.cloudBlock() && stateAtPos.getValue(CloudBlock.TEMPORARY)) {
                    level.removeBlock(pos, false);
                }
                if (displayId != null) {
                    Entity ent = level.getEntity(displayId);
                    if (ent != null) {
                        ent.discard();
                    }
                }
                toRemove.add(posLong);
                continue;
            }

            var stateAtPos = level.getBlockState(pos);
            if (stateAtPos.getBlock() != ModBottleOfCloud.cloudBlock()) {
                if (displayId != null) {
                    Entity ent = level.getEntity(displayId);
                    if (ent != null) {
                        ent.discard();
                    }
                }
                toRemove.add(posLong);
                continue;
            }

            boolean inFade = now >= fadeStartsAt;

            if (!stateAtPos.getValue(CloudBlock.TEMPORARY)) {
                level.setBlock(pos, stateAtPos.setValue(CloudBlock.TEMPORARY, Boolean.TRUE), 3);
                stateAtPos = level.getBlockState(pos);
            }

            if (displayId != null) {
                Entity ent = level.getEntity(displayId);
                if (ent != null) {
                    ent.discard();
                }
                state.displayByPos.remove(posLong);
                displayId = null;
            }

            int alphaByte;
            if (inFade) {
                float fadeTotal = Math.max(1.0F, (float) (expiresAt - fadeStartsAt));
                float t = (float) (now - fadeStartsAt) / fadeTotal;
                if (t < 0.0F) t = 0.0F;
                if (t > 1.0F) t = 1.0F;
                alphaByte = Math.round((1.0F - t) * 255.0F);
                if (alphaByte < 0) alphaByte = 0;
            } else {
                alphaByte = 255;
            }

            if (SharedConstants.IS_RUNNING_IN_IDE) {
                int bucket = alphaByte / 16;
                int previousBucket = state.lastDebugAlphaBucket.get(posLong);
                if (previousBucket != bucket) {
                    LOGGER.info(
                        "Bottle o' Cloud fade server probe pos={} alpha={} bucket={} now={} fadeStart={} expires={} display={} inFade={}",
                        pos,
                        alphaByte,
                        bucket,
                        now,
                        fadeStartsAt,
                        expiresAt,
                        displayId,
                        inFade
                    );
                    state.lastDebugAlphaBucket.put(posLong, bucket);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            LongIterator rit = toRemove.iterator();
            while (rit.hasNext()) {
                long key = rit.nextLong();
                state.expiresAt.remove(key);
                state.fadeStartsAt.remove(key);
                state.displayByPos.remove(key);
                state.lastDebugAlphaBucket.remove(key);
            }
        }
    }

    private static final class State {
        final Long2LongOpenHashMap expiresAt = new Long2LongOpenHashMap();
        final Long2LongOpenHashMap fadeStartsAt = new Long2LongOpenHashMap();
        final Long2ObjectOpenHashMap<UUID> displayByPos = new Long2ObjectOpenHashMap<>();
        final Long2IntOpenHashMap lastDebugAlphaBucket = new Long2IntOpenHashMap();

        State() {
            expiresAt.defaultReturnValue(Long.MIN_VALUE);
            fadeStartsAt.defaultReturnValue(Long.MIN_VALUE);
            lastDebugAlphaBucket.defaultReturnValue(-1);
        }
    }

}
