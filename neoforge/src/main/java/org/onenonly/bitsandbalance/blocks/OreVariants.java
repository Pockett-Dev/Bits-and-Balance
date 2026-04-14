package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.util.valueproviders.UniformInt;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Registers ore variant blocks for all vanilla ores.
 * Each ore has variants for Andesite, Diorite, Granite, and Tuff host stones.
 * 
 * Properties (hardness, sound, etc.) are copied from the vanilla stone blocks,
 * ensuring compatibility with mods that modify stone block properties.
 */
public class OreVariants {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BitsAndBalance.MODID);

    // ========================================
    // COAL ORE VARIANTS
    // ========================================
    public static final DeferredHolder<Block, CustomOreBlock> ANDESITE_COAL_ORE = BLOCKS.register("andesite_coal_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.COAL, 0, 2)); // Use coal ore properties but andesite sound, drop coal, 0-2 XP

    public static final DeferredHolder<Block, CustomOreBlock> DIORITE_COAL_ORE = BLOCKS.register("diorite_coal_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.COAL, 0, 2)); // Use coal ore properties but diorite sound, drop coal, 0-2 XP

    public static final DeferredHolder<Block, CustomOreBlock> GRANITE_COAL_ORE = BLOCKS.register("granite_coal_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.COAL, 0, 2)); // Use coal ore properties but granite sound, drop coal, 0-2 XP

    public static final DeferredHolder<Block, CustomOreBlock> TUFF_COAL_ORE = BLOCKS.register("tuff_coal_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.COAL, 0, 2)); // Use coal ore properties but tuff sound, drop coal, 0-2 XP

    // ========================================
    // IRON ORE VARIANTS
    // ========================================
    public static final DeferredHolder<Block, CustomOreBlock> ANDESITE_IRON_ORE = BLOCKS.register("andesite_iron_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_IRON, 0, 0)); // Use iron ore properties but andesite sound, drop raw iron, no XP

    public static final DeferredHolder<Block, CustomOreBlock> DIORITE_IRON_ORE = BLOCKS.register("diorite_iron_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_IRON, 0, 0)); // Use iron ore properties but diorite sound, drop raw iron, no XP

    public static final DeferredHolder<Block, CustomOreBlock> GRANITE_IRON_ORE = BLOCKS.register("granite_iron_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_IRON, 0, 0)); // Use iron ore properties but granite sound, drop raw iron, no XP

    public static final DeferredHolder<Block, CustomOreBlock> TUFF_IRON_ORE = BLOCKS.register("tuff_iron_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_IRON, 0, 0)); // Use iron ore properties but tuff sound, drop raw iron, no XP

    // ========================================
    // COPPER ORE VARIANTS
    // ========================================
    public static final DeferredHolder<Block, CustomOreBlock> ANDESITE_COPPER_ORE = BLOCKS.register("andesite_copper_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_COPPER, 0, 0)); // Use copper ore properties but andesite sound, drop raw copper, no XP

    public static final DeferredHolder<Block, CustomOreBlock> DIORITE_COPPER_ORE = BLOCKS.register("diorite_copper_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_COPPER, 0, 0)); // Use copper ore properties but diorite sound, drop raw copper, no XP

    public static final DeferredHolder<Block, CustomOreBlock> GRANITE_COPPER_ORE = BLOCKS.register("granite_copper_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_COPPER, 0, 0)); // Use copper ore properties but granite sound, drop raw copper, no XP

    public static final DeferredHolder<Block, CustomOreBlock> TUFF_COPPER_ORE = BLOCKS.register("tuff_copper_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_COPPER, 0, 0)); // Use copper ore properties but tuff sound, drop raw copper, no XP

    // ========================================
    // GOLD ORE VARIANTS
    // ========================================
    public static final DeferredHolder<Block, CustomOreBlock> ANDESITE_GOLD_ORE = BLOCKS.register("andesite_gold_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_GOLD, 0, 0)); // Use gold ore properties but andesite sound, drop raw gold, no XP

    public static final DeferredHolder<Block, CustomOreBlock> DIORITE_GOLD_ORE = BLOCKS.register("diorite_gold_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_GOLD, 0, 0)); // Use gold ore properties but diorite sound, drop raw gold, no XP

    public static final DeferredHolder<Block, CustomOreBlock> GRANITE_GOLD_ORE = BLOCKS.register("granite_gold_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_GOLD, 0, 0)); // Use gold ore properties but granite sound, drop raw gold, no XP

    public static final DeferredHolder<Block, CustomOreBlock> TUFF_GOLD_ORE = BLOCKS.register("tuff_gold_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.RAW_GOLD, 0, 0)); // Use gold ore properties but tuff sound, drop raw gold, no XP

    // ========================================
    // DIAMOND ORE VARIANTS
    // ========================================
    public static final DeferredHolder<Block, CustomOreBlock> ANDESITE_DIAMOND_ORE = BLOCKS.register("andesite_diamond_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.DIAMOND, 3, 7)); // Use diamond ore properties but andesite sound, drop diamond, 3-7 XP

    public static final DeferredHolder<Block, CustomOreBlock> DIORITE_DIAMOND_ORE = BLOCKS.register("diorite_diamond_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.DIAMOND, 3, 7)); // Use diamond ore properties but diorite sound, drop diamond, 3-7 XP

    public static final DeferredHolder<Block, CustomOreBlock> GRANITE_DIAMOND_ORE = BLOCKS.register("granite_diamond_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.DIAMOND, 3, 7)); // Use diamond ore properties but granite sound, drop diamond, 3-7 XP

    public static final DeferredHolder<Block, CustomOreBlock> TUFF_DIAMOND_ORE = BLOCKS.register("tuff_diamond_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.DIAMOND, 3, 7)); // Use diamond ore properties but tuff sound, drop diamond, 3-7 XP

    // ========================================
    // EMERALD ORE VARIANTS
    // ========================================
    public static final DeferredHolder<Block, CustomOreBlock> ANDESITE_EMERALD_ORE = BLOCKS.register("andesite_emerald_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.EMERALD, 3, 7)); // Use emerald ore properties but andesite sound, drop emerald, 3-7 XP

    public static final DeferredHolder<Block, CustomOreBlock> DIORITE_EMERALD_ORE = BLOCKS.register("diorite_emerald_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.EMERALD, 3, 7)); // Use emerald ore properties but diorite sound, drop emerald, 3-7 XP

    public static final DeferredHolder<Block, CustomOreBlock> GRANITE_EMERALD_ORE = BLOCKS.register("granite_emerald_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.EMERALD, 3, 7)); // Use emerald ore properties but granite sound, drop emerald, 3-7 XP

    public static final DeferredHolder<Block, CustomOreBlock> TUFF_EMERALD_ORE = BLOCKS.register("tuff_emerald_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.EMERALD, 3, 7)); // Use emerald ore properties but tuff sound, drop emerald, 3-7 XP

    // ========================================
    // LAPIS ORE VARIANTS
    // ========================================
    public static final DeferredHolder<Block, CustomOreBlock> ANDESITE_LAPIS_ORE = BLOCKS.register("andesite_lapis_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.LAPIS_LAZULI, 2, 5, 4, 9)); // Drop 4-9 lapis, 2-5 XP

    public static final DeferredHolder<Block, CustomOreBlock> DIORITE_LAPIS_ORE = BLOCKS.register("diorite_lapis_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.LAPIS_LAZULI, 2, 5, 4, 9)); // Drop 4-9 lapis, 2-5 XP

    public static final DeferredHolder<Block, CustomOreBlock> GRANITE_LAPIS_ORE = BLOCKS.register("granite_lapis_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.LAPIS_LAZULI, 2, 5, 4, 9)); // Drop 4-9 lapis, 2-5 XP

    public static final DeferredHolder<Block, CustomOreBlock> TUFF_LAPIS_ORE = BLOCKS.register("tuff_lapis_ore",
            (Identifier key) -> new CustomOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops(), 
                    Items.LAPIS_LAZULI, 2, 5, 4, 9)); // Drop 4-9 lapis, 2-5 XP

    // ========================================
    // REDSTONE ORE VARIANTS
    // ========================================
    public static final DeferredHolder<Block, CustomRedstoneOreBlock> ANDESITE_REDSTONE_ORE = BLOCKS.register("andesite_redstone_ore",
            (Identifier key) -> new CustomRedstoneOreBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE)
                            .setId(ResourceKey.create(Registries.BLOCK, key))
                            .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> state.getValue(CustomRedstoneOreBlock.LIT) ? 9 : 0)
                            .randomTicks(), 
                    Items.REDSTONE, 1, 5, 4, 5)); // Drop 4-5 redstone, 1-5 XP

    public static final DeferredHolder<Block, CustomRedstoneOreBlock> DIORITE_REDSTONE_ORE = BLOCKS.register("diorite_redstone_ore",
            (Identifier key) -> new CustomRedstoneOreBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE)
                            .setId(ResourceKey.create(Registries.BLOCK, key))
                            .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> state.getValue(CustomRedstoneOreBlock.LIT) ? 9 : 0)
                            .randomTicks(), 
                    Items.REDSTONE, 1, 5, 4, 5)); // Drop 4-5 redstone, 1-5 XP

    public static final DeferredHolder<Block, CustomRedstoneOreBlock> GRANITE_REDSTONE_ORE = BLOCKS.register("granite_redstone_ore",
            (Identifier key) -> new CustomRedstoneOreBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE)
                            .setId(ResourceKey.create(Registries.BLOCK, key))
                            .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> state.getValue(CustomRedstoneOreBlock.LIT) ? 9 : 0)
                            .randomTicks(), 
                    Items.REDSTONE, 1, 5, 4, 5)); // Drop 4-5 redstone, 1-5 XP

    public static final DeferredHolder<Block, CustomRedstoneOreBlock> TUFF_REDSTONE_ORE = BLOCKS.register("tuff_redstone_ore",
            (Identifier key) -> new CustomRedstoneOreBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE)
                            .setId(ResourceKey.create(Registries.BLOCK, key))
                            .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> state.getValue(CustomRedstoneOreBlock.LIT) ? 9 : 0)
                            .randomTicks(), 
                    Items.REDSTONE, 1, 5, 4, 5)); // Drop 4-5 redstone, 1-5 XP

    // ========================================
    // ZINC ORE VARIANTS (Create Mod)
    // ========================================
    public static final DeferredHolder<Block, CustomZincOreBlock> ANDESITE_ZINC_ORE = BLOCKS.register("andesite_zinc_ore",
            (Identifier key) -> new CustomZincOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.ANDESITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops())); // Use iron ore properties but andesite sound

    public static final DeferredHolder<Block, CustomZincOreBlock> DIORITE_ZINC_ORE = BLOCKS.register("diorite_zinc_ore",
            (Identifier key) -> new CustomZincOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.DIORITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops())); // Use iron ore properties but diorite sound

    public static final DeferredHolder<Block, CustomZincOreBlock> GRANITE_ZINC_ORE = BLOCKS.register("granite_zinc_ore",
            (Identifier key) -> new CustomZincOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.GRANITE.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops())); // Use iron ore properties but granite sound

    public static final DeferredHolder<Block, CustomZincOreBlock> TUFF_ZINC_ORE = BLOCKS.register("tuff_zinc_ore",
            (Identifier key) -> new CustomZincOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)
                    .setId(ResourceKey.create(Registries.BLOCK, key))
                    .sound(Blocks.TUFF.defaultBlockState().getSoundType())
                    .requiresCorrectToolForDrops())); // Use iron ore properties but tuff sound

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}

