package org.onenonly.bitsandbalance.common.mechanics;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceBlockDisplayAccess;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceDisplayAccess;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;

import java.util.WeakHashMap;
import java.util.UUID;

public final class BottleOfCloudPlacedGlass {
    /**
     * Sentinel RGB stored in the low 24 bits of the glow-color-override.
     * The alpha byte occupies the high 8 bits. Using 0xFFFFFE (not 0xFFFFFF)
     * avoids colliding with NO_GLOW_COLOR_OVERRIDE (-1 / 0xFFFFFFFF).
     */
    public static final int FADE_SENTINEL_RGB = 0x00FFFFFE;

    private static final WeakHashMap<ServerLevel, State> STATES = new WeakHashMap<>();

    private BottleOfCloudPlacedGlass() {
    }

    public static void track(ServerLevel level, BlockPos pos, int durationTicks, int fadeLastTicks) {
        track(level, pos, durationTicks, fadeLastTicks, null);
    }

    public static void track(ServerLevel level, BlockPos pos, int durationTicks, int fadeLastTicks, UUID displayId) {
        if (durationTicks <= 0) return;

        long now = level.getGameTime();
        long expiresAt = now + durationTicks;
        long fadeStartsAt = expiresAt - Math.max(0, Math.min(fadeLastTicks, durationTicks));

        State state = STATES.computeIfAbsent(level, k -> new State());
        long key = pos.asLong();
        state.expiresAt.put(key, expiresAt);
        state.fadeStartsAt.put(key, fadeStartsAt);
        if (displayId != null) {
            state.displayByPos.put(key, displayId);
        }
    }

    public static void tick(ServerLevel level) {
        State state = STATES.get(level);
        if (state == null || state.expiresAt.isEmpty()) return;

        long now = level.getGameTime();

        // Avoid mutating maps while iterating them.
        LongSet toRemove = new LongOpenHashSet();

        LongIterator it = state.expiresAt.keySet().iterator();
        while (it.hasNext()) {
            long posLong = it.nextLong();
            long expiresAt = state.expiresAt.get(posLong);
            long fadeStartsAt = state.fadeStartsAt.get(posLong);

            BlockPos pos = BlockPos.of(posLong);
            UUID displayId = state.displayByPos.get(posLong);

            // If the chunk isn't loaded, defer until it is.
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            if (now >= expiresAt) {
                // Expired: remove the block only if it is still our temporary one.
                var stateAtPos = level.getBlockState(pos);
                if (stateAtPos.is(ModBottleOfCloud.cloudBlock()) && stateAtPos.getValue(CloudBlock.TEMPORARY)) {
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

            // If the cloud block is gone or replaced by something else, stop tracking.
            var stateAtPos = level.getBlockState(pos);
            if (!stateAtPos.is(ModBottleOfCloud.cloudBlock())) {
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

            // The physical cloud block should remain invisible from placement time onward.
            // Rendering is always handled by the display entity so the fade window begins
            // with no visible swap/pop at all.
            if (!stateAtPos.getValue(CloudBlock.TEMPORARY)) {
                level.setBlock(pos, stateAtPos.setValue(CloudBlock.TEMPORARY, Boolean.TRUE), 3);
                stateAtPos = level.getBlockState(pos);
            }

            // Ensure there is a display entity to carry the rendering for the entire lifetime.
            if (displayId == null) {
                Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
                display.setPos(pos.getX(), pos.getY(), pos.getZ());
                ((BitsAndBalanceBlockDisplayAccess) (Object) display).bitsandbalance$setBlockState(ModBottleOfCloud.cloudBlock().defaultBlockState());
                ((BitsAndBalanceDisplayAccess) (Object) display).bitsandbalance$setGlowColorOverride((255 << 24) | FADE_SENTINEL_RGB);
                level.addFreshEntity(display);
                displayId = display.getUUID();
                state.displayByPos.put(posLong, displayId);
            }

            Entity ent = level.getEntity(displayId);
            if (!(ent instanceof Display display)) {
                toRemove.add(posLong);
                continue;
            }

            int alphaByte;
            if (inFade) {
                float fadeTotal = Math.max(1.0F, (float) (expiresAt - fadeStartsAt));
                float t = (float) (now - fadeStartsAt) / fadeTotal;
                if (t < 0.0F) t = 0.0F;
                if (t > 1.0F) t = 1.0F;

                // Fade from fully opaque (alpha 255) down to 0 over the fade window.
                alphaByte = Math.round((1.0F - t) * 255.0F);
                if (alphaByte < 0) alphaByte = 0;
            } else {
                // Stay fully opaque before the fade window so the Bottle o' Cloud
                // is visually identical to a normal cloud block.
                alphaByte = 255;
            }

            int argb = (alphaByte << 24) | FADE_SENTINEL_RGB;
            ((BitsAndBalanceDisplayAccess) (Object) display).bitsandbalance$setGlowColorOverride(argb);
        }

        if (!toRemove.isEmpty()) {
            LongIterator rit = toRemove.iterator();
            while (rit.hasNext()) {
                long key = rit.nextLong();
                state.expiresAt.remove(key);
                state.fadeStartsAt.remove(key);
                state.displayByPos.remove(key);
            }
        }
    }

    private static final class State {
        final Long2LongOpenHashMap expiresAt = new Long2LongOpenHashMap();
        final Long2LongOpenHashMap fadeStartsAt = new Long2LongOpenHashMap();
        final Long2ObjectOpenHashMap<UUID> displayByPos = new Long2ObjectOpenHashMap<>();

        State() {
            expiresAt.defaultReturnValue(Long.MIN_VALUE);
            fadeStartsAt.defaultReturnValue(Long.MIN_VALUE);
        }
    }
}
