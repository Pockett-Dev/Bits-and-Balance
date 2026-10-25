package org.onenonly.bitsandbalance.fabric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.BiomeItemTintResolver;
import org.onenonly.bitsandbalance.common.client.BiomeItemTintSettings;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.onenonly.bitsandbalance.mixin.ItemStackRenderStateAccessor;
import org.onenonly.bitsandbalance.fabric.mixin.ItemStackRenderStateLayerAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class FabricBiomeItemTint {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-client");
    private static final int NO_TINT = 0xFFFFFFFF;
    private static final float LEAF_LITTER_ITEM_LIGHTEN_FACTOR = 1.03F;
    private static final Map<BlockStateModel, Set<Integer>> SOURCE_TINT_INDEX_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean initialized;

    private static final TagKey<Block> GRASS_TINT_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/grass"));
    private static final TagKey<Block> FOLIAGE_TINT_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/foliage"));
    private static final TagKey<Block> DISABLED_TINT_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/disabled"));
    private static final TagKey<Item> GRASS_TINT_ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/grass"));
    private static final TagKey<Item> FOLIAGE_TINT_ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/foliage"));
    private static final TagKey<Item> DISABLED_TINT_ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/disabled"));
    private static final Identifier VINERY_GRASS_SLAB_ID = Identifier.fromNamespaceAndPath("vinery", "grass_slab");
    private static final Identifier GRASS_STEP_ID = Identifier.fromNamespaceAndPath("bitsandbalance", "grass_step");
    private static final Identifier VERTICAL_GRASS_STEP_ID = Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_grass_step");
    private static final Identifier VERTICAL_GRASS_SLAB_ID = Identifier.fromNamespaceAndPath("bitsandbalance", "vertical_grass_slab");
    private static final BiomeItemTintSettings TINT_SETTINGS = new BiomeItemTintSettings() {
        @Override public boolean enabled() { return FabricClientConfig.biomeItemTintEnabled; }
        @Override public boolean autoDetect() { return FabricClientConfig.biomeItemTintAutoDetect; }
        @Override public boolean isAllowed(Identifier id) { return id != null && FabricClientConfig.biomeItemTintAllowedItemIds.contains(id); }
        @Override public boolean isGrassFallback(Identifier id) { return id != null && FabricClientConfig.biomeItemTintGrassFallbackItemIds.contains(id); }
        @Override public boolean isFoliageFallback(Identifier id) { return id != null && FabricClientConfig.biomeItemTintFoliageFallbackItemIds.contains(id); }
        @Override public boolean isDisabled(Identifier id) { return id != null && FabricClientConfig.biomeItemTintDisabledItemIds.contains(id); }
    };

    private enum FallbackTintType {
        NONE,
        GRASS,
        FOLIAGE
    }

    private FabricBiomeItemTint() {
    }

    public static void initClient() {
        if (initialized) {
            return;
        }
        initialized = true;

        List<Item> itemsToTint = new ArrayList<>();

        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            Item item = entry.getValue();
            if (!(item instanceof BlockItem blockItem)) {
                continue;
            }

            Block block = blockItem.getBlock();
            BlockState tintState = BiomeItemTintResolver.resolveTintState(block);
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            Identifier tintBlockId = BuiltInRegistries.BLOCK.getKey(tintState.getBlock());
            if (!BiomeItemTintResolver.isTintCandidate(item, block, tintState, itemId, blockId, tintBlockId, TINT_SETTINGS)) {
                continue;
            }

            itemsToTint.add(item);
        }

        if (itemsToTint.isEmpty()) {
            LOGGER.warn("No block items were available for biome item tint registration");
            return;
        }

        LOGGER.info(
            "Prepared biome-aware item tinting for {} {} block items",
            itemsToTint.size(),
            FabricClientConfig.biomeItemTintAutoDetect ? "auto-detected" : "manually configured"
        );
    }

    public static void applyRenderStateTint(ItemStackRenderState renderState, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return;
        }

        if (!FabricClientConfig.leafLitterTintEnabled && BiomeItemTintResolver.isLeafLitterLike(stack)) {
            applyFlatTint(renderState, NO_TINT);
            return;
        }

        if (!FabricClientConfig.biomeItemTintEnabled) {
            applyVanillaDisabledFallbackTint(renderState, stack);
            return;
        }

        var context = BiomeItemTintResolver.resolveContext(stack, TINT_SETTINGS);
        if (context == null) {
            return;
        }

        renderState.setAnimated();

        ItemStackRenderStateAccessor stateAccessor;
        try {
            stateAccessor = (ItemStackRenderStateAccessor) renderState;
        } catch (Throwable t) {
            return;
        }

        ItemStackRenderState.LayerRenderState[] layers;
        int activeLayerCount;
        try {
            layers = stateAccessor.bitsandbalance$getLayers();
            activeLayerCount = stateAccessor.bitsandbalance$getActiveLayerCount();
        } catch (Throwable t) {
            return;
        }
        if (layers == null || activeLayerCount <= 0) {
            return;
        }

        int limit = Math.min(activeLayerCount, layers.length);
        for (int i = 0; i < limit; i++) {
            ItemStackRenderState.LayerRenderState layer = layers[i];
            if (layer == null) {
                continue;
            }

            List<BakedQuad> quads;
            try {
                quads = ((ItemStackRenderStateLayerAccessor) (Object) layer).bitsandbalance$getQuads();
            } catch (Throwable t) {
                continue;
            }
            if (quads == null || quads.isEmpty()) {
                continue;
            }

            int maxTintIndex = -1;
            for (BakedQuad quad : quads) {
                if (quad != null && quad.tintIndex() > maxTintIndex) {
                    maxTintIndex = quad.tintIndex();
                }
            }
            if (maxTintIndex < 0) {
                continue;
            }

            boolean hasTint = false;
            int[] tintLayers = layer.prepareTintLayers(maxTintIndex + 1);
            for (int tintIndex = 0; tintIndex <= maxTintIndex; tintIndex++) {
                int color = getBiomeTint(context, tintIndex);
                if (color != NO_TINT) {
                    hasTint = true;
                }
                tintLayers[tintIndex] = color;
            }
            if (!hasTint) {
                continue;
            }
        }
    }

    private static void applyVanillaDisabledFallbackTint(ItemStackRenderState renderState, ItemStack stack) {
        int color = getVanillaDisabledFallbackColor(stack);
        if (color == NO_TINT) {
            return;
        }

        applyFlatTint(renderState, color);
    }

    private static void applyFlatTint(ItemStackRenderState renderState, int color) {
        ItemStackRenderStateAccessor stateAccessor;
        try {
            stateAccessor = (ItemStackRenderStateAccessor) renderState;
        } catch (Throwable t) {
            return;
        }

        ItemStackRenderState.LayerRenderState[] layers;
        int activeLayerCount;
        try {
            layers = stateAccessor.bitsandbalance$getLayers();
            activeLayerCount = stateAccessor.bitsandbalance$getActiveLayerCount();
        } catch (Throwable t) {
            return;
        }
        if (layers == null || activeLayerCount <= 0) {
            return;
        }

        int limit = Math.min(activeLayerCount, layers.length);
        boolean appliedTint = false;
        for (int i = 0; i < limit; i++) {
            ItemStackRenderState.LayerRenderState layer = layers[i];
            if (layer == null) {
                continue;
            }

            List<BakedQuad> quads;
            try {
                quads = ((ItemStackRenderStateLayerAccessor) (Object) layer).bitsandbalance$getQuads();
            } catch (Throwable t) {
                continue;
            }
            if (quads == null || quads.isEmpty()) {
                continue;
            }

            int maxTintIndex = -1;
            for (BakedQuad quad : quads) {
                if (quad != null && quad.tintIndex() > maxTintIndex) {
                    maxTintIndex = quad.tintIndex();
                }
            }
            if (maxTintIndex < 0) {
                continue;
            }

            int[] tintLayers = layer.prepareTintLayers(maxTintIndex + 1);
            for (int tintIndex = 0; tintIndex <= maxTintIndex; tintIndex++) {
                tintLayers[tintIndex] = color;
            }
            appliedTint = true;
        }

        if (appliedTint) {
            renderState.setAnimated();
        }
    }

    private static int getVanillaDisabledFallbackColor(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return NO_TINT;
        }

        Block block = blockItem.getBlock();
        BlockState tintState = resolveTintState(block);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        Identifier tintBlockId = BuiltInRegistries.BLOCK.getKey(tintState.getBlock());
        int color = vanillaDisabledFallbackColor(itemId);
        if (color == NO_TINT) color = vanillaDisabledFallbackColor(blockId);
        if (color == NO_TINT) color = vanillaDisabledFallbackColor(tintBlockId);
        return color;
    }

    private static int vanillaDisabledFallbackColor(Identifier id) {
        if (id == null || !id.getNamespace().equals("minecraft")) {
            return NO_TINT;
        }

        return switch (id.getPath()) {
            case "grass_block", "grass", "short_grass", "tall_grass", "fern", "large_fern", "potted_fern", "sugar_cane" ->
                0xFF000000 | (GrassColor.get(0.5D, 1.0D) & 0xFFFFFF);
            case "oak_leaves", "jungle_leaves", "acacia_leaves", "vine" ->
                0xFF000000 | (FoliageColor.get(0.5D, 1.0D) & 0xFFFFFF);
            case "spruce_leaves" ->
                0xFF000000 | (FoliageColor.FOLIAGE_EVERGREEN & 0xFFFFFF);
            case "birch_leaves" ->
                0xFF000000 | (FoliageColor.FOLIAGE_BIRCH & 0xFFFFFF);
            case "mangrove_leaves" ->
                0xFF000000 | (FoliageColor.FOLIAGE_MANGROVE & 0xFFFFFF);
            case "leaf_litter" ->
                0xFF000000 | (lightenRgb(FoliageColor.get(0.5D, 1.0D), LEAF_LITTER_ITEM_LIGHTEN_FACTOR) & 0xFFFFFF);
            default -> NO_TINT;
        };
    }

    private static int getBiomeTint(ItemStack stack, int tintIndex) {
        if (!FabricClientConfig.biomeItemTintEnabled) {
            return NO_TINT;
        }

        var context = BiomeItemTintResolver.resolveContext(stack, TINT_SETTINGS);
        if (context == null) {
            return NO_TINT;
        }

        return getBiomeTint(context, tintIndex);
    }

    private static int getBiomeTint(BiomeItemTintResolver.Context context, int tintIndex) {
        if (!BiomeItemTintResolver.shouldUseTintIndex(context, tintIndex, FabricBiomeItemTint::sourceTintIndexStatus)) {
            return NO_TINT;
        }

        Minecraft client = Minecraft.getInstance();
        BlockColors blockColors = client.getBlockColors();
        BiomeItemTintResolver.BlockTintSampler sampler = blockColors == null
            ? null
            : (state, level, pos, index) -> blockColors.getColor(state, level, pos, index);
        int color = -1;
        if (blockColors != null) {
            color = blockColors.getColor(context.tintState(), context.level(), context.pos(), tintIndex);
        }

        if (color == -1) {
            color = BiomeItemTintResolver.resolveFallbackColor(context, TINT_SETTINGS, tintIndex, sampler);
        }

        if (color == -1) {
            return NO_TINT;
        }

        return BiomeItemTintResolver.finalizeColor(context, color);
    }

    private static BiomeItemTintResolver.SourceTintIndexStatus sourceTintIndexStatus(BiomeItemTintResolver.Context context, int tintIndex) {
        Set<Integer> tintIndices = sourceTintIndices(context.tintState());
        if (tintIndices == null) {
            return BiomeItemTintResolver.SourceTintIndexStatus.UNKNOWN;
        }
        return tintIndices.contains(tintIndex)
            ? BiomeItemTintResolver.SourceTintIndexStatus.PRESENT
            : BiomeItemTintResolver.SourceTintIndexStatus.ABSENT;
    }

    private static Set<Integer> sourceTintIndices(BlockState tintState) {
        BlockStateModel model;
        try {
            model = Minecraft.getInstance().getBlockRenderer().getBlockModel(tintState);
        } catch (Throwable t) {
            return null;
        }
        if (model == null) {
            return null;
        }

        return SOURCE_TINT_INDEX_CACHE.computeIfAbsent(model, unused -> collectSourceTintIndices(model));
    }

    private static Set<Integer> collectSourceTintIndices(BlockStateModel model) {
        Set<Integer> tintIndices = new HashSet<>();
        List<BlockModelPart> parts;
        try {
            parts = model.collectParts(RandomSource.create(42L));
        } catch (Throwable t) {
            return tintIndices;
        }
        if (parts == null) {
            return tintIndices;
        }

        for (BlockModelPart part : parts) {
            collectTintIndices(part, null, tintIndices);
            for (Direction direction : Direction.values()) {
                collectTintIndices(part, direction, tintIndices);
            }
        }
        return tintIndices;
    }

    private static void collectTintIndices(BlockModelPart part, Direction direction, Set<Integer> tintIndices) {
        List<BakedQuad> quads;
        try {
            quads = part.getQuads(direction);
        } catch (Throwable t) {
            return;
        }
        if (quads == null) {
            return;
        }

        for (BakedQuad quad : quads) {
            if (quad != null && quad.tintIndex() >= 0) {
                tintIndices.add(quad.tintIndex());
            }
        }
    }

    private static int resolveFallbackColor(Context context) {
        FallbackTintType fallbackTintType = resolveFallbackTintType(context);
        if (fallbackTintType == FallbackTintType.GRASS) {
            return context.level() != null && context.pos() != null
                ? BiomeColors.getAverageGrassColor(context.level(), context.pos())
                : GrassColor.get(0.5D, 1.0D);
        }

        if (fallbackTintType == FallbackTintType.FOLIAGE) {
            return context.level() != null && context.pos() != null
                ? BiomeColors.getAverageFoliageColor(context.level(), context.pos())
                : FoliageColor.get(0.5D, 1.0D);
        }

        return -1;
    }

    private static Context resolveContext(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        Block block = blockItem.getBlock();
        BlockState tintState = resolveTintState(block);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        Identifier tintBlockId = BuiltInRegistries.BLOCK.getKey(tintState.getBlock());
        if (!isTintCandidate(stack.getItem(), block, tintState, itemId, blockId, tintBlockId)) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            return null;
        }

        return new Context(level, client.player.blockPosition(), stack.getItem(), block, itemId, blockId, tintBlockId, tintState);
    }

    private static boolean isTintCandidate(Item item, Block block, BlockState tintState, Identifier itemId, Identifier blockId, Identifier tintBlockId) {
        if ((blockId == null && tintBlockId == null)
            || isDisabled(itemId)
            || isDisabled(blockId)
            || isDisabled(tintBlockId)
            || isDisabled(item)
            || isDisabled(block)
            || isDisabled(tintState.getBlock())) {
            return false;
        }

        if (FabricClientConfig.biomeItemTintAutoDetect) {
            return isConfiguredCandidate(itemId, blockId, tintBlockId)
                || isTaggedTintCandidate(item, block, tintState)
                || isAutomaticTintCandidate(tintState, itemId, blockId, tintBlockId);
        }

        return isConfiguredCandidate(itemId, blockId, tintBlockId) || isTaggedTintCandidate(item, block, tintState);
    }

    private static boolean isTaggedTintCandidate(Item item, Block block, BlockState tintState) {
        return isGrassTagged(item, block, tintState) || isFoliageTagged(item, block, tintState);
    }

    private static boolean isAutomaticTintCandidate(BlockState tintState, Identifier itemId, Identifier blockId, Identifier tintBlockId) {
        return isGrassLike(itemId)
            || isGrassLike(blockId)
            || isGrassLike(tintBlockId)
            || isFoliageLike(tintState, tintBlockId)
            || isFoliageLike(null, blockId)
            || isFoliageLike(null, itemId);
    }

    private static boolean isDisabled(Identifier id) {
        return id != null && FabricClientConfig.biomeItemTintDisabledItemIds.contains(id);
    }

    private static boolean isDisabled(Item item) {
        return item != null && item.builtInRegistryHolder().is(DISABLED_TINT_ITEMS);
    }

    private static boolean isDisabled(Block block) {
        return block != null && block.builtInRegistryHolder().is(DISABLED_TINT_BLOCKS);
    }

    private static boolean isAllowed(Identifier itemId, Identifier blockId, Identifier tintBlockId) {
        return isAllowed(itemId)
            || isAllowed(blockId)
            || isAllowed(tintBlockId);
    }

    private static boolean isConfiguredCandidate(Identifier itemId, Identifier blockId, Identifier tintBlockId) {
        return isAllowed(itemId, blockId, tintBlockId)
            || isGrassFallback(itemId)
            || isGrassFallback(blockId)
            || isGrassFallback(tintBlockId)
            || isFoliageFallback(itemId)
            || isFoliageFallback(blockId)
            || isFoliageFallback(tintBlockId);
    }

    private static boolean isAllowed(Identifier id) {
        return id != null && FabricClientConfig.biomeItemTintAllowedItemIds.contains(id);
    }

    private static FallbackTintType resolveFallbackTintType(Context context) {
        if (isGrassFallback(context.itemId())) {
            return FallbackTintType.GRASS;
        }
        if (isFoliageFallback(context.itemId())) {
            return FallbackTintType.FOLIAGE;
        }
        if (isGrassFallback(context.tintBlockId()) || isGrassFallback(context.blockId())) {
            return FallbackTintType.GRASS;
        }
        if (isFoliageFallback(context.tintBlockId()) || isFoliageFallback(context.blockId())) {
            return FallbackTintType.FOLIAGE;
        }

        if (isGrassTagged(context.item(), context.block(), context.tintState())) {
            return FallbackTintType.GRASS;
        }
        if (isFoliageTagged(context.item(), context.block(), context.tintState())) {
            return FallbackTintType.FOLIAGE;
        }

        FallbackTintType configuredFallbackTintType = detectConfiguredFallbackTintType(context);
        if (configuredFallbackTintType != FallbackTintType.NONE) {
            return configuredFallbackTintType;
        }

        if (!FabricClientConfig.biomeItemTintAutoDetect) {
            return FallbackTintType.NONE;
        }

        return detectAutomaticFallbackTintType(context);
    }

    private static FallbackTintType detectAutomaticFallbackTintType(Context context) {
        if (isGrassLike(context.tintBlockId()) || isGrassLike(context.blockId()) || isGrassLike(context.itemId())) {
            return FallbackTintType.GRASS;
        }

        if (isFoliageLike(context.tintState(), context.tintBlockId())
            || isFoliageLike(null, context.blockId())
            || isFoliageLike(null, context.itemId())) {
            return FallbackTintType.FOLIAGE;
        }

        return FallbackTintType.NONE;
    }

    private static FallbackTintType detectConfiguredFallbackTintType(Context context) {
        if (!isAllowed(context.itemId(), context.blockId(), context.tintBlockId())) {
            return FallbackTintType.NONE;
        }

        if (isGrassLike(context.tintBlockId()) || isGrassLike(context.blockId()) || isGrassLike(context.itemId())) {
            return FallbackTintType.GRASS;
        }

        if (isConfiguredFoliageFallbackLike(context.tintBlockId())
            || isConfiguredFoliageFallbackLike(context.blockId())
            || isConfiguredFoliageFallbackLike(context.itemId())) {
            return FallbackTintType.FOLIAGE;
        }

        return FallbackTintType.NONE;
    }

    private static boolean isGrassFallback(Identifier id) {
        return id != null && (FabricClientConfig.biomeItemTintGrassFallbackItemIds.contains(id) || isHardcodedGrassCompat(id));
    }

    private static boolean isHardcodedGrassCompat(Identifier id) {
        return VINERY_GRASS_SLAB_ID.equals(id)
            || GRASS_STEP_ID.equals(id)
            || VERTICAL_GRASS_STEP_ID.equals(id)
            || VERTICAL_GRASS_SLAB_ID.equals(id);
    }

    private static boolean isFoliageFallback(Identifier id) {
        return id != null && FabricClientConfig.biomeItemTintFoliageFallbackItemIds.contains(id);
    }

    private static boolean isGrassTagged(Item item, Block block, BlockState tintState) {
        return (item != null && item.builtInRegistryHolder().is(GRASS_TINT_ITEMS))
            || (block != null && block.builtInRegistryHolder().is(GRASS_TINT_BLOCKS))
            || (tintState != null && tintState.getBlock().builtInRegistryHolder().is(GRASS_TINT_BLOCKS));
    }

    private static boolean isFoliageTagged(Item item, Block block, BlockState tintState) {
        return (item != null && item.builtInRegistryHolder().is(FOLIAGE_TINT_ITEMS))
            || (block != null && block.builtInRegistryHolder().is(FOLIAGE_TINT_BLOCKS))
            || (tintState != null && tintState.getBlock().builtInRegistryHolder().is(FOLIAGE_TINT_BLOCKS));
    }

    private static boolean isGrassLike(Identifier id) {
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        return path.contains("grass") || path.contains("fern");
    }

    private static boolean isFoliageLike(BlockState state, Identifier id) {
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        return isBiomeTintedLeafPath(path)
            || path.equals("leaf_litter")
            || path.contains("leaf_pile")
            || isVineLikePath(path)
            || path.contains("ivy")
            || path.equals("bush")
            || path.contains("hedge");
    }

    private static boolean isConfiguredFoliageFallbackLike(Identifier id) {
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        return path.endsWith("_leaves")
            || path.endsWith("_leaf")
            || path.equals("leaf_litter")
            || path.contains("leaf_pile")
            || isVineLikePath(path)
            || path.contains("ivy")
            || path.equals("bush")
            || path.contains("hedge");
    }

    private static boolean isVineLikePath(String path) {
        return path.equals("vine")
            || path.endsWith("_vine")
            || path.contains("_vine_")
            || path.equals("grapevine_leaves")
            || path.equals("grapevine_stem");
    }

    private static boolean isBiomeTintedLeafPath(String path) {
        if (!path.endsWith("_leaves") && !path.endsWith("_leaf")) {
            return false;
        }

        if (path.contains("cherry")
            || path.contains("spruce")
            || path.contains("birch")
            || path.contains("azalea")
            || path.contains("pale_oak")) {
            return false;
        }

        return path.contains("oak")
            || path.contains("jungle")
            || path.contains("acacia")
            || path.contains("mangrove");
    }

    private static BlockState resolveTintState(Block block) {
        if (block instanceof FixedStepBlock fixedStepBlock) {
            return fixedStepBlock.getSourceSlab().defaultBlockState();
        }

        if (block instanceof FixedVerticalSlabBlock fixedVerticalSlabBlock) {
            return fixedVerticalSlabBlock.getSourceSlab().defaultBlockState();
        }

        if (block instanceof FixedVerticalStepBlock fixedVerticalStepBlock) {
            return resolveTintState(fixedVerticalStepBlock.getSourceVerticalSlab());
        }

        Block sourceSlab = StepDynamicRegistry.getSlabForStep(block);
        if (sourceSlab != null) {
            return sourceSlab.defaultBlockState();
        }

        sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(block);
        if (sourceSlab != null) {
            return sourceSlab.defaultBlockState();
        }

        Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block);
        if (sourceVerticalSlab != null) {
            return resolveTintState(sourceVerticalSlab);
        }

        return block.defaultBlockState();
    }

    private static boolean isLeafLitterLike(Identifier id) {
        return id != null && (id.getPath().equals("leaf_litter") || id.getPath().contains("leaf_pile"));
    }

    private static int lightenRgb(int color, float factor) {
        int red = Math.min(255, Math.round(((color >> 16) & 0xFF) * factor));
        int green = Math.min(255, Math.round(((color >> 8) & 0xFF) * factor));
        int blue = Math.min(255, Math.round((color & 0xFF) * factor));
        return (red << 16) | (green << 8) | blue;
    }

    private record Context(ClientLevel level, BlockPos pos, Item item, Block block, Identifier itemId, Identifier blockId, Identifier tintBlockId, BlockState tintState) {
    }
}
