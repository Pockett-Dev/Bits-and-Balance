package org.onenonly.bitsandbalance.fabric.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.compat.BlockEntityTypeExt;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FabricStepContent {

    public static final ResourceLocation STEP_ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step");
    public static final ResourceLocation QUAD_STEP_ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "quad_step");
    private static final String NO_DYNAMIC_STEPS_RESOURCE = "data/bitsandbalance/tags/blocks/no_dynamic_steps.json";
    private static final Set<ResourceLocation> STARTUP_EXCLUDED_STEP_SLAB_IDS = bitsandbalance$loadStartupExcludedStepSlabIds();

    public static Block STEP;
    public static Block QUAD_STEP;

    private static boolean registered;

    private FabricStepContent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        StepBlock stepBlock = new StepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
                .noOcclusion()
            .sound(SoundType.STONE));
        STEP = Registry.register(BuiltInRegistries.BLOCK, STEP_ID, stepBlock);
        CommonBlocks.STEP = stepBlock;

        ResourceLocation proxyId = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "step_crack_proxy");
        StepCrackProxyBlock proxyBlock = new StepCrackProxyBlock(
            BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .strength(-1.0F));
        Registry.register(BuiltInRegistries.BLOCK, proxyId, proxyBlock);
        CommonBlocks.STEP_CRACK_PROXY = proxyBlock;

        QuadStepBlock quadBlock = new StorageQuadStepBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0F, 6.0F)
                .noOcclusion()
                .dynamicShape()
            .sound(SoundType.STONE));
        QUAD_STEP = Registry.register(BuiltInRegistries.BLOCK, QUAD_STEP_ID, quadBlock);
        CommonBlocks.QUAD_STEP = quadBlock;

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            return;
        }

        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation slabId = BuiltInRegistries.BLOCK.getKey(block);
            bitsandbalance$registerForSlabIfNeeded(slabId, block);
        }

        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, block) ->
                bitsandbalance$registerForSlabIfNeeded(id, block)
        );
    }

    private static void bitsandbalance$registerForSlabIfNeeded(ResourceLocation slabId, Block slabBlock) {
        if (slabBlock == null) return;
        if (!(slabBlock instanceof SlabBlock)) return;
        if (StepDynamicRegistry.getStepForSlab(slabBlock) != null) return;

        if (bitsandbalance$shouldSkipDynamicGeneration(slabId, slabBlock)) return;

        ResourceLocation stepId = StepDynamicRegistry.idForSlab(slabId, BitsAndBalanceCommon.MOD_ID);
        if (BuiltInRegistries.BLOCK.containsKey(stepId)) {
            Block existing = BuiltInRegistries.BLOCK.get(stepId);
            if (existing != null) {
                StepDynamicRegistry.register(slabBlock, existing);
                bitsandbalance$tryAddValidBlock(existing);
                bitsandbalance$tryRegisterStepItem(existing, slabBlock);
            }
            return;
        }

        FixedStepBlock fixed = bitsandbalance$createDynamicStepBlock(slabId, stepId, slabBlock);
        Block registeredBlock = bitsandbalance$registerDynamicStepBlock(slabId, stepId, fixed);
        StepDynamicRegistry.register(slabBlock, registeredBlock);
        bitsandbalance$tryAddValidBlock(registeredBlock);
        bitsandbalance$tryRegisterStepItem(registeredBlock, slabBlock);
    }

    private static FixedStepBlock bitsandbalance$createDynamicStepBlock(ResourceLocation slabId, ResourceLocation stepId, Block slabBlock) {
        try {
            BlockBehaviour.Properties stepProperties = DynamicVariantMaterialHelper.copySafeDynamicProperties(slabBlock)
                    .noOcclusion();
            if (DynamicVariantMaterialHelper.isCopperRandomlyTicking(slabBlock.defaultBlockState())) {
                stepProperties = stepProperties.randomTicks();
            }

            return new FixedStepBlock(slabBlock, stepProperties);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to create dynamic step block " + stepId + " from slab " + slabId, throwable);
        }
    }

    private static Block bitsandbalance$registerDynamicStepBlock(ResourceLocation slabId, ResourceLocation stepId, FixedStepBlock fixed) {
        try {
            return Registry.register(BuiltInRegistries.BLOCK, stepId, fixed);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to register dynamic step block " + stepId + " from slab " + slabId, throwable);
        }
    }

    private static boolean bitsandbalance$shouldSkipDynamicGeneration(ResourceLocation slabId, Block slabBlock) {
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
            String namespace = slabId.getNamespace();
            String path = slabId.getPath();
            String base = path.endsWith("_slab") ? path.substring(0, path.length() - "_slab".length()) : path;
            ResourceLocation candidate1 = ResourceLocation.fromNamespaceAndPath(namespace, base + "_step");
            if (BuiltInRegistries.BLOCK.containsKey(candidate1)) return true;
            ResourceLocation candidate2 = ResourceLocation.fromNamespaceAndPath(namespace, base.replace('/', '_') + "_step");
            return BuiltInRegistries.BLOCK.containsKey(candidate2);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Set<ResourceLocation> bitsandbalance$loadStartupExcludedStepSlabIds() {
        try (InputStream input = FabricStepContent.class.getClassLoader()
                .getResourceAsStream(NO_DYNAMIC_STEPS_RESOURCE)) {
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

    private static void bitsandbalance$tryRegisterStepItem(Block stepBlock, Block sourceSlab) {
        try {
            if (!(stepBlock instanceof FixedStepBlock)) return;
            ResourceLocation stepId = BuiltInRegistries.BLOCK.getKey(stepBlock);
            if (stepId == null) return;
            if (BuiltInRegistries.ITEM.containsKey(stepId)) return;
            Item.Properties props = new Item.Properties()
                    ;
            Registry.register(BuiltInRegistries.ITEM, stepId, new StepBlockItem(stepBlock, sourceSlab, props));

            ResourceLocation legacyAliasId = bitsandbalance$legacyMalformedStepItemId(stepId);
            if (legacyAliasId != null && !BuiltInRegistries.ITEM.containsKey(legacyAliasId)) {
                Item.Properties aliasProps = new Item.Properties()
                    ;
                Registry.register(BuiltInRegistries.ITEM, legacyAliasId,
                        new StepBlockItem(stepBlock, sourceSlab, aliasProps));
            }
        } catch (Throwable ignored) {
        }
    }

    private static ResourceLocation bitsandbalance$legacyMalformedStepItemId(ResourceLocation stepId) {
        String path = stepId.getPath();
        if (!path.matches(".*_step_\\d+$")) return null;
        return ResourceLocation.fromNamespaceAndPath(stepId.getNamespace(), path + "_step");
    }

    private static void bitsandbalance$tryAddValidBlock(Block stepBlock) {
        if (CommonBlockEntities.STEP == null) return;
        try {
            ((BlockEntityTypeExt) (Object) CommonBlockEntities.STEP)
                    .bitsandbalance$addValidBlocks(stepBlock);
        } catch (Throwable ignored) {
        }
    }
}
