package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnhancedSlabMiningTarget {
    private static final Map<UUID, Target> TARGETS = new ConcurrentHashMap<>();

    private EnhancedSlabMiningTarget() {
    }

    public static void lock(Player player, BlockPos pos, SlabType half) {
        TARGETS.put(player.getUUID(), new Target(pos.asLong(), half));
    }

    public static @Nullable SlabType get(Player player, BlockPos pos) {
        Target target = TARGETS.get(player.getUUID());
        return target != null && target.posLong == pos.asLong() ? target.half : null;
    }

    public static void clear(Player player) {
        TARGETS.remove(player.getUUID());
    }

    private static final class Target {
        private final long posLong;
        private final SlabType half;

        private Target(long posLong, SlabType half) {
            this.posLong = posLong;
            this.half = half;
        }
    }
}