package org.onenonly.bitsandbalance.common.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Shared Fortune scaling; each loot entry chooses whether it is a rare find or common materials. */
public final class BrushFortune {
    public static volatile boolean enabled = true;
    private static volatile Set<String> itemBlacklist = Set.of();
    private BrushFortune() {}

    public static void setItemBlacklist(Collection<? extends String> items) {
        Set<String> ids = new HashSet<>();
        for (String item : items) {
            if (item != null && !item.isBlank()) ids.add(item.trim());
        }
        itemBlacklist = Set.copyOf(ids);
    }

    /** Match the reward's registry ID, including items supplied by other mods. */
    public static boolean isItemBlacklisted(ItemStack reward) {
        Set<String> blacklist = itemBlacklist;
        return !blacklist.isEmpty() && !reward.isEmpty()
                && blacklist.contains(BuiltInRegistries.ITEM.getKey(reward.getItem()).toString());
    }

    public static ItemStack activeBrush(LivingEntity brusher) {
        ItemStack active = brusher.getUseItem();
        if (!active.isEmpty()) return active.is(Items.BRUSH) ? active : ItemStack.EMPTY;
        // Allows non-using living entities and direct brush calls; normal use supplies the active hand.
        return brusher.getMainHandItem().is(Items.BRUSH) ? brusher.getMainHandItem() : ItemStack.EMPTY;
    }

    public static int level(ItemStack brush, Level level) {
        if (!enabled || !brush.is(Items.BRUSH)) return 0;
        var fortune = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        return Math.clamp(EnchantmentHelper.getItemEnchantmentLevel(fortune, brush), 0, 3);
    }

    /** Configured XP values are literal points on every loader and Minecraft version. */
    public static int baseExperience(int configuredMin, int configuredMax) {
        int min = Math.max(0, configuredMin);
        int max = Math.max(min, configuredMax);
        if (min == max) return min;
        return (int) ThreadLocalRandom.current().nextLong(min, (long) max + 1L);
    }

    public static int experience(int base, ItemStack brush, Level level) {
        int fortune = level(brush, level);
        return (int) Math.min(Integer.MAX_VALUE, Math.round(base * (1.0D + fortune * 0.5D)));
    }

    public static int commonCount(int base, int fortune) {
        // Fortune III triples the original roll. Lower levels interpolate with integer rounding.
        return (int) Math.min(64L, base + Math.round(base * 2.0D * fortune / 3.0D));
    }
}
