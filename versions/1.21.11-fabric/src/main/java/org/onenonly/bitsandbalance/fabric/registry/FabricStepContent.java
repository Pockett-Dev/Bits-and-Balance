package org.onenonly.bitsandbalance.fabric.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.StorageQuadStepBlock;
import org.onenonly.bitsandbalance.common.items.StepBlockItem;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Fabric-only registration for step blocks. */
public final class FabricStepContent {

    public static final Identifier STEP_ID    = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step");
    public static final Identifier QUAD_STEP_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "quad_step");
    private static final String NO_DYNAMIC_STEPS_RESOURCE = "data/bitsandbalance/tags/blocks/no_dynamic_steps.json";
    private static final Set<Identifier> STARTUP_EXCLUDED_STEP_SLAB_IDS = bitsandbalance$loadStartupExcludedStepSlabIds();

    public static Block STEP;
    public static Block QUAD_STEP;

    private static boolean registered;

    private FabricStepContent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        // Generic step block (kept for backwards compatibility and as a base type).
        StepBlock stepBlock = new StepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, STEP_ID)));
        STEP = Registry.register(BuiltInRegistries.BLOCK, STEP_ID, stepBlock);
        CommonBlocks.STEP = stepBlock;

        Identifier proxyId = Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step_crack_proxy");
        StepCrackProxyBlock proxyBlock = new StepCrackProxyBlock(
            BlockBehaviour.Properties.of()
                .noCollision()
                .noOcclusion()
                .strength(-1.0F)
                .setId(ResourceKey.create(Registries.BLOCK, proxyId)));
        Registry.register(BuiltInRegistries.BLOCK, proxyId, proxyBlock);
        CommonBlocks.STEP_CRACK_PROXY = proxyBlock;

        // Quad step container block.
        QuadStepBlock quadBlock = new StorageQuadStepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
                .sound(SoundType.STONE)
                .dynamicShape()
                .setId(ResourceKey.create(Registries.BLOCK, QUAD_STEP_ID)));
        QUAD_STEP = Registry.register(BuiltInRegistries.BLOCK, QUAD_STEP_ID, quadBlock);
        CommonBlocks.QUAD_STEP = quadBlock;

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            return;
        }

        // Dynamic per-slab step blocks: initial scan.
        for (Block b : BuiltInRegistries.BLOCK) {
            Identifier slabId = BuiltInRegistries.BLOCK.getKey(b);
            bitsandbalance$registerForSlabIfNeeded(slabId, b);
        }

        // Late-registered modded slabs.
        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, b) ->
                bitsandbalance$registerForSlabIfNeeded(id, b)
        );
    }

    private static void bitsandbalance$registerForSlabIfNeeded(Identifier slabId, Block slabBlock) {
        try {
            if (slabBlock == null) return;
            if (!(slabBlock instanceof SlabBlock)) return;
            if (StepDynamicRegistry.getStepForSlab(slabBlock) != null) return;

            if (bitsandbalance$shouldSkipDynamicGeneration(slabId, slabBlock)) return;

            Identifier stepId = StepDynamicRegistry.idForSlab(slabId, BitsAndBalanceCommon.MOD_ID);
            if (BuiltInRegistries.BLOCK.containsKey(stepId)) {
                Block existing = BuiltInRegistries.BLOCK.getValue(stepId);
                if (existing != null) {
                    StepDynamicRegistry.register(slabBlock, existing);
                    bitsandbalance$tryRegisterStepItem(existing, slabBlock);
                }
                return;
            }

            BlockBehaviour.Properties stepProperties = DynamicVariantMaterialHelper.copySafeDynamicProperties(slabBlock)
                    .setId(ResourceKey.create(Registries.BLOCK, stepId));
            if (DynamicVariantMaterialHelper.isCopperRandomlyTicking(slabBlock.defaultBlockState())) {
                stepProperties = stepProperties.randomTicks();
            }

            FixedStepBlock fixed = new FixedStepBlock(slabBlock, stepProperties);
            Block registered = Registry.register(BuiltInRegistries.BLOCK, stepId, fixed);
            StepDynamicRegistry.register(slabBlock, registered);
            bitsandbalance$tryRegisterStepItem(registered, slabBlock);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(Identifier slabId, Block slabBlock) {
        try {
            if (slabId == null || slabBlock == null) return false;

            if ("minecraft".equals(slabId.getNamespace()) && "petrified_oak_slab".equals(slabId.getPath())) {
                return true;
            }

            if (STARTUP_EXCLUDED_STEP_SLAB_IDS.contains(slabId)) {
                return true;
            }

            try {
                if (slabBlock.builtInRegistryHolder().is(StepDynamicRegistry.NO_DYNAMIC_STEPS)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
            String ns   = slabId.getNamespace();
            String path = slabId.getPath();
            String base = path.endsWith("_slab") ? path.substring(0, path.length() - "_slab".length()) : path;
            Identifier c1 = Identifier.fromNamespaceAndPath(ns, base + "_step");
            if (BuiltInRegistries.BLOCK.containsKey(c1)) return true;
            Identifier c2 = Identifier.fromNamespaceAndPath(ns, base.replace('/', '_') + "_step");
            return BuiltInRegistries.BLOCK.containsKey(c2);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Set<Identifier> bitsandbalance$loadStartupExcludedStepSlabIds() {
        try (InputStream input = FabricStepContent.class.getClassLoader()
                .getResourceAsStream(NO_DYNAMIC_STEPS_RESOURCE)) {
            if (input == null) return Collections.emptySet();

            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonArray values = root.getAsJsonArray("values");
            if (values == null || values.isEmpty()) return Collections.emptySet();

            Set<Identifier> excludedIds = new HashSet<>();
            for (JsonElement value : values) {
                Identifier blockId = bitsandbalance$parseExcludedBlockId(value);
                if (blockId != null) {
                    excludedIds.add(blockId);
                }
            }
            return Collections.unmodifiableSet(excludedIds);
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }

    private static Identifier bitsandbalance$parseExcludedBlockId(JsonElement value) {
        try {
            if (value == null || value.isJsonNull()) return null;

            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                return Identifier.tryParse(value.getAsString());
            }

            if (!value.isJsonObject()) return null;

            JsonObject object = value.getAsJsonObject();
            JsonElement id = object.get("id");
            if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()) {
                return null;
            }
            return Identifier.tryParse(id.getAsString());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void bitsandbalance$tryRegisterStepItem(Block stepBlock, Block sourceSlab) {
        try {
            if (!(stepBlock instanceof FixedStepBlock)) return;
            Identifier stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
            if (stepId == null) return;
            if (BuiltInRegistries.ITEM.containsKey(stepId)) return;
            Item.Properties props = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, stepId));
            Registry.register(BuiltInRegistries.ITEM, stepId, new StepBlockItem(stepBlock, sourceSlab, props));

            Identifier legacyAliasId = bitsandbalance$legacyMalformedStepItemId(stepId);
            if (legacyAliasId != null && !BuiltInRegistries.ITEM.containsKey(legacyAliasId)) {
                Item.Properties aliasProps = new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, legacyAliasId));
                Registry.register(BuiltInRegistries.ITEM, legacyAliasId,
                        new StepBlockItem(stepBlock, sourceSlab, aliasProps));
            }
        } catch (Throwable ignored) {
        }
    }

    private static Identifier bitsandbalance$legacyMalformedStepItemId(Identifier stepId) {
        String path = stepId.getPath();
        if (!path.matches(".*_step_\\d+$")) return null;
        return Identifier.fromNamespaceAndPath(stepId.getNamespace(), path + "_step");
    }

}
