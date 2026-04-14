package org.onenonly.bitsandbalance.fabric.registry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;

import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.common.registry.EnhancedSlabCreativeTabFamilies;
import org.onenonly.bitsandbalance.compat.CreateCompat;
import org.onenonly.bitsandbalance.fabric.config.FabricContentConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;

import java.util.ArrayList;
import java.util.Collection;
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
                if (FabricContentConfig.enableGlowGoo) {
                    acceptItem(output, "glow_goo");
                }
                if (FabricWorldgenConfig.isDogMusicDiscEnabled()) {
                    acceptItem(output, "music_disc_dog");
                }

                // ========================================
                // ENHANCED SLABS (Vertical Slabs)
                // ========================================
                if (FabricTweaksConfig.isVerticalSlabRuntimeEnabled()) {
                    addEnhancedSlabItems(output, "vertical_", "_slab");
                }
                if (FabricTweaksConfig.isStepRuntimeEnabled()) {
                    addEnhancedSlabItems(output, "", "_step");
                    addEnhancedSlabItems(output, "vertical_", "_step");
                }

                // ========================================
                // POTIONS (mirrors NeoForge CreativeCategory)
                // ========================================
                addPotions(output);
            })
            .build());

        // Also inject enhanced slab-family items into any tab that already contains
        // the corresponding source slab, including source-mod tabs.
        registerSourceTabs();
    }

    /**
     * Registers a global {@link ItemGroupEvents} modifier so late-registered modded tabs still
     * get enhanced-slab family insertion after their source slab.
     *
     * <p>Important: Fabric's {@code addAfter(...)} appends to the end when the anchor item is
     * missing. We therefore must first verify the source item is already present in the tab's
     * parent display list, otherwise the enhanced item would get appended into unrelated tabs and
     * show the blue creative-category lore for nearly every tab.</p>
     */
    private static void registerSourceTabs() {
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((group, entries) -> {
            Identifier tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(group);
            if (tabId != null && MOD_ID.equals(tabId.getNamespace())) {
                return;
            }

            Set<Item> parentDisplayItems = bitsandbalance$displayItems(entries);

            Item dogDisc = ModDogMusicDisc.musicDiscDog();
            if (FabricWorldgenConfig.isDogMusicDiscEnabled() && dogDisc != Items.AIR && parentDisplayItems.contains(Items.MUSIC_DISC_CAT)) {
                entries.addAfter(Items.MUSIC_DISC_CAT, java.util.List.of(new ItemStack(dogDisc)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }

            if (FabricTweaksConfig.isGeneratedEnhancedSlabContentEnabled()) {
                Set<Item> visibleItems = new HashSet<>(parentDisplayItems);
                for (EnhancedSlabCreativeTabFamilies.Family family : EnhancedSlabCreativeTabFamilies.snapshot(
                        FabricTweaksConfig.isVerticalSlabRuntimeEnabled(),
                        FabricTweaksConfig.isStepRuntimeEnabled())) {
                    bitsandbalance$addFamily(entries, visibleItems, family);
                }
            }
        });
    }

    private static void bitsandbalance$addFamily(net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries entries,
                                                 Set<Item> visibleItems,
                                                 EnhancedSlabCreativeTabFamilies.Family family) {
        if (entries == null || visibleItems == null || family == null) return;

        Item anchor = family.sourceSlab().asItem();
        if (anchor == Items.AIR || !visibleItems.contains(anchor)) return;

        Item insertedVerticalSlab = bitsandbalance$addAfterAnchor(entries, visibleItems, anchor, family.verticalSlab());
        if (insertedVerticalSlab != Items.AIR) {
            anchor = insertedVerticalSlab;
        }

        Item insertedStep = bitsandbalance$addAfterAnchor(entries, visibleItems, anchor, family.step());
        if (insertedStep != Items.AIR) {
            anchor = insertedStep;
        }

        bitsandbalance$addAfterAnchor(entries, visibleItems, anchor, family.verticalStep());
    }

    private static Item bitsandbalance$addAfterAnchor(net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries entries,
                                                      Set<Item> visibleItems,
                                                      Item anchor,
                                                      Block outputBlock) {
        if (entries == null || visibleItems == null || anchor == null || anchor == Items.AIR || outputBlock == null) {
            return Items.AIR;
        }

        Item outputItem = outputBlock.asItem();
        if (outputItem == Items.AIR || visibleItems.contains(outputItem)) {
            return Items.AIR;
        }

        entries.addAfter(anchor, java.util.List.of(new ItemStack(outputItem)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        visibleItems.add(outputItem);
        return outputItem;
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
