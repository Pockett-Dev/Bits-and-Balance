package org.onenonly.bitsandbalance.fabric.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

/**
 * Fabric port of NeoForge's "More Mining XP" tweak.
 *
 * Adds bonus XP orbs when certain ores are broken without Silk Touch.
 */
public final class FabricMoreMiningXp {
    private FabricMoreMiningXp() {
    }

    public static void init() {
        // Runtime hook is registered by DropExperienceBlockMoreMiningXpMixin.
    }

    public static void awardBonusXp(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        if (!FabricTweaksConfig.enableMoreMiningXp) return;
        if (!dropExperience) return;
        if (state == null || level == null || pos == null) return;
        if (hasSilkTouch(tool)) return;

        Block block = state.getBlock();
        RandomSource random = level.getRandom();

        int xpAmount = 0;
        if (isCoalOre(block)) {
            xpAmount = random.nextIntBetweenInclusive(FabricTweaksConfig.coalXpMin, FabricTweaksConfig.coalXpMax);
        } else if (isDiamondOre(block)) {
            xpAmount = random.nextIntBetweenInclusive(FabricTweaksConfig.diamondXpMin, FabricTweaksConfig.diamondXpMax);
        } else if (isEmeraldOre(block)) {
            xpAmount = random.nextIntBetweenInclusive(FabricTweaksConfig.emeraldXpMin, FabricTweaksConfig.emeraldXpMax);
        } else if (isLapisOre(block)) {
            xpAmount = random.nextIntBetweenInclusive(FabricTweaksConfig.lapisXpMin, FabricTweaksConfig.lapisXpMax);
        } else if (isRedstoneOre(block)) {
            xpAmount = random.nextIntBetweenInclusive(FabricTweaksConfig.redstoneXpMin, FabricTweaksConfig.redstoneXpMax);
        }

        if (xpAmount > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), xpAmount);
        }
    }

    private static boolean hasSilkTouch(ItemStack tool) {
        if (tool == null || tool.isEmpty()) return false;

        ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var holder : enchantments.keySet()) {
            if (holder.is(Enchantments.SILK_TOUCH)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCoalOre(Block block) {
        return block == Blocks.COAL_ORE
            || block == Blocks.DEEPSLATE_COAL_ORE
            || isVariant(block, "andesite_coal_ore")
            || isVariant(block, "diorite_coal_ore")
            || isVariant(block, "granite_coal_ore")
            || isVariant(block, "tuff_coal_ore");
    }

    private static boolean isDiamondOre(Block block) {
        return block == Blocks.DIAMOND_ORE
            || block == Blocks.DEEPSLATE_DIAMOND_ORE
            || isVariant(block, "andesite_diamond_ore")
            || isVariant(block, "diorite_diamond_ore")
            || isVariant(block, "granite_diamond_ore")
            || isVariant(block, "tuff_diamond_ore");
    }

    private static boolean isEmeraldOre(Block block) {
        return block == Blocks.EMERALD_ORE
            || block == Blocks.DEEPSLATE_EMERALD_ORE
            || isVariant(block, "andesite_emerald_ore")
            || isVariant(block, "diorite_emerald_ore")
            || isVariant(block, "granite_emerald_ore")
            || isVariant(block, "tuff_emerald_ore");
    }

    private static boolean isLapisOre(Block block) {
        return block == Blocks.LAPIS_ORE
            || block == Blocks.DEEPSLATE_LAPIS_ORE
            || isVariant(block, "andesite_lapis_ore")
            || isVariant(block, "diorite_lapis_ore")
            || isVariant(block, "granite_lapis_ore")
            || isVariant(block, "tuff_lapis_ore");
    }

    private static boolean isRedstoneOre(Block block) {
        return block == Blocks.REDSTONE_ORE
            || block == Blocks.DEEPSLATE_REDSTONE_ORE
            || isVariant(block, "andesite_redstone_ore")
            || isVariant(block, "diorite_redstone_ore")
            || isVariant(block, "granite_redstone_ore")
            || isVariant(block, "tuff_redstone_ore");
    }

    private static boolean isVariant(Block block, String path) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null
            && BitsAndBalanceCommon.MOD_ID.equals(id.getNamespace())
            && path.equals(id.getPath());
    }
}
