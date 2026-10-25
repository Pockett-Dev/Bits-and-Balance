package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class BiomeTitlesStructureTracker {
    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final Map<UUID, Boolean> LAST_STRUCTURE_STATES = new HashMap<>();

    private BiomeTitlesStructureTracker() {
    }

    public static void tick(ServerPlayer player, BiConsumer<ServerPlayer, Boolean> sender) {
        if (player == null || sender == null || player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        boolean insideStructure = isInsideStructure(player);
        UUID playerId = player.getUUID();
        Boolean previous = LAST_STRUCTURE_STATES.put(playerId, insideStructure);
        if (previous == null || previous.booleanValue() != insideStructure || player.tickCount <= CHECK_INTERVAL_TICKS) {
            sender.accept(player, insideStructure);
        }
    }

    public static void forgetMissingPlayers(Iterable<ServerPlayer> players) {
        if (players == null) {
            LAST_STRUCTURE_STATES.clear();
            return;
        }

        Map<UUID, Boolean> present = new HashMap<>();
        for (ServerPlayer player : players) {
            if (player != null) {
                present.put(player.getUUID(), Boolean.TRUE);
            }
        }

        Iterator<UUID> iterator = LAST_STRUCTURE_STATES.keySet().iterator();
        while (iterator.hasNext()) {
            if (!present.containsKey(iterator.next())) {
                iterator.remove();
            }
        }
    }

    private static boolean isInsideStructure(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        BlockPos pos = player.blockPosition();
        try {
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, holder -> true);
            return start != null && start.isValid();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
