package org.onenonly.bitsandbalance.fabric.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.common.registry.EnhancedSlabCreativeTabFamilies;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricContentConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricLootConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FabricCreativeTabs {
    private static final String MOD_ID = BitsAndBalanceCommon.MOD_ID;
    private static final Map<String, List<Item>> ENHANCED_SLAB_TAB_ITEM_CACHE = new HashMap<>();

    private FabricCreativeTabs() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("bits_and_balance"), FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.bitsandbalance.bits_and_balance"))
            .icon(() -> itemStack("azalea_log"))
            .displayItems((parameters, output) -> {
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
                    acceptItem(output, "azalea_door");
                    acceptItem(output, "azalea_trapdoor");
                    acceptItem(output, "azalea_sign");
                    acceptItem(output, "azalea_hanging_sign");

                    acceptItem(output, "azalea_boat");
                    acceptItem(output, "azalea_chest_boat");
                }

                acceptItem(output, "andesite_coal_ore");
                acceptItem(output, "diorite_coal_ore");
                acceptItem(output, "granite_coal_ore");
                acceptItem(output, "tuff_coal_ore");

                acceptItem(output, "andesite_iron_ore");
                acceptItem(output, "diorite_iron_ore");
                acceptItem(output, "granite_iron_ore");
                acceptItem(output, "tuff_iron_ore");

                acceptItem(output, "andesite_copper_ore");
                acceptItem(output, "diorite_copper_ore");
                acceptItem(output, "granite_copper_ore");
                acceptItem(output, "tuff_copper_ore");

                acceptItem(output, "andesite_gold_ore");
                acceptItem(output, "diorite_gold_ore");
                acceptItem(output, "granite_gold_ore");
                acceptItem(output, "tuff_gold_ore");

                acceptItem(output, "andesite_diamond_ore");
                acceptItem(output, "diorite_diamond_ore");
                acceptItem(output, "granite_diamond_ore");
                acceptItem(output, "tuff_diamond_ore");

                acceptItem(output, "andesite_emerald_ore");
                acceptItem(output, "diorite_emerald_ore");
                acceptItem(output, "granite_emerald_ore");
                acceptItem(output, "tuff_emerald_ore");

                acceptItem(output, "andesite_lapis_ore");
                acceptItem(output, "diorite_lapis_ore");
                acceptItem(output, "granite_lapis_ore");
                acceptItem(output, "tuff_lapis_ore");

                acceptItem(output, "andesite_redstone_ore");
                acceptItem(output, "diorite_redstone_ore");
                acceptItem(output, "granite_redstone_ore");
                acceptItem(output, "tuff_redstone_ore");

                acceptItem(output, "andesite_zinc_ore");
                acceptItem(output, "diorite_zinc_ore");
                acceptItem(output, "granite_zinc_ore");
                acceptItem(output, "tuff_zinc_ore");

                acceptItem(output, "raw_quartz_block");
                acceptItem(output, "kiln");
                acceptItem(output, "bottle_of_cloud");
                if (FabricContentConfig.enableGlowGoo) {
                    acceptItem(output, "glow_goo");
                }
                if (FabricLootConfig.isDogMusicDiscEnabled()) {
                    acceptItem(output, "music_disc_dog");
                }

                if (FabricTweaksConfig.enableEnhancedSlabs) {
                    if (FabricTweaksConfig.enhancedSlabsVerticalSlabs) {
                        addEnhancedSlabItems(output, "vertical_", "_slab");
                    }
                    if (FabricTweaksConfig.enhancedSlabsSteps) {
                        addEnhancedSlabItems(output, "", "_step");
                        addEnhancedSlabItems(output, "vertical_", "_step");
                    }
                }

                addPotions(output);
            })
            .build());

        registerSourceTabs();
    }

    private static void registerSourceTabs() {
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((group, entries) -> {
            ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(group);
            if (tabId != null && MOD_ID.equals(tabId.getNamespace())) {
                return;
            }

            Set<Item> parentDisplayItems = bitsandbalance$displayItems(entries);

            Item dogDisc = ModDogMusicDisc.musicDiscDog();
            if (FabricLootConfig.isDogMusicDiscEnabled() && dogDisc != Items.AIR && parentDisplayItems.contains(Items.MUSIC_DISC_CAT)) {
                entries.addAfter(Items.MUSIC_DISC_CAT, java.util.List.of(new ItemStack(dogDisc)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }

            if (FabricTweaksConfig.enableEnhancedSlabs && FabricTweaksConfig.enhancedSlabsVerticalSlabs) {
                for (Block vertical : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
                    try {
                        Block slab = VerticalSlabDynamicRegistry.getSlabForVertical(vertical);
                        bitsandbalance$addAfterSource(entries, parentDisplayItems, slab, vertical);
                    } catch (Throwable ignored) {
                    }
                }
            }

            if (FabricTweaksConfig.enableEnhancedSlabs && FabricTweaksConfig.enhancedSlabsSteps) {
                for (Block step : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
                    try {
                        Block slab = StepDynamicRegistry.getSlabForStep(step);
                        bitsandbalance$addAfterSource(entries, parentDisplayItems, slab, step);
                    } catch (Throwable ignored) {
                    }
                }

                for (Block verticalStep : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
                    try {
                        Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(verticalStep);
                        Block sourceSlab = sourceVerticalSlab == null
                                ? null
                                : VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
                        if (sourceSlab == null) {
                            sourceSlab = sourceVerticalSlab;
                        }
                        bitsandbalance$addAfterSource(entries, parentDisplayItems, sourceSlab, verticalStep);
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    private static void bitsandbalance$addAfterSource(net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries entries,
                                                       Set<Item> parentDisplayItems,
                                                       Block sourceBlock,
                                                       Block outputBlock) {
        if (entries == null || sourceBlock == null || outputBlock == null) return;
        Item sourceItem = sourceBlock.asItem();
        if (sourceItem == Items.AIR) return;

        Item outputItem = outputBlock.asItem();
        if (outputItem == Items.AIR) return;

        if (parentDisplayItems == null || !parentDisplayItems.contains(sourceItem)) return;

        entries.addAfter(sourceItem, java.util.List.of(new ItemStack(outputItem)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    private static void addEnhancedSlabItems(CreativeModeTab.Output output, String requiredPrefix, String requiredSuffix) {
        for (Item item : bitsandbalance$enhancedSlabItems(requiredPrefix, requiredSuffix)) {
            if (item != Items.AIR) {
                output.accept(item);
            }
        }
    }

    private static Set<Item> bitsandbalance$displayItems(net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries entries) {
        if (entries == null) return Collections.emptySet();

        Set<Item> items = new HashSet<>();
        for (ItemStack stack : entries.getDisplayStacks()) {
            if (stack == null || stack.isEmpty()) continue;
            items.add(stack.getItem());
        }
        return items;
    }

    private static List<Item> bitsandbalance$enhancedSlabItems(String requiredPrefix, String requiredSuffix) {
        String key = requiredPrefix + "|" + requiredSuffix;
        List<Item> cached = ENHANCED_SLAB_TAB_ITEM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (!MOD_ID.equals(id.getNamespace())) continue;
            String path = id.getPath();
            if (path == null) continue;
            if (!requiredPrefix.isEmpty() && !path.startsWith(requiredPrefix)) continue;
            if (requiredPrefix.isEmpty() && path.startsWith("vertical_")) continue;
            if (!path.endsWith(requiredSuffix)) continue;
            ids.add(id);
        }

        ids.sort(Comparator.comparing(ResourceLocation::toString));
        List<Item> items = new ArrayList<>(ids.size());
        for (ResourceLocation id : ids) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                items.add(item);
            }
        }

        List<Item> built = Collections.unmodifiableList(items);
        ENHANCED_SLAB_TAB_ITEM_CACHE.put(key, built);
        return built;
    }

    private static void addPotions(CreativeModeTab.Output output) {
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
        if (FabricPotionConfig.enableBlindnessPotion) {
            output.accept(potionStack(Items.POTION, "blindness"));
            output.accept(potionStack(Items.POTION, "long_blindness"));
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
        if (FabricPotionConfig.enableBlindnessPotion) {
            output.accept(potionStack(Items.SPLASH_POTION, "blindness"));
            output.accept(potionStack(Items.SPLASH_POTION, "long_blindness"));
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
        if (FabricPotionConfig.enableBlindnessPotion) {
            output.accept(potionStack(Items.LINGERING_POTION, "blindness"));
            output.accept(potionStack(Items.LINGERING_POTION, "long_blindness"));
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
        Item item = BuiltInRegistries.ITEM.get(id(path));
        if (item == null || item == Items.AIR) {
            return;
        }
        output.accept(item);
    }

    private static ItemStack itemStack(String path) {
        Item item = BuiltInRegistries.ITEM.get(id(path));
        if (item != null && item != Items.AIR) {
            return new ItemStack(item);
        }
        return new ItemStack(Items.GRASS_BLOCK);
    }

    private static ItemStack potionStack(Item baseItem, String path) {
        ResourceLocation potionId = ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        Potion potion = BuiltInRegistries.POTION.get(potionId);
        if (potion == null) {
            return new ItemStack(baseItem);
        }
        return PotionContents.createItemStack(baseItem, BuiltInRegistries.POTION.wrapAsHolder(potion));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
