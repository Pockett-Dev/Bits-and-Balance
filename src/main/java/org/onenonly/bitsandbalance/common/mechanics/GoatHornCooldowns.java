package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** Per-player, per-instrument cooldown timing shared by horn powers and legacy HUD rendering. */
public final class GoatHornCooldowns {
    private static final ReferenceQueue<Player> STALE_PLAYERS = new ReferenceQueue<>();
    private static final Map<WeakPlayer, Map<String, Cooldown>> BY_PLAYER = new HashMap<>();

    private GoatHornCooldowns() {
    }

    public static void begin(Player player, String horn, int ticks) {
        if (horn.isEmpty() || ticks <= 0) {
            return;
        }
        synchronized (BY_PLAYER) {
            removeStalePlayers();
            BY_PLAYER.computeIfAbsent(new WeakPlayer(player, STALE_PLAYERS), ignored -> new HashMap<>())
                    .put(horn, new Cooldown(player.tickCount, player.tickCount + ticks));
        }
    }

    public static int remainingTicks(Player player, String horn) {
        synchronized (BY_PLAYER) {
            removeStalePlayers();
            Map<String, Cooldown> byHorn = BY_PLAYER.get(new WeakPlayer(player, null));
            Cooldown cooldown = byHorn == null ? null : byHorn.get(horn);
            return cooldown == null ? 0 : Math.max(0, cooldown.endTick() - player.tickCount);
        }
    }

    public static float percent(Player player, ItemStack stack, float partialTick) {
        String horn = GoatHornPurposes.horn(stack);
        if (horn.isEmpty()) {
            return 0.0F;
        }
        synchronized (BY_PLAYER) {
            removeStalePlayers();
            Map<String, Cooldown> byHorn = BY_PLAYER.get(new WeakPlayer(player, null));
            Cooldown cooldown = byHorn == null ? null : byHorn.get(horn);
            if (cooldown == null) {
                return 0.0F;
            }
            float duration = cooldown.endTick() - cooldown.startTick();
            return Math.max(0.0F, Math.min(1.0F,
                    (cooldown.endTick() - (player.tickCount + partialTick)) / duration));
        }
    }

    private record Cooldown(int startTick, int endTick) {
    }

    private static void removeStalePlayers() {
        WeakPlayer stale;
        while ((stale = (WeakPlayer) STALE_PLAYERS.poll()) != null) {
            BY_PLAYER.remove(stale);
        }
    }

    private static final class WeakPlayer extends WeakReference<Player> {
        private final int hash;

        private WeakPlayer(Player player, ReferenceQueue<Player> queue) {
            super(player, queue);
            hash = System.identityHashCode(player);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof WeakPlayer key
                    && get() != null && get() == key.get();
        }
    }
}
