package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class FabricBiomeItemTint {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-client");
    private static final int NO_TINT = 0xFFFFFFFF;
    private static final float LEAF_LITTER_ITEM_LIGHTEN_FACTOR = 1.03F;
    private static final Map<BakedModel, Set<Integer>> SOURCE_TINT_INDEX_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Item, ItemColor> FALLBACK_ITEM_COLORS = Collections.synchronizedMap(new IdentityHashMap<>());
    private static boolean initialized;

    private static final TagKey<Block> GRASS_TINT_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/grass"));
    private static final TagKey<Block> FOLIAGE_TINT_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/foliage"));
    private static final TagKey<Block> DISABLED_TINT_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/disabled"));
    private static final TagKey<Item> GRASS_TINT_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/grass"));
    private static final TagKey<Item> FOLIAGE_TINT_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/foliage"));
    private static final TagKey<Item> DISABLED_TINT_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("bitsandbalance", "biome_item_tint/disabled"));
    private static final ResourceLocation VINERY_GRASS_SLAB_ID = ResourceLocation.fromNamespaceAndPath("vinery", "grass_slab");
    private static final ResourceLocation GRASS_STEP_ID = ResourceLocation.fromNamespaceAndPath("bitsandbalance", "grass_step");
    private static final ResourceLocation VERTICAL_GRASS_STEP_ID = ResourceLocation.fromNamespaceAndPath("bitsandbalance", "vertical_grass_step");
    private static final ResourceLocation VERTICAL_GRASS_SLAB_ID = ResourceLocation.fromNamespaceAndPath("bitsandbalance", "vertical_grass_slab");
    private static final BiomeItemTintSettings TINT_SETTINGS = new BiomeItemTintSettings() {
        @Override public boolean enabled() { return FabricClientConfig.biomeItemTintEnabled; }
        @Override public boolean autoDetect() { return FabricClientConfig.biomeItemTintAutoDetect; }
        @Override public boolean isAllowed(ResourceLocation id) { return id != null && FabricClientConfig.biomeItemTintAllowedItemIds.contains(id); }
        @Override public boolean isGrassFallback(ResourceLocation id) { return id != null && FabricClientConfig.biomeItemTintGrassFallbackItemIds.contains(id); }
        @Override public boolean isFoliageFallback(ResourceLocation id) { return id != null && FabricClientConfig.biomeItemTintFoliageFallbackItemIds.contains(id); }
        @Override public boolean isDisabled(ResourceLocation id) { return id != null && FabricClientConfig.biomeItemTintDisabledItemIds.contains(id); }
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

        if (!FabricClientConfig.biomeItemTintEnabled && FabricClientConfig.leafLitterTintEnabled) {
            LOGGER.info("Biome item tinting disabled; leaving vanilla item tint providers intact");
            return;
        }

        List<Item> itemsToTint = new ArrayList<>();

        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            Item item = entry.getValue();
            if (!(item instanceof BlockItem blockItem)) {
                continue;
            }

            Block block = blockItem.getBlock();
            BlockState tintState = BiomeItemTintResolver.resolveTintState(block);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            ResourceLocation tintBlockId = BuiltInRegistries.BLOCK.getKey(tintState.getBlock());
            boolean forceUntintedLeafLitter = !FabricClientConfig.leafLitterTintEnabled
                && (BiomeItemTintResolver.isLeafLitterLike(itemId)
                    || BiomeItemTintResolver.isLeafLitterLike(blockId)
                    || BiomeItemTintResolver.isLeafLitterLike(tintBlockId));
            if (!forceUntintedLeafLitter
                && (!FabricClientConfig.biomeItemTintEnabled
                    || !BiomeItemTintResolver.isTintCandidate(item, block, tintState, itemId, blockId, tintBlockId, TINT_SETTINGS))) {
                continue;
            }

            itemsToTint.add(item);
            ItemColor fallbackColor = ColorProviderRegistry.ITEM.get(item);
            if (fallbackColor != null) {
                FALLBACK_ITEM_COLORS.put(item, fallbackColor);
            }
        }

        if (itemsToTint.isEmpty()) {
            LOGGER.warn("No block items were available for biome item tint registration");
            return;
        }

        ColorProviderRegistry.ITEM.register(FabricBiomeItemTint::getBiomeTint, itemsToTint.toArray(Item[]::new));
        LOGGER.info(
            "Registered biome-aware item tint provider for {} {} block items",
            itemsToTint.size(),
            FabricClientConfig.biomeItemTintAutoDetect ? "auto-detected" : "manually configured"
        );
    }

    private static int getBiomeTint(ItemStack stack, int tintIndex) {
        if (!FabricClientConfig.leafLitterTintEnabled && BiomeItemTintResolver.isLeafLitterLike(stack)) {
            return NO_TINT;
        }

        if (!FabricClientConfig.biomeItemTintEnabled) {
            return getFallbackItemTint(stack, tintIndex);
        }

        var context = BiomeItemTintResolver.resolveContext(stack, TINT_SETTINGS);
        if (context == null) {
            return NO_TINT;
        }

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

    private static int getFallbackItemTint(ItemStack stack, int tintIndex) {
        ItemColor fallbackColor = FALLBACK_ITEM_COLORS.get(stack.getItem());
        return fallbackColor == null ? NO_TINT : fallbackColor.getColor(stack, tintIndex);
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
        BakedModel model;
        try {
            model = Minecraft.getInstance().getBlockRenderer().getBlockModel(tintState);
        } catch (Throwable t) {
            return null;
        }
        if (model == null) {
            return null;
        }

        return SOURCE_TINT_INDEX_CACHE.computeIfAbsent(model, unused -> collectSourceTintIndices(model, tintState));
    }

    private static Set<Integer> collectSourceTintIndices(BakedModel model, BlockState state) {
        Set<Integer> tintIndices = new HashSet<>();
        collectTintIndices(model, state, null, tintIndices);
        for (Direction direction : Direction.values()) {
            collectTintIndices(model, state, direction, tintIndices);
        }
        return tintIndices;
    }

    private static void collectTintIndices(BakedModel model, BlockState state, Direction direction, Set<Integer> tintIndices) {
        List<BakedQuad> quads;
        try {
            quads = model.getQuads(state, direction, RandomSource.create(42L));
        } catch (Throwable t) {
            return;
        }
        if (quads == null) {
            return;
        }

        for (BakedQuad quad : quads) {
            if (quad != null && quad.getTintIndex() >= 0) {
                tintIndices.add(quad.getTintIndex());
            }
        }
    }

    private static int resolveFallbackColor(Context context) {
        FallbackTintType fallbackTintType = resolveFallbackTintType(context);
        if (fallbackTintType == FallbackTintType.GRASS) {
            return context.level() != null && context.pos() != null
                ? BiomeColors.getAverageGrassColor(context.level(), context.pos())
                : GrassColor.getDefaultColor();
        }

        if (fallbackTintType == FallbackTintType.FOLIAGE) {
            return context.level() != null && context.pos() != null
                ? BiomeColors.getAverageFoliageColor(context.level(), context.pos())
                : FoliageColor.getDefaultColor();
        }

        return -1;
    }

    private static Context resolveContext(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        Block block = blockItem.getBlock();
        BlockState tintState = resolveTintState(block);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation tintBlockId = BuiltInRegistries.BLOCK.getKey(tintState.getBlock());
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

    private static boolean isTintCandidate(Item item, Block block, BlockState tintState, ResourceLocation itemId, ResourceLocation blockId, ResourceLocation tintBlockId) {
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

    private static boolean isAutomaticTintCandidate(BlockState tintState, ResourceLocation itemId, ResourceLocation blockId, ResourceLocation tintBlockId) {
        return isGrassLike(itemId)
            || isGrassLike(blockId)
            || isGrassLike(tintBlockId)
            || isFoliageLike(tintState, tintBlockId)
            || isFoliageLike(null, blockId)
            || isFoliageLike(null, itemId);
    }

    private static boolean isDisabled(ResourceLocation id) {
        return id != null && FabricClientConfig.biomeItemTintDisabledItemIds.contains(id);
    }

    private static boolean isDisabled(Item item) {
        return item != null && item.builtInRegistryHolder().is(DISABLED_TINT_ITEMS);
    }

    private static boolean isDisabled(Block block) {
        return block != null && block.builtInRegistryHolder().is(DISABLED_TINT_BLOCKS);
    }

    private static boolean isAllowed(ResourceLocation itemId, ResourceLocation blockId, ResourceLocation tintBlockId) {
        return isAllowed(itemId)
            || isAllowed(blockId)
            || isAllowed(tintBlockId);
    }

    private static boolean isConfiguredCandidate(ResourceLocation itemId, ResourceLocation blockId, ResourceLocation tintBlockId) {
        return isAllowed(itemId, blockId, tintBlockId)
            || isGrassFallback(itemId)
            || isGrassFallback(blockId)
            || isGrassFallback(tintBlockId)
            || isFoliageFallback(itemId)
            || isFoliageFallback(blockId)
            || isFoliageFallback(tintBlockId);
    }

    private static boolean isAllowed(ResourceLocation id) {
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

    private static boolean isGrassFallback(ResourceLocation id) {
        return id != null && (FabricClientConfig.biomeItemTintGrassFallbackItemIds.contains(id) || isHardcodedGrassCompat(id));
    }

    private static boolean isHardcodedGrassCompat(ResourceLocation id) {
        return VINERY_GRASS_SLAB_ID.equals(id)
            || GRASS_STEP_ID.equals(id)
            || VERTICAL_GRASS_STEP_ID.equals(id)
            || VERTICAL_GRASS_SLAB_ID.equals(id);
    }

    private static boolean isFoliageFallback(ResourceLocation id) {
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

    private static boolean isGrassLike(ResourceLocation id) {
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        return path.equals("grass_block") || path.equals("short_grass") || path.equals("tall_grass");
    }

    private static boolean isFoliageLike(BlockState state, ResourceLocation id) {
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

    private static boolean isConfiguredFoliageFallbackLike(ResourceLocation id) {
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

    private static boolean isLeafLitterLike(ResourceLocation id) {
        return id != null && (id.getPath().equals("leaf_litter") || id.getPath().contains("leaf_pile"));
    }

    private static int lightenRgb(int color, float factor) {
        int red = Math.min(255, Math.round(((color >> 16) & 0xFF) * factor));
        int green = Math.min(255, Math.round(((color >> 8) & 0xFF) * factor));
        int blue = Math.min(255, Math.round((color & 0xFF) * factor));
        return (red << 16) | (green << 8) | blue;
    }

    private record Context(ClientLevel level, BlockPos pos, Item item, Block block, ResourceLocation itemId, ResourceLocation blockId, ResourceLocation tintBlockId, BlockState tintState) {
    }
}
