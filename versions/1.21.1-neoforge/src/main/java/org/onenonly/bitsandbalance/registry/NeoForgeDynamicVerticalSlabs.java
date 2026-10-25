package org.onenonly.bitsandbalance.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.items.VerticalSlabBlockItem;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Registers per-slab vertical slab blocks on NeoForge.
 */
public final class NeoForgeDynamicVerticalSlabs {
    private static final String NO_DYNAMIC_VERTICAL_SLABS_RESOURCE = "data/bitsandbalance/tags/block/no_dynamic_vertical_slabs.json";
    private static final Set<ResourceLocation> STARTUP_EXCLUDED_VERTICAL_SLAB_IDS = bitsandbalance$loadStartupExcludedVerticalSlabIds();

    private NeoForgeDynamicVerticalSlabs() {
    }

    public static void onRegisterBlocks(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)) {
            return;
        }

        for (Block slabBlock : bitsandbalance$sortedRegistryBlocks()) {
            ResourceLocation slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
            bitsandbalance$registerForSlabIfNeeded(event, slabId, slabBlock);
        }
    }

    public static void onRegisterItems(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) {
            return;
        }

        for (Block slabBlock : bitsandbalance$sortedRegistryBlocks()) {
            try {
                if (!(slabBlock instanceof SlabBlock)) continue;

                Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
                if (!(verticalBlock instanceof FixedVerticalSlabBlock)) continue;

                ResourceLocation verticalId = BuiltInRegistries.BLOCK.getKey(verticalBlock);
                if (verticalId == null) continue;
                if (BuiltInRegistries.ITEM.containsKey(verticalId)) continue;

                Item.Properties props = new Item.Properties();
                event.register(Registries.ITEM, verticalId, () -> new VerticalSlabBlockItem(verticalBlock, slabBlock, props));
            } catch (Throwable ignored) {
            }
        }
    }

    private static List<Block> bitsandbalance$sortedRegistryBlocks() {
        List<Block> blocks = new ArrayList<>();
        BuiltInRegistries.BLOCK.forEach(blocks::add);
        blocks.sort(Comparator
                .comparingInt((Block block) -> bitsandbalance$isVanillaBlock(block) ? 0 : 1)
                // Reverse resource-id order preserves the released fallback assignment
                // for the netherexp/infernalexp basalt slab collisions.
                .thenComparing(block -> {
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                    return id == null ? "" : id.toString();
                }, Comparator.<String>reverseOrder()));
        return blocks;
    }

    private static boolean bitsandbalance$isVanillaBlock(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && "minecraft".equals(id.getNamespace());
    }

    private static void bitsandbalance$registerForSlabIfNeeded(RegisterEvent event, ResourceLocation slabId, Block slabBlock) {
        try {
            if (slabBlock == null || slabId == null) return;
            if (!(slabBlock instanceof SlabBlock)) return;
            if (VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock) != null) return;

            // Compatibility: allow opting specific slabs out of dynamic vertical slab generation,
            // and skip generation if another mod already provides a vertical slab with a common id.
            if (bitsandbalance$shouldSkipDynamicGeneration(slabId, slabBlock)) return;

            ResourceLocation verticalId = VerticalSlabDynamicRegistry.idForSlab(slabId, BitsAndBalance.MODID);
            if (BuiltInRegistries.BLOCK.containsKey(verticalId)) {
                Block existing = BuiltInRegistries.BLOCK.get(verticalId);
                if (existing != null) {
                    VerticalSlabDynamicRegistry.register(slabBlock, existing);
                }
                return;
            }

            FixedVerticalSlabBlock verticalBlock = new FixedVerticalSlabBlock(
                    DynamicVariantMaterialHelper.copySafeDynamicProperties(slabBlock)
                            ,
                    slabBlock
            );

            event.register(Registries.BLOCK, verticalId, () -> verticalBlock);
            VerticalSlabDynamicRegistry.register(slabBlock, verticalBlock);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(ResourceLocation slabId, Block slabBlock) {
        try {
            if (slabId == null || slabBlock == null) return false;

            if ("minecraft".equals(slabId.getNamespace()) && "petrified_oak_slab".equals(slabId.getPath())) {
                return true;
            }

            if (STARTUP_EXCLUDED_VERTICAL_SLAB_IDS.contains(slabId)) {
                return true;
            }

            // Data-driven opt-out.
            try {
                if (slabBlock.builtInRegistryHolder().is(VerticalSlabDynamicRegistry.NO_DYNAMIC_VERTICAL_SLABS)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }

            // Heuristic opt-out: if the slab's own namespace already has a common "vertical_<slab>" id,
            // assume another mod is providing vertical slabs and avoid generating duplicates.
            String ns = slabId.getNamespace();
            String path = slabId.getPath();

            ResourceLocation candidate1 = ResourceLocation.fromNamespaceAndPath(ns, "vertical_" + path);
            if (BuiltInRegistries.BLOCK.containsKey(candidate1)) return true;

            ResourceLocation candidate2 = ResourceLocation.fromNamespaceAndPath(ns, "vertical_" + path.replace('/', '_'));
            return BuiltInRegistries.BLOCK.containsKey(candidate2);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Set<ResourceLocation> bitsandbalance$loadStartupExcludedVerticalSlabIds() {
        try (InputStream input = NeoForgeDynamicVerticalSlabs.class.getClassLoader()
                .getResourceAsStream(NO_DYNAMIC_VERTICAL_SLABS_RESOURCE)) {
            if (input == null) return Collections.emptySet();

            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonArray values = root.getAsJsonArray("values");
            if (values == null || values.isEmpty()) return Collections.emptySet();

            Set<ResourceLocation> excludedIds = new HashSet<>();
            for (JsonElement value : values) {
                ResourceLocation blockId = bitsandbalance$parseExcludedBlockId(value);
                if (blockId != null) {
                    excludedIds.add(blockId);
                }
            }
            return Collections.unmodifiableSet(excludedIds);
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }

    private static ResourceLocation bitsandbalance$parseExcludedBlockId(JsonElement value) {
        try {
            if (value == null || value.isJsonNull()) return null;

            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                return ResourceLocation.tryParse(value.getAsString());
            }

            if (!value.isJsonObject()) return null;

            JsonObject object = value.getAsJsonObject();
            JsonElement id = object.get("id");
            if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()) {
                return null;
            }
            return ResourceLocation.tryParse(id.getAsString());
        } catch (Throwable ignored) {
            return null;
        }
    }

}

