package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
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
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.HashMap;
import java.util.Map;

public final class BiomeItemTintResolver {
    public static final int NO_TINT = 0xFFFFFFFF;

    private static final float LEAF_LITTER_ITEM_LIGHTEN_FACTOR = 1.03F;
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
    private static final int[] BIOME_PROBE_OFFSETS = {
        0,
        16,
        -16,
        32,
        -32,
        64,
        -64,
        128,
        -128,
        256,
        -256
    };
    private static final Map<BiomeTintProbeKey, Boolean> BIOME_DEPENDENT_TINT_CACHE = new HashMap<>();

    private enum FallbackTintType {
        NONE,
        GRASS,
        FOLIAGE
    }

    private BiomeItemTintResolver() {
    }

    public static Context resolveContext(ItemStack stack, BiomeItemTintSettings settings) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        Block block = blockItem.getBlock();
        BlockState tintState = resolveTintState(block);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        Identifier tintBlockId = BuiltInRegistries.BLOCK.getKey(tintState.getBlock());
        if (!isTintCandidate(stack.getItem(), block, tintState, itemId, blockId, tintBlockId, settings)) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            return null;
        }

        return new Context(level, client.player.blockPosition(), stack.getItem(), block, itemId, blockId, tintBlockId, tintState);
    }

    public static boolean isTintCandidate(
        Item item,
        Block block,
        BlockState tintState,
        Identifier itemId,
        Identifier blockId,
        Identifier tintBlockId,
        BiomeItemTintSettings settings
    ) {
        if ((blockId == null && tintBlockId == null)
            || isDisabled(itemId, settings)
            || isDisabled(blockId, settings)
            || isDisabled(tintBlockId, settings)
            || isDisabled(item)
            || isDisabled(block)
            || isDisabled(tintState.getBlock())) {
            return false;
        }

        if (settings.autoDetect()) {
            return isConfiguredCandidate(itemId, blockId, tintBlockId, settings)
                || isTaggedTintCandidate(item, block, tintState)
                || isAutomaticTintCandidate(tintState, itemId, blockId, tintBlockId);
        }

        return isConfiguredCandidate(itemId, blockId, tintBlockId, settings)
            || isTaggedTintCandidate(item, block, tintState);
    }

    public static boolean shouldForceUntintedRenderState(ItemStack stack, BiomeItemTintSettings settings) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();
        BlockState tintState = resolveTintState(block);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        Identifier tintBlockId = BuiltInRegistries.BLOCK.getKey(tintState.getBlock());
        if (hasNativeFixedLeafTint(itemId) || hasNativeFixedLeafTint(blockId) || hasNativeFixedLeafTint(tintBlockId)) {
            return false;
        }

        return isDisabled(itemId, settings)
            || isDisabled(blockId, settings)
            || isDisabled(tintBlockId, settings)
            || isDisabled(stack.getItem())
            || isDisabled(block)
            || isDisabled(tintState.getBlock());
    }

    public static int resolveFallbackColor(Context context, BiomeItemTintSettings settings) {
        return resolveFallbackColor(context, settings, -1, null);
    }

    public static boolean shouldUseTintIndex(Context context, int tintIndex, SourceTintIndexLookup sourceTintIndexLookup) {
        if (tintIndex < 0) {
            return false;
        }

        if (context == null || sourceTintIndexLookup == null) {
            return true;
        }

        try {
            return sourceTintIndexLookup.getStatus(context, tintIndex) != SourceTintIndexStatus.ABSENT;
        } catch (Throwable t) {
            return true;
        }
    }

    public static int resolveFallbackColor(Context context, BiomeItemTintSettings settings, int tintIndex, BlockTintSampler sampler) {
        if (tintIndex > 0) {
            return -1;
        }

        FallbackTintDecision fallbackTintDecision = resolveFallbackTintDecision(context, settings);
        FallbackTintType fallbackTintType = fallbackTintDecision.type();
        if (fallbackTintDecision.automatic()
            && !hasBiomeDependentTint(context, fallbackTintType, tintIndex, sampler)) {
            return -1;
        }

        if (fallbackTintType == FallbackTintType.GRASS) {
            if (context.level() != null && context.pos() != null) {
                return BiomeColors.getAverageGrassColor(context.level(), context.pos());
            }
            //? if mc == 1.21.1
            return GrassColor.getDefaultColor();
            //? else
            return GrassColor.get(0.5D, 1.0D);
            //? endif
        }

        if (fallbackTintType == FallbackTintType.FOLIAGE) {
            if (context.level() != null && context.pos() != null) {
                return BiomeColors.getAverageFoliageColor(context.level(), context.pos());
            }
            //? if mc == 1.21.1
            return FoliageColor.getDefaultColor();
            //? else
            return FoliageColor.get(0.5D, 1.0D);
            //? endif
        }

        return -1;
    }

    public static int finalizeColor(Context context, int color) {
        if (isLeafLitterLike(context.blockId()) || isLeafLitterLike(context.tintBlockId())) {
            color = lightenRgb(color, LEAF_LITTER_ITEM_LIGHTEN_FACTOR);
        }
        return 0xFF000000 | (color & 0xFFFFFF);
    }

    public static boolean isGrassBlock(Identifier id) {
        return id != null && id.getPath().equals("grass_block");
    }

    public static BlockState resolveTintState(Block block) {
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

    private static boolean isDisabled(Identifier id, BiomeItemTintSettings settings) {
        return id != null && settings.isDisabled(id);
    }

    private static boolean isDisabled(Item item) {
        return item != null && item.builtInRegistryHolder().is(DISABLED_TINT_ITEMS);
    }

    private static boolean isDisabled(Block block) {
        return block != null && block.builtInRegistryHolder().is(DISABLED_TINT_BLOCKS);
    }

    private static boolean isAllowed(Identifier itemId, Identifier blockId, Identifier tintBlockId, BiomeItemTintSettings settings) {
        return isAllowed(itemId, settings)
            || isAllowed(blockId, settings)
            || isAllowed(tintBlockId, settings);
    }

    private static boolean isConfiguredCandidate(Identifier itemId, Identifier blockId, Identifier tintBlockId, BiomeItemTintSettings settings) {
        return isAllowed(itemId, blockId, tintBlockId, settings)
            || isGrassFallback(itemId, settings)
            || isGrassFallback(blockId, settings)
            || isGrassFallback(tintBlockId, settings)
            || isFoliageFallback(itemId, settings)
            || isFoliageFallback(blockId, settings)
            || isFoliageFallback(tintBlockId, settings);
    }

    private static boolean isAllowed(Identifier id, BiomeItemTintSettings settings) {
        return id != null && settings.isAllowed(id);
    }

    private static FallbackTintDecision resolveFallbackTintDecision(Context context, BiomeItemTintSettings settings) {
        if (isGrassFallback(context.itemId(), settings)
            || isGrassFallback(context.tintBlockId(), settings)
            || isGrassFallback(context.blockId(), settings)) {
            return new FallbackTintDecision(FallbackTintType.GRASS, false);
        }
        if (isFoliageFallback(context.itemId(), settings)
            || isFoliageFallback(context.tintBlockId(), settings)
            || isFoliageFallback(context.blockId(), settings)) {
            return new FallbackTintDecision(FallbackTintType.FOLIAGE, false);
        }

        if (isGrassTagged(context.item(), context.block(), context.tintState())) {
            return new FallbackTintDecision(FallbackTintType.GRASS, false);
        }
        if (isFoliageTagged(context.item(), context.block(), context.tintState())) {
            return new FallbackTintDecision(FallbackTintType.FOLIAGE, false);
        }

        FallbackTintType configuredFallbackTintType = detectConfiguredFallbackTintType(context, settings);
        if (configuredFallbackTintType != FallbackTintType.NONE) {
            return new FallbackTintDecision(configuredFallbackTintType, false);
        }

        if (!settings.autoDetect()) {
            return new FallbackTintDecision(FallbackTintType.NONE, false);
        }

        return new FallbackTintDecision(detectAutomaticFallbackTintType(context), true);
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

    private static FallbackTintType detectConfiguredFallbackTintType(Context context, BiomeItemTintSettings settings) {
        if (!isAllowed(context.itemId(), context.blockId(), context.tintBlockId(), settings)) {
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

    private static boolean isGrassFallback(Identifier id, BiomeItemTintSettings settings) {
        return id != null && (settings.isGrassFallback(id) || isHardcodedGrassCompat(id));
    }

    private static boolean isHardcodedGrassCompat(Identifier id) {
        return VINERY_GRASS_SLAB_ID.equals(id)
            || GRASS_STEP_ID.equals(id)
            || VERTICAL_GRASS_STEP_ID.equals(id)
            || VERTICAL_GRASS_SLAB_ID.equals(id);
    }

    private static boolean isFoliageFallback(Identifier id, BiomeItemTintSettings settings) {
        return id != null && settings.isFoliageFallback(id);
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
        //? if mc == 1.21.1
        return path.equals("grass_block") || path.equals("short_grass") || path.equals("tall_grass");
        //? else
        return path.contains("grass") || path.contains("fern");
        //? endif
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

    private static boolean hasNativeFixedLeafTint(Identifier id) {
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        return path.equals("spruce_leaves")
            || path.equals("birch_leaves")
            || path.equals("cherry_leaves")
            || path.equals("azalea_leaves")
            || path.equals("flowering_azalea_leaves")
            || path.equals("pale_oak_leaves")
            || path.equals("dark_cherry_leaves");
    }

    public static boolean isLeafLitterLike(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();
        BlockState tintState = resolveTintState(block);
        return isLeafLitterLike(BuiltInRegistries.ITEM.getKey(stack.getItem()))
            || isLeafLitterLike(BuiltInRegistries.BLOCK.getKey(block))
            || isLeafLitterLike(BuiltInRegistries.BLOCK.getKey(tintState.getBlock()));
    }

    public static boolean isLeafLitterLike(Identifier id) {
        return id != null && (id.getPath().equals("leaf_litter") || id.getPath().contains("leaf_pile"));
    }

    private static boolean hasBiomeDependentTint(Context context, FallbackTintType fallbackTintType, int tintIndex, BlockTintSampler sampler) {
        if (fallbackTintType == FallbackTintType.NONE
            || tintIndex < 0
            || sampler == null
            || context.level() == null
            || context.pos() == null) {
            return false;
        }

        Identifier keyId = context.tintBlockId() != null ? context.tintBlockId() : context.blockId();
        if (keyId == null) {
            return false;
        }

        BiomeTintProbeKey key = new BiomeTintProbeKey(keyId, tintIndex, fallbackTintType);
        Boolean cached = BIOME_DEPENDENT_TINT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        BiomeProbePair probePair = findBiomeProbePair(context.level(), context.pos(), fallbackTintType);
        if (probePair == null) {
            return false;
        }

        int firstColor = sampler.getColor(context.tintState(), context.level(), probePair.first(), tintIndex);
        int secondColor = sampler.getColor(context.tintState(), context.level(), probePair.second(), tintIndex);
        if (firstColor == -1 || secondColor == -1) {
            BIOME_DEPENDENT_TINT_CACHE.put(key, false);
            return false;
        }

        boolean biomeDependent = (firstColor & 0xFFFFFF) != (secondColor & 0xFFFFFF);
        BIOME_DEPENDENT_TINT_CACHE.put(key, biomeDependent);
        return biomeDependent;
    }

    private static BiomeProbePair findBiomeProbePair(ClientLevel level, BlockPos origin, FallbackTintType fallbackTintType) {
        BlockPos first = null;
        int firstBiomeColor = -1;
        for (int xOffset : BIOME_PROBE_OFFSETS) {
            for (int zOffset : BIOME_PROBE_OFFSETS) {
                BlockPos pos = origin.offset(xOffset, 0, zOffset);
                int biomeColor = getBiomeProbeColor(level, pos, fallbackTintType);
                if (first == null) {
                    first = pos;
                    firstBiomeColor = biomeColor;
                    continue;
                }

                if ((biomeColor & 0xFFFFFF) != (firstBiomeColor & 0xFFFFFF)) {
                    return new BiomeProbePair(first, pos);
                }
            }
        }

        return null;
    }

    private static int getBiomeProbeColor(ClientLevel level, BlockPos pos, FallbackTintType fallbackTintType) {
        if (fallbackTintType == FallbackTintType.GRASS) {
            return BiomeColors.getAverageGrassColor(level, pos);
        }
        if (fallbackTintType == FallbackTintType.FOLIAGE) {
            return BiomeColors.getAverageFoliageColor(level, pos);
        }
        return -1;
    }

    private static int lightenRgb(int color, float factor) {
        int red = Math.min(255, Math.round(((color >> 16) & 0xFF) * factor));
        int green = Math.min(255, Math.round(((color >> 8) & 0xFF) * factor));
        int blue = Math.min(255, Math.round((color & 0xFF) * factor));
        return (red << 16) | (green << 8) | blue;
    }

    public record Context(ClientLevel level, BlockPos pos, Item item, Block block, Identifier itemId, Identifier blockId, Identifier tintBlockId, BlockState tintState) {
    }

    @FunctionalInterface
    public interface BlockTintSampler {
        int getColor(BlockState state, ClientLevel level, BlockPos pos, int tintIndex);
    }

    public enum SourceTintIndexStatus {
        UNKNOWN,
        PRESENT,
        ABSENT
    }

    @FunctionalInterface
    public interface SourceTintIndexLookup {
        SourceTintIndexStatus getStatus(Context context, int tintIndex);
    }

    private record FallbackTintDecision(FallbackTintType type, boolean automatic) {
    }

    private record BiomeProbePair(BlockPos first, BlockPos second) {
    }

    private record BiomeTintProbeKey(Identifier id, int tintIndex, FallbackTintType fallbackTintType) {
    }
}
