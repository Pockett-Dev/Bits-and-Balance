package org.onenonly.bitsandbalance.fabric.combat;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public final class SecondChanceTags {
    private SecondChanceTags() {
    }

    private static final String PREFIX = "rebalance_second_chance_next_allowed:";

    public static long getNextAllowed(Player player) {
        for (String tag : player.getTags()) {
            if (tag != null && tag.startsWith(PREFIX)) {
                try {
                    return Long.parseLong(tag.substring(PREFIX.length()));
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    public static void setNextAllowed(Player player, long nextAllowed) {
        clear(player);
        if (nextAllowed > 0L) {
            player.addTag(PREFIX + nextAllowed);
        }
    }

    public static void clear(Player player) {
        var toRemove = new ArrayList<String>();
        for (String tag : player.getTags()) {
            if (tag != null && tag.startsWith(PREFIX)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            player.removeTag(tag);
        }
    }

    public static void copy(Player from, Player to) {
        long val = getNextAllowed(from);
        if (val > 0L) {
            setNextAllowed(to, val);
        } else {
            clear(to);
        }
    }
}
