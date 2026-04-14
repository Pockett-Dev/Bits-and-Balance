package org.onenonly.bitsandbalance.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.blocks.OreVariants;
import org.onenonly.bitsandbalance.blocks.RedstoneOreVariant;

/**
 * Handles XP drop modifications for mining ores when More Mining XP is enabled.
 * Affects both vanilla ores and ore variants.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class MiningXPEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!Config.enableMoreMiningXp) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // Don't drop XP if the player has Silk Touch
        Player player = event.getPlayer();
        if (player != null) {
            ItemStack tool = player.getMainHandItem();
            if (!tool.isEmpty()) {
                ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                
                // Check for silk touch
                boolean hasSilkTouch = false;
                for (var holder : enchantments.keySet()) {
                    if (holder.is(Enchantments.SILK_TOUCH)) {
                        hasSilkTouch = true;
                        break;
                    }
                }
                
                if (hasSilkTouch) {
                    return; // Don't drop XP with Silk Touch
                }
            }
        }

        BlockState state = event.getState();
        Block block = state.getBlock();
        BlockPos pos = event.getPos();
        RandomSource random = serverLevel.getRandom();

        // Check if this is an ore that should drop XP
        if (isCoalOre(block)) {
            int xpAmount = random.nextIntBetweenInclusive(Config.coalXpMin, Config.coalXpMax);
            if (xpAmount > 0) {
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), xpAmount);
            }
        } else if (isDiamondOre(block)) {
            int xpAmount = random.nextIntBetweenInclusive(Config.diamondXpMin, Config.diamondXpMax);
            if (xpAmount > 0) {
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), xpAmount);
            }
        } else if (isEmeraldOre(block)) {
            int xpAmount = random.nextIntBetweenInclusive(Config.emeraldXpMin, Config.emeraldXpMax);
            if (xpAmount > 0) {
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), xpAmount);
            }
        } else if (isLapisOre(block)) {
            int xpAmount = random.nextIntBetweenInclusive(Config.lapisXpMin, Config.lapisXpMax);
            if (xpAmount > 0) {
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), xpAmount);
            }
        } else if (isRedstoneOre(block)) {
            int xpAmount = random.nextIntBetweenInclusive(Config.redstoneXpMin, Config.redstoneXpMax);
            if (xpAmount > 0) {
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), xpAmount);
            }
        }
    }

    /**
     * Check if the block is a coal ore (vanilla or variant)
     */
    private static boolean isCoalOre(Block block) {
        return block == Blocks.COAL_ORE ||
               block == Blocks.DEEPSLATE_COAL_ORE ||
               block == OreVariants.ANDESITE_COAL_ORE.get() ||
               block == OreVariants.DIORITE_COAL_ORE.get() ||
               block == OreVariants.GRANITE_COAL_ORE.get() ||
               block == OreVariants.TUFF_COAL_ORE.get();
    }

    /**
     * Check if the block is a diamond ore (vanilla or variant)
     */
    private static boolean isDiamondOre(Block block) {
        return block == Blocks.DIAMOND_ORE ||
               block == Blocks.DEEPSLATE_DIAMOND_ORE ||
               block == OreVariants.ANDESITE_DIAMOND_ORE.get() ||
               block == OreVariants.DIORITE_DIAMOND_ORE.get() ||
               block == OreVariants.GRANITE_DIAMOND_ORE.get() ||
               block == OreVariants.TUFF_DIAMOND_ORE.get();
    }

    /**
     * Check if the block is an emerald ore (vanilla or variant)
     */
    private static boolean isEmeraldOre(Block block) {
        return block == Blocks.EMERALD_ORE ||
               block == Blocks.DEEPSLATE_EMERALD_ORE ||
               block == OreVariants.ANDESITE_EMERALD_ORE.get() ||
               block == OreVariants.DIORITE_EMERALD_ORE.get() ||
               block == OreVariants.GRANITE_EMERALD_ORE.get() ||
               block == OreVariants.TUFF_EMERALD_ORE.get();
    }

    /**
     * Check if the block is a lapis ore (vanilla or variant)
     */
    private static boolean isLapisOre(Block block) {
        return block == Blocks.LAPIS_ORE ||
               block == Blocks.DEEPSLATE_LAPIS_ORE ||
               block == OreVariants.ANDESITE_LAPIS_ORE.get() ||
               block == OreVariants.DIORITE_LAPIS_ORE.get() ||
               block == OreVariants.GRANITE_LAPIS_ORE.get() ||
               block == OreVariants.TUFF_LAPIS_ORE.get();
    }

    /**
     * Check if the block is a redstone ore (vanilla or variant)
     */
    private static boolean isRedstoneOre(Block block) {
        return block == Blocks.REDSTONE_ORE ||
               block == Blocks.DEEPSLATE_REDSTONE_ORE ||
               block == OreVariants.ANDESITE_REDSTONE_ORE.get() ||
               block == OreVariants.DIORITE_REDSTONE_ORE.get() ||
               block == OreVariants.GRANITE_REDSTONE_ORE.get() ||
               block == OreVariants.TUFF_REDSTONE_ORE.get();
    }
}
