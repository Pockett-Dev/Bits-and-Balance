package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudPlacedGlass;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class TemporaryCloudBlockEntity extends BlockEntity {
    private static final String TAG_PLACED_AT = "PlacedAt";
    private static final String TAG_DURATION_TICKS = "DurationTicks";
    private static final String TAG_FADE_LAST_TICKS = "FadeLastTicks";
    private static final Map<Level, Set<Long>> CLIENT_TRACKED_POSITIONS = Collections.synchronizedMap(new WeakHashMap<>());

    private long placedAtGameTime = Long.MIN_VALUE;
    private int durationTicks;
    private int fadeLastTicks;

    public TemporaryCloudBlockEntity(BlockPos pos, BlockState blockState) {
        super(CommonBlockEntities.TEMPORARY_CLOUD, pos, blockState);
    }

    public void configureFade(long now, int durationTicks, int fadeLastTicks) {
        this.placedAtGameTime = now;
        this.durationTicks = Math.max(1, durationTicks);
        this.fadeLastTicks = Math.max(0, Math.min(fadeLastTicks, this.durationTicks));

        setChanged();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        serverLevel.getChunkSource().blockChanged(worldPosition);

        Packet<ClientGamePacketListener> packet = getUpdatePacket();
        if (packet != null) {
            for (ServerPlayer player : serverLevel.players()) {
                try {
                    player.connection.send(packet);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public boolean hasActiveFade() {
        return placedAtGameTime != Long.MIN_VALUE && durationTicks > 0;
    }

    public int getRenderAlpha(float partialTick) {
        if (!getBlockState().hasProperty(CloudBlock.TEMPORARY) || !getBlockState().getValue(CloudBlock.TEMPORARY)) {
            return 255;
        }
        if (!hasActiveFade() || level == null) {
            return 255;
        }

        float expiresAt = placedAtGameTime + durationTicks;
        float now = level.getGameTime() + Math.max(0.0F, partialTick);
        if (now >= expiresAt) {
            return 0;
        }
        if (fadeLastTicks <= 0) {
            return 255;
        }

        float fadeStartsAt = expiresAt - fadeLastTicks;
        if (now <= fadeStartsAt) {
            return 255;
        }

        float t = (now - fadeStartsAt) / Math.max(1.0F, fadeLastTicks);
        t = Math.max(0.0F, Math.min(1.0F, t));
        return Math.max(0, Math.min(255, Math.round((1.0F - t) * 255.0F)));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TemporaryCloudBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!state.hasProperty(CloudBlock.TEMPORARY) || !state.getValue(CloudBlock.TEMPORARY)) {
            return;
        }

        BottleOfCloudPlacedGlass.cleanupNearbyDisplays(serverLevel, pos);

        if (!blockEntity.hasActiveFade()) {
            return;
        }

        long expiresAt = blockEntity.placedAtGameTime + blockEntity.durationTicks;
        if (serverLevel.getGameTime() >= expiresAt) {
            serverLevel.removeBlock(pos, false);
        }
    }

    public static Iterable<BlockPos> getTrackedPositions(Level level) {
        Set<Long> tracked = CLIENT_TRACKED_POSITIONS.get(level);
        if (tracked == null || tracked.isEmpty()) {
            return java.util.List.of();
        }

        ArrayList<BlockPos> snapshot = new ArrayList<>(tracked.size());
        for (long posLong : tracked) {
            snapshot.add(BlockPos.of(posLong));
        }
        return snapshot;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        trackClient(level);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        trackClient(level);
    }

    @Override
    public void setRemoved() {
        untrackClient(level);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(TAG_PLACED_AT, placedAtGameTime);
        output.putInt(TAG_DURATION_TICKS, durationTicks);
        output.putInt(TAG_FADE_LAST_TICKS, fadeLastTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        placedAtGameTime = input.getLongOr(TAG_PLACED_AT, Long.MIN_VALUE);
        durationTicks = Math.max(0, input.getIntOr(TAG_DURATION_TICKS, 0));
        fadeLastTicks = Math.max(0, Math.min(input.getIntOr(TAG_FADE_LAST_TICKS, 0), durationTicks));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void trackClient(Level level) {
        if (level == null || !level.isClientSide()) {
            return;
        }
        CLIENT_TRACKED_POSITIONS
                .computeIfAbsent(level, ignored -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(worldPosition.asLong());
    }

    private void untrackClient(Level level) {
        if (level == null || !level.isClientSide()) {
            return;
        }
        Set<Long> tracked = CLIENT_TRACKED_POSITIONS.get(level);
        if (tracked == null) {
            return;
        }
        tracked.remove(worldPosition.asLong());
        if (tracked.isEmpty()) {
            CLIENT_TRACKED_POSITIONS.remove(level);
        }
    }
}