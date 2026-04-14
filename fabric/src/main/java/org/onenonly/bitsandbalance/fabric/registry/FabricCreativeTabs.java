package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;

import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.compat.CreateCompat;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricLootConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FabricCreativeTabs {
    private static final String MOD_ID = BitsAndBalanceCommon.MOD_ID;
    private static final Map<String, List<Item>> ENHANCED_SLAB_TAB_ITEM_CACHE = new HashMap<>();

    private FabricCreativeTabs() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("bits_and_balance"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.bitsandbalance.bits_and_balance"))
            .icon(() -> itemStack("azalea_log"))
            .displayItems((parameters, output) -> {
                // ========================================
                // AZALEA WOOD BLOCKS
                // ========================================
                if (FabricWorldgenConfig.isAzaleaWoodsetEnabled()) {
                    acceptItem(output, "azalea_log");
                    acceptItem(output, "azalea_wood");
                    acceptItem(output, "stripped_azalea_log");
                    acceptItem(output, "stripped_azalea_wood");
                    acceptItem(output, "azalea_planks");

                    acceptItem(output, "azalea_slab");
                    acceptItem(output, "azalea_stairs");
                    acceptItem(output, "azalea_button");
                    acceptItem(output, "azalea_pressure_plate");
                    acceptItem(output, "azalea_fence");
                    acceptItem(output, "azalea_fence_gate");
                    acceptItem(output, "azalea_shelf");
                    acceptItem(output, "azalea_door");
                    acceptItem(output, "azalea_trapdoor");
                    acceptItem(output, "azalea_sign");
                    acceptItem(output, "azalea_hanging_sign");

                    acceptItem(output, "azalea_boat");
                    acceptItem(output, "azalea_chest_boat");
                }

                // ========================================
                // ORE VARIANTS
                // ========================================
                // Coal
                acceptItem(output, "andesite_coal_ore");
                acceptItem(output, "diorite_coal_ore");
                acceptItem(output, "granite_coal_ore");
                acceptItem(output, "tuff_coal_ore");

                // Iron
                acceptItem(output, "andesite_iron_ore");
                acceptItem(output, "diorite_iron_ore");
                acceptItem(output, "granite_iron_ore");
                acceptItem(output, "tuff_iron_ore");

                // Copper
                acceptItem(output, "andesite_copper_ore");
                acceptItem(output, "diorite_copper_ore");
                acceptItem(output, "granite_copper_ore");
                acceptItem(output, "tuff_copper_ore");

                // Gold
                acceptItem(output, "andesite_gold_ore");
                acceptItem(output, "diorite_gold_ore");
                acceptItem(output, "granite_gold_ore");
                acceptItem(output, "tuff_gold_ore");

                // Diamond
                acceptItem(output, "andesite_diamond_ore");
                acceptItem(output, "diorite_diamond_ore");
                acceptItem(output, "granite_diamond_ore");
                acceptItem(output, "tuff_diamond_ore");

                // Emerald
                acceptItem(output, "andesite_emerald_ore");
                acceptItem(output, "diorite_emerald_ore");
                acceptItem(output, "granite_emerald_ore");
                acceptItem(output, "tuff_emerald_ore");

                // Lapis
                acceptItem(output, "andesite_lapis_ore");
                acceptItem(output, "diorite_lapis_ore");
                acceptItem(output, "granite_lapis_ore");
                acceptItem(output, "tuff_lapis_ore");

                // Redstone
                acceptItem(output, "andesite_redstone_ore");
                acceptItem(output, "diorite_redstone_ore");
                acceptItem(output, "granite_redstone_ore");
                acceptItem(output, "tuff_redstone_ore");

                // Zinc
                if (CreateCompat.isZincContentAvailable()) {
                    acceptItem(output, "andesite_zinc_ore");
                    acceptItem(output, "diorite_zinc_ore");
                    acceptItem(output, "granite_zinc_ore");
                    acceptItem(output, "tuff_zinc_ore");
                }

                // Storage blocks
                acceptItem(output, "raw_quartz_block");
                acceptItem(output, "bottle_of_cloud");
                if (org.onenonly.bitsandbalance.fabric.config.FabricContentConfig.enableGlowGoo) {
                    acceptItem(output, "glow_goo");
                }
                if (FabricLootConfig.enableDogMusicDisc) {
                    acceptItem(output, "music_disc_dog");
                }

                // ========================================
                // ENHANCED SLABS (Vertical Slabs)
                // ========================================
                if (FabricTweaksConfig.enableEnhancedSlabs) {
                    if (FabricTweaksConfig.enhancedSlabsVerticalSlabs) {
                        addEnhancedSlabItems(output, "vertical_", "_slab");
                    }
                    if (FabricTweaksConfig.enhancedSlabsSteps) {
                        addEnhancedSlabItems(output, "", "_step");
                        addEnhancedSlabItems(output, "vertical_", "_step");
                    }
                }

                // ========================================
                // POTIONS (mirrors NeoForge CreativeCategory)
                // ========================================
                addPotions(output);
            })
            .build());
    }

    public static void injectEnhancedSlabFamilyItems(CreativeModeTab group,
                                                     Collection<ItemStack> displayItems,
                                                     Collection<ItemStack> searchTabItems) {
        if (!bitsandbalance$shouldInjectEnhancedSlabItems(group)) {
            return;
        }

        bitsandbalance$injectEnhancedSlabFamilies(displayItems, searchTabItems);
    }

    private static boolean bitsandbalance$shouldInjectEnhancedSlabItems(CreativeModeTab group) {
        if (group == null) {
            return false;
        }

        Identifier tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(group);
        if (tabId == null) {
            return false;
        }

        return !MOD_ID.equals(tabId.getNamespace());
    }

    private static void bitsandbalance$injectEnhancedSlabFamilies(Collection<ItemStack> displayItems,
                                                                   Collection<ItemStack> searchTabItems) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) {
            return;
        }

        List<ItemStack> displayStacks = new ArrayList<>(displayItems);
        if (displayStacks.isEmpty()) {
            return;
        }

        List<ItemStack> searchStacks = new ArrayList<>(searchTabItems);

        Set<Item> presentItems = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack stack : displayStacks) {
            Item item = stack.getItem();
            if (item != Items.AIR) {
                presentItems.add(item);
            }
        }

        Set<Item> presentSearchItems = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack stack : searchStacks) {
            Item item = stack.getItem();
            if (item != Items.AIR) {
                presentSearchItems.add(item);
            }
        }

        if (FabricTweaksConfig.enhancedSlabsVerticalSlabs) {
            for (Block verticalBlock : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
                bitsandbalance$insertAfterSource(displayStacks, presentItems,
                        VerticalSlabDynamicRegistry.getSlabForVertical(verticalBlock),
                        verticalBlock);
                bitsandbalance$insertAfterSource(searchStacks, presentSearchItems,
                        VerticalSlabDynamicRegistry.getSlabForVertical(verticalBlock),
                        verticalBlock);
            }
        }

        if (FabricTweaksConfig.enhancedSlabsSteps) {
            for (Block stepBlock : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
                bitsandbalance$insertAfterSource(displayStacks, presentItems,
                        StepDynamicRegistry.getSlabForStep(stepBlock),
                        stepBlock);
                bitsandbalance$insertAfterSource(searchStacks, presentSearchItems,
                        StepDynamicRegistry.getSlabForStep(stepBlock),
                        stepBlock);
            }

            for (Block verticalStepBlock : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
                Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(verticalStepBlock);
                Block sourceSlab = sourceVerticalSlab == null
                        ? null
                        : VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
                if (sourceSlab == null) {
                    sourceSlab = sourceVerticalSlab;
                }
                bitsandbalance$insertAfterSource(displayStacks, presentItems, sourceSlab, verticalStepBlock);
                bitsandbalance$insertAfterSource(searchStacks, presentSearchItems, sourceSlab, verticalStepBlock);
            }
        }

        bitsandbalance$replaceStacks(displayItems, displayStacks);
        bitsandbalance$replaceStacks(searchTabItems, searchStacks);
    }

    private static void bitsandbalance$insertAfterSource(List<ItemStack> entries,
                                                         Set<Item> presentItems,
                                                         Block sourceBlock,
                                                         Block outputBlock) {
        if (sourceBlock == null || outputBlock == null) {
            return;
        }

        Item sourceItem = sourceBlock.asItem();
        Item outputItem = outputBlock.asItem();
        if (sourceItem == Items.AIR || outputItem == Items.AIR || !presentItems.contains(sourceItem) || presentItems.contains(outputItem)) {
            return;
        }

        int insertionIndex = -1;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getItem() == sourceItem) {
                insertionIndex = i;
                break;
            }
        }

        if (insertionIndex < 0) {
            return;
        }

        entries.add(insertionIndex + 1, new ItemStack(outputItem));
        presentItems.add(outputItem);
    }

    private static void bitsandbalance$replaceStacks(Collection<ItemStack> target, List<ItemStack> orderedStacks) {
        Set<ItemStack> rebuilt = ItemStackLinkedSet.createTypeAndComponentsSet();
        rebuilt.addAll(orderedStacks);
        target.clear();
        target.addAll(rebuilt);
    }

    private static void addEnhancedSlabItems(CreativeModeTab.Output output, String requiredPrefix, String requiredSuffix) {
        for (Item item : bitsandbalance$enhancedSlabItems(requiredPrefix, requiredSuffix)) {
            if (item != Items.AIR) {
                output.accept(item);
            }
        }
    }

    private static List<Item> bitsandbalance$enhancedSlabItems(String requiredPrefix, String requiredSuffix) {
        String key = requiredPrefix + "|" + requiredSuffix;
        List<Item> cached = ENHANCED_SLAB_TAB_ITEM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        List<Identifier> ids = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (!MOD_ID.equals(id.getNamespace())) continue;
            String path = id.getPath();
            if (path == null) continue;
            if (!requiredPrefix.isEmpty() && !path.startsWith(requiredPrefix)) continue;
            if (requiredPrefix.isEmpty() && path.startsWith("vertical_")) continue;
            if (!path.endsWith(requiredSuffix)) continue;
            ids.add(id);
        }

        ids.sort(Comparator.comparing(Identifier::toString));
        List<Item> items = new ArrayList<>(ids.size());
        for (Identifier id : ids) {
            BuiltInRegistries.ITEM.get(id).ifPresent(holder -> {
                Item item = holder.value();
                if (item != Items.AIR) {
                    items.add(item);
                }
            });
        }

        List<Item> built = Collections.unmodifiableList(items);
        ENHANCED_SLAB_TAB_ITEM_CACHE.put(key, built);
        return built;
    }

    private static void addPotions(CreativeModeTab.Output output) {
        // Regular potions
        if (FabricPotionConfig.enableWitheringPotion) {
            output.accept(potionStack(Items.POTION, "withering"));
            output.accept(potionStack(Items.POTION, "long_withering"));
            output.accept(potionStack(Items.POTION, "strong_withering"));
        }
        if (FabricPotionConfig.enableResurfacingPotion) {
            output.accept(potionStack(Items.POTION, "resurfacing"));
        }
        if (FabricPotionConfig.enableDisplacementPotion) {
            output.accept(potionStack(Items.POTION, "displacement"));
        }
        if (FabricPotionConfig.enableReturningPotion) {
            output.accept(potionStack(Items.POTION, "returning"));
        }
        if (FabricPotionConfig.enableLevitationPotion) {
            output.accept(potionStack(Items.POTION, "levitation"));
            output.accept(potionStack(Items.POTION, "long_levitation"));
            output.accept(potionStack(Items.POTION, "strong_levitation"));
        }
        if (FabricPotionConfig.enableHastePotion) {
            output.accept(potionStack(Items.POTION, "haste"));
            output.accept(potionStack(Items.POTION, "long_haste"));
            output.accept(potionStack(Items.POTION, "strong_haste"));
        }
        if (FabricPotionConfig.enableGlowingPotion) {
            output.accept(potionStack(Items.POTION, "glowing"));
            output.accept(potionStack(Items.POTION, "long_glowing"));
        }
        if (FabricPotionConfig.enableBioluminescencePotion) {
            output.accept(potionStack(Items.POTION, "bioluminescence"));
            output.accept(potionStack(Items.POTION, "strong_bioluminescence"));
            output.accept(potionStack(Items.POTION, "long_strong_bioluminescence"));
            output.accept(potionStack(Items.POTION, "long_bioluminescence"));
        }

        // Splash potions
        if (FabricPotionConfig.enableWitheringPotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "withering"));
            output.accept(potionStack(Items.SPLASH_POTION, "long_withering"));
            output.accept(potionStack(Items.SPLASH_POTION, "strong_withering"));
        }
        if (FabricPotionConfig.enableResurfacingPotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "resurfacing"));
        }
        if (FabricPotionConfig.enableDisplacementPotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "displacement"));
        }
        if (FabricPotionConfig.enableReturningPotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "returning"));
        }
        if (FabricPotionConfig.enableLevitationPotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "levitation"));
            output.accept(potionStack(Items.SPLASH_POTION, "long_levitation"));
            output.accept(potionStack(Items.SPLASH_POTION, "strong_levitation"));
        }
        if (FabricPotionConfig.enableHastePotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "haste"));
            output.accept(potionStack(Items.SPLASH_POTION, "long_haste"));
            output.accept(potionStack(Items.SPLASH_POTION, "strong_haste"));
        }
        if (FabricPotionConfig.enableGlowingPotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "glowing"));
            output.accept(potionStack(Items.SPLASH_POTION, "long_glowing"));
        }
        if (FabricPotionConfig.enableBioluminescencePotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "bioluminescence"));
            output.accept(potionStack(Items.SPLASH_POTION, "strong_bioluminescence"));
            output.accept(potionStack(Items.SPLASH_POTION, "long_strong_bioluminescence"));
            output.accept(potionStack(Items.SPLASH_POTION, "long_bioluminescence"));
        }

        // Lingering potions
        if (FabricPotionConfig.enableWitheringPotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "withering"));
            output.accept(potionStack(Items.LINGERING_POTION, "long_withering"));
            output.accept(potionStack(Items.LINGERING_POTION, "strong_withering"));
        }
        if (FabricPotionConfig.enableResurfacingPotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "resurfacing"));
        }
        if (FabricPotionConfig.enableDisplacementPotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "displacement"));
        }
        if (FabricPotionConfig.enableReturningPotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "returning"));
        }
        if (FabricPotionConfig.enableLevitationPotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "levitation"));
            output.accept(potionStack(Items.LINGERING_POTION, "long_levitation"));
            output.accept(potionStack(Items.LINGERING_POTION, "strong_levitation"));
        }
        if (FabricPotionConfig.enableHastePotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "haste"));
            output.accept(potionStack(Items.LINGERING_POTION, "long_haste"));
            output.accept(potionStack(Items.LINGERING_POTION, "strong_haste"));
        }
        if (FabricPotionConfig.enableGlowingPotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "glowing"));
            output.accept(potionStack(Items.LINGERING_POTION, "long_glowing"));
        }
        if (FabricPotionConfig.enableBioluminescencePotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "bioluminescence"));
            output.accept(potionStack(Items.LINGERING_POTION, "strong_bioluminescence"));
            output.accept(potionStack(Items.LINGERING_POTION, "long_strong_bioluminescence"));
            output.accept(potionStack(Items.LINGERING_POTION, "long_bioluminescence"));
        }
    }

    private static void acceptItem(CreativeModeTab.Output output, String path) {
        var holderOpt = BuiltInRegistries.ITEM.get(id(path));
        if (holderOpt.isEmpty()) {
            return;
        }
        Item item = holderOpt.get().value();
        if (item == Items.AIR) {
            return;
        }
        output.accept(item);
    }

    private static ItemStack itemStack(String path) {
        var holderOpt = BuiltInRegistries.ITEM.get(id(path));
        if (holderOpt.isPresent()) {
            Item item = holderOpt.get().value();
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(Items.GRASS_BLOCK);
    }

    private static ItemStack potionStack(Item baseItem, String id) {
		var potionId = Identifier.fromNamespaceAndPath(MOD_ID, id);
		var holderOpt = BuiltInRegistries.POTION.get(potionId);
        if (holderOpt.isEmpty()) {
            return new ItemStack(baseItem);
        }
        return PotionContents.createItemStack(baseItem, holderOpt.get());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
