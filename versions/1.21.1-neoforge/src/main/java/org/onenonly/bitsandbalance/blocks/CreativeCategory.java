package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.ModBlocks;
import org.onenonly.bitsandbalance.ModItems;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.common.registry.EnhancedSlabCreativeTabFamilies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creative tab (category) for all Bits and Balance content.
 * Includes Azalea wood blocks, ore variants, and potions.
 */
public class CreativeCategory {
    private static final String MOD_ID = BitsAndBalanceCommon.MOD_ID;

    // Register the Bits and Balance tab via the main mod's CreativeModeTab deferred register
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BITS_AND_BALANCE_TAB = BitsAndBalance.CREATIVE_MODE_TABS.register(
            "bits_and_balance",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bitsandbalance.bits_and_balance"))
                .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ModItems.AZALEA_LOG.get()))
                    .displayItems((parameters, output) -> {
                        // ========================================
                        // AZALEA WOOD BLOCKS
                        // ========================================
                        // Only show azalea woodset if enabled in config
                        if (org.onenonly.bitsandbalance.WorldConfig.ENABLE_AZALEA_WOODSET.get()) {
                            output.accept(ModItems.AZALEA_LOG.get());
                            output.accept(ModItems.AZALEA_WOOD.get());
                            output.accept(ModItems.STRIPPED_AZALEA_LOG.get());
                            output.accept(ModItems.STRIPPED_AZALEA_WOOD.get());
                            output.accept(ModItems.AZALEA_PLANKS.get());
                            
                            // Azalea Wood Derivatives
                            output.accept(ModItems.AZALEA_SLAB.get());
                            addEnhancedSlabFamily(output, ModBlocks.AZALEA_SLAB.get());
                            output.accept(ModItems.AZALEA_STAIRS.get());
                            output.accept(ModItems.AZALEA_BUTTON.get());
                            output.accept(ModItems.AZALEA_PRESSURE_PLATE.get());
                            output.accept(ModItems.AZALEA_FENCE.get());
                            output.accept(ModItems.AZALEA_FENCE_GATE.get());
                            output.accept(ModItems.AZALEA_DOOR.get());
                            output.accept(ModItems.AZALEA_TRAPDOOR.get());
                            output.accept(ModItems.AZALEA_SIGN.get());
                            output.accept(ModItems.AZALEA_HANGING_SIGN.get());
                            
                            // Azalea Boats
                            output.accept(ModItems.AZALEA_BOAT.get());
                            output.accept(ModItems.AZALEA_CHEST_BOAT.get());
                        }
                        
                        // ========================================
                        // ORE VARIANTS
                        // ========================================
                        // Coal Ore Variants
                        output.accept(ModItems.ANDESITE_COAL_ORE.get());
                        output.accept(ModItems.DIORITE_COAL_ORE.get());
                        output.accept(ModItems.GRANITE_COAL_ORE.get());
                        output.accept(ModItems.TUFF_COAL_ORE.get());
                        
                        // Iron Ore Variants
                        output.accept(ModItems.ANDESITE_IRON_ORE.get());
                        output.accept(ModItems.DIORITE_IRON_ORE.get());
                        output.accept(ModItems.GRANITE_IRON_ORE.get());
                        output.accept(ModItems.TUFF_IRON_ORE.get());
                        
                        // Copper Ore Variants
                        output.accept(ModItems.ANDESITE_COPPER_ORE.get());
                        output.accept(ModItems.DIORITE_COPPER_ORE.get());
                        output.accept(ModItems.GRANITE_COPPER_ORE.get());
                        output.accept(ModItems.TUFF_COPPER_ORE.get());
                        
                        // Gold Ore Variants
                        output.accept(ModItems.ANDESITE_GOLD_ORE.get());
                        output.accept(ModItems.DIORITE_GOLD_ORE.get());
                        output.accept(ModItems.GRANITE_GOLD_ORE.get());
                        output.accept(ModItems.TUFF_GOLD_ORE.get());
                        
                        // Diamond Ore Variants
                        output.accept(ModItems.ANDESITE_DIAMOND_ORE.get());
                        output.accept(ModItems.DIORITE_DIAMOND_ORE.get());
                        output.accept(ModItems.GRANITE_DIAMOND_ORE.get());
                        output.accept(ModItems.TUFF_DIAMOND_ORE.get());
                        
                        // Emerald Ore Variants
                        output.accept(ModItems.ANDESITE_EMERALD_ORE.get());
                        output.accept(ModItems.DIORITE_EMERALD_ORE.get());
                        output.accept(ModItems.GRANITE_EMERALD_ORE.get());
                        output.accept(ModItems.TUFF_EMERALD_ORE.get());
                        
                        // Lapis Ore Variants
                        output.accept(ModItems.ANDESITE_LAPIS_ORE.get());
                        output.accept(ModItems.DIORITE_LAPIS_ORE.get());
                        output.accept(ModItems.GRANITE_LAPIS_ORE.get());
                        output.accept(ModItems.TUFF_LAPIS_ORE.get());
                        
                        // Redstone Ore Variants
                        output.accept(ModItems.ANDESITE_REDSTONE_ORE.get());
                        output.accept(ModItems.DIORITE_REDSTONE_ORE.get());
                        output.accept(ModItems.GRANITE_REDSTONE_ORE.get());
                        output.accept(ModItems.TUFF_REDSTONE_ORE.get());
                        
                        // Zinc Ore Variants (Create Mod)
                        output.accept(ModItems.ANDESITE_ZINC_ORE.get());
                        output.accept(ModItems.DIORITE_ZINC_ORE.get());
                        output.accept(ModItems.GRANITE_ZINC_ORE.get());
                        output.accept(ModItems.TUFF_ZINC_ORE.get());

                        // ========================================
                        // STORAGE BLOCKS
                        // ========================================
                        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("bitsandbalance:raw_quartz_block"))
                            .ifPresent(output::accept);
                        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("bitsandbalance:kiln"))
                            .ifPresent(output::accept);
                        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("bitsandbalance:bottle_of_cloud"))
                            .ifPresent(output::accept);
                        if (Config.enableGlowGoo) {
                            BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("bitsandbalance:glow_goo"))
                                .ifPresent(output::accept);
                        }
                        if (Config.enableDogMusicDisc) {
                            BuiltInRegistries.ITEM.getOptional(ModDogMusicDisc.MUSIC_DISC_DOG_ID)
                                .ifPresent(output::accept);
                        }

                        // ========================================
                        // ENHANCED SLABS
                        // ========================================
                        if (Config.enableEnhancedSlabs) {
                            addEnhancedSlabFamilies(output, ModBlocks.AZALEA_SLAB.get());
                        }
                        
                        // ========================================
                        // POTIONS
                        // ========================================
                        // Regular potions
                        output.accept(potionStack(Items.POTION, "withering"));
                        output.accept(potionStack(Items.POTION, "long_withering"));
                        output.accept(potionStack(Items.POTION, "strong_withering"));
                        output.accept(potionStack(Items.POTION, "resurfacing"));
                        output.accept(potionStack(Items.POTION, "displacement"));
                        output.accept(potionStack(Items.POTION, "returning"));
                        output.accept(potionStack(Items.POTION, "levitation"));
                        output.accept(potionStack(Items.POTION, "long_levitation"));
                        output.accept(potionStack(Items.POTION, "strong_levitation"));
                        output.accept(potionStack(Items.POTION, "haste"));
                        output.accept(potionStack(Items.POTION, "long_haste"));
                        output.accept(potionStack(Items.POTION, "strong_haste"));
                        if (Config.enableBlindnessPotion) {
                            output.accept(potionStack(Items.POTION, "blindness"));
                            output.accept(potionStack(Items.POTION, "long_blindness"));
                        }

                        if (Config.enableGlowingPotion) {
                            output.accept(potionStack(Items.POTION, "glowing"));
                            output.accept(potionStack(Items.POTION, "long_glowing"));
                        }
                        if (Config.enableBioluminescencePotion) {
                            output.accept(potionStack(Items.POTION, "bioluminescence"));
                            output.accept(potionStack(Items.POTION, "strong_bioluminescence"));
                            output.accept(potionStack(Items.POTION, "long_strong_bioluminescence"));
                            output.accept(potionStack(Items.POTION, "long_bioluminescence"));
                        }
                        
                        // Splash potions
                        output.accept(potionStack(Items.SPLASH_POTION, "withering"));
                        output.accept(potionStack(Items.SPLASH_POTION, "long_withering"));
                        output.accept(potionStack(Items.SPLASH_POTION, "strong_withering"));
                        output.accept(potionStack(Items.SPLASH_POTION, "resurfacing"));
                        output.accept(potionStack(Items.SPLASH_POTION, "displacement"));
                        output.accept(potionStack(Items.SPLASH_POTION, "returning"));
                        output.accept(potionStack(Items.SPLASH_POTION, "levitation"));
                        output.accept(potionStack(Items.SPLASH_POTION, "long_levitation"));
                        output.accept(potionStack(Items.SPLASH_POTION, "strong_levitation"));
                        output.accept(potionStack(Items.SPLASH_POTION, "haste"));
                        output.accept(potionStack(Items.SPLASH_POTION, "long_haste"));
                        output.accept(potionStack(Items.SPLASH_POTION, "strong_haste"));
                        if (Config.enableBlindnessPotion) {
                            output.accept(potionStack(Items.SPLASH_POTION, "blindness"));
                            output.accept(potionStack(Items.SPLASH_POTION, "long_blindness"));
                        }

                        if (Config.enableGlowingPotion) {
                            output.accept(potionStack(Items.SPLASH_POTION, "glowing"));
                            output.accept(potionStack(Items.SPLASH_POTION, "long_glowing"));
                        }
                        if (Config.enableBioluminescencePotion) {
                            output.accept(potionStack(Items.SPLASH_POTION, "bioluminescence"));
                            output.accept(potionStack(Items.SPLASH_POTION, "strong_bioluminescence"));
                            output.accept(potionStack(Items.SPLASH_POTION, "long_strong_bioluminescence"));
                            output.accept(potionStack(Items.SPLASH_POTION, "long_bioluminescence"));
                        }
                        
                        // Lingering potions
                        output.accept(potionStack(Items.LINGERING_POTION, "withering"));
                        output.accept(potionStack(Items.LINGERING_POTION, "long_withering"));
                        output.accept(potionStack(Items.LINGERING_POTION, "strong_withering"));
                        output.accept(potionStack(Items.LINGERING_POTION, "resurfacing"));
                        output.accept(potionStack(Items.LINGERING_POTION, "displacement"));
                        output.accept(potionStack(Items.LINGERING_POTION, "returning"));
                        output.accept(potionStack(Items.LINGERING_POTION, "levitation"));
                        output.accept(potionStack(Items.LINGERING_POTION, "long_levitation"));
                        output.accept(potionStack(Items.LINGERING_POTION, "strong_levitation"));
                        output.accept(potionStack(Items.LINGERING_POTION, "haste"));
                        output.accept(potionStack(Items.LINGERING_POTION, "long_haste"));
                        output.accept(potionStack(Items.LINGERING_POTION, "strong_haste"));
                        if (Config.enableBlindnessPotion) {
                            output.accept(potionStack(Items.LINGERING_POTION, "blindness"));
                            output.accept(potionStack(Items.LINGERING_POTION, "long_blindness"));
                        }

                        if (Config.enableGlowingPotion) {
                            output.accept(potionStack(Items.LINGERING_POTION, "glowing"));
                            output.accept(potionStack(Items.LINGERING_POTION, "long_glowing"));
                        }
                        if (Config.enableBioluminescencePotion) {
                            output.accept(potionStack(Items.LINGERING_POTION, "bioluminescence"));
                            output.accept(potionStack(Items.LINGERING_POTION, "strong_bioluminescence"));
                            output.accept(potionStack(Items.LINGERING_POTION, "long_strong_bioluminescence"));
                            output.accept(potionStack(Items.LINGERING_POTION, "long_bioluminescence"));
                        }
                    })
                    .build()
    );
    private static void addEnhancedSlabFamily(CreativeModeTab.Output output, Block sourceSlab) {
        if (!Config.enableEnhancedSlabs || sourceSlab == null) {
            return;
        }

        for (EnhancedSlabCreativeTabFamilies.Family family : EnhancedSlabCreativeTabFamilies.snapshot(
                Config.enhancedSlabsVerticalSlabs,
                Config.enhancedSlabsSteps)) {
            if (family.sourceSlab() == sourceSlab) {
                bitsandbalance$acceptFamilyItems(output, family);
                return;
            }
        }
    }

    private static void addEnhancedSlabFamilies(CreativeModeTab.Output output, Block excludedSourceSlab) {
        if (!Config.enableEnhancedSlabs) {
            return;
        }

        for (EnhancedSlabCreativeTabFamilies.Family family : EnhancedSlabCreativeTabFamilies.snapshot(
                Config.enhancedSlabsVerticalSlabs,
                Config.enhancedSlabsSteps)) {
            if (family.sourceSlab() == excludedSourceSlab) {
                continue;
            }
            bitsandbalance$acceptFamilyItems(output, family);
        }
    }

    private static void bitsandbalance$acceptFamilyItems(CreativeModeTab.Output output,
                                                         EnhancedSlabCreativeTabFamilies.Family family) {
        if (family == null) {
            return;
        }

        if (Config.enhancedSlabsVerticalSlabs) {
            bitsandbalance$acceptBlock(output, family.verticalSlab());
        }
        if (Config.enhancedSlabsSteps) {
            bitsandbalance$acceptBlock(output, family.step());
            bitsandbalance$acceptBlock(output, family.verticalStep());
        }
    }

    private static void bitsandbalance$acceptBlock(CreativeModeTab.Output output, Block block) {
        if (output == null || block == null) {
            return;
        }

        Item item = block.asItem();
        if (item != Items.AIR) {
            output.accept(item);
        }
    }

    private static Item itemOrAir(String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(BitsAndBalance.MODID + ":" + path))
                .orElse(Items.AIR);
    }

    /**
     * Helper method to create a potion ItemStack with the specified potion type.
     */
    private static ItemStack potionStack(Item baseItem, String id) {
        var rl = ResourceLocation.parse(BitsAndBalance.MODID + ":" + id);
        var potion = BuiltInRegistries.POTION.getOptional(rl).orElse(null);
        if (potion == null) {
            return new ItemStack(baseItem);
        }
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(baseItem, BuiltInRegistries.POTION.wrapAsHolder(potion));
    }

    /**
     * Ensures the class is initialized so that the tab is registered.
     */
    public static void register() {
        // no-op; referencing this method from BitsAndBalance guarantees class loading
    }
}

