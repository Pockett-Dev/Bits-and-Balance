package org.onenonly.bitsandbalance.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.onenonly.bitsandbalance.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mojang-style coal veins for NeoForge.
 *
 * Vanilla massive veins are generated via the noise-based {@link OreVeinifier} (not a placed feature).
 * When enabled, we preserve vanilla massive COPPER and IRON veins exactly as-is, and add an additional
 * Mojang-style coal/andesite vein pass (same algorithm), using an offset sampling of the vein density
 * functions so the coal veins are independent.
 */
@Mixin(OreVeinifier.class)
public abstract class OreVeinifierCoalVeinsMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-coal-veins");
    private static final AtomicBoolean LOGGED_WRAPPER_ACTIVE = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_DISABLED = new AtomicBoolean(false);

    /**
     * When debugVeins is enabled we can log a lot; keep filler logs to once-per-chunk-per-variant.
     * Key format: chunkKey ^ variantBit.
     */
    private static final ConcurrentHashMap<Long, Boolean> LOGGED_FILLER_CHUNKS = new ConcurrentHashMap<>();

    // Vanilla constants (1.21.11)
    private static final double VEININESS_THRESHOLD = 0.4000000059604645D;
    private static final double MAX_RICHNESS_THRESHOLD = 0.6000000238418579D;
    private static final double MIN_RICHNESS = 0.10000000149011612D;
    private static final double MAX_RICHNESS = 0.30000001192092896D;
    private static final float VEIN_SOLIDNESS = 0.7F;
    private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;
    private static final double SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.30000001192092896D;
    private static final double MIN_FILLER_VEININESS = 0.5D;
    private static final double MIN_RAW_BLOCK_VEININESS = 0.58D;
    private static final double MIN_DEEP_ORE_VEININESS = 0.48D;
    private static final int MIN_FILLER_EDGE_DISTANCE = 4;
    private static final int MIN_RAW_BLOCK_EDGE_DISTANCE = 8;
    private static final int MIN_DEEP_ORE_EDGE_DISTANCE = 4;
    private static final int MIN_DEEP_SUPPORTING_NEIGHBORS = 4;

    // Coal vein variants
    private static final int COAL_SHALLOW_MIN_Y = 24;
    private static final int COAL_SHALLOW_MAX_Y = 90;
    private static final BlockState COAL_SHALLOW_ORE = Blocks.COAL_ORE.defaultBlockState();
    private static final BlockState COAL_SHALLOW_RAW_BLOCK = Blocks.COAL_BLOCK.defaultBlockState();
    private static final BlockState COAL_SHALLOW_FILLER = Blocks.ANDESITE.defaultBlockState();

    // Deep variant (35% tuff / 65% deepslate filler + coal ores + coal blocks)
    private static final int COAL_DEEP_MIN_Y = -64;
    private static final int COAL_DEEP_MAX_Y = 0;
    private static final BlockState COAL_DEEP_EDGE_ORE = Blocks.DEEPSLATE_COAL_ORE.defaultBlockState();
    private static final BlockState COAL_DEEP_RAW_BLOCK = Blocks.COAL_BLOCK.defaultBlockState();
    private static final BlockState COAL_DEEP_PRIMARY_FILLER = Blocks.TUFF.defaultBlockState();
    private static final BlockState COAL_DEEP_SECONDARY_FILLER = Blocks.DEEPSLATE.defaultBlockState();
    private static final float COAL_DEEP_PRIMARY_FILLER_CHANCE = 0.35F;
    private static final Identifier TUFF_COAL_ORE_ID = Identifier.fromNamespaceAndPath("bitsandbalance", "tuff_coal_ore");

    private static final int COAL_SHALLOW_SAMPLE_OFFSET_XZ = 4096;
    private static final int COAL_DEEP_SAMPLE_OFFSET_XZ = -4096;
    private static final int COAL_DEEP_SECONDARY_SAMPLE_OFFSET_XZ = -12288;

    @Inject(method = "create", at = @At("RETURN"), cancellable = true)
    private static void bitsandbalance$wrapVeinifier(
            DensityFunction veinToggle,
            DensityFunction veinRidged,
            DensityFunction veinGap,
            PositionalRandomFactory randomFactory,
            CallbackInfoReturnable<NoiseChunk.BlockStateFiller> cir
    ) {
        NoiseChunk.BlockStateFiller original = cir.getReturnValue();
        if (original == null) {
            return;
        }

        cir.setReturnValue((DensityFunction.FunctionContext ctx) -> {
            boolean debug = Config.debugVeins;
            if (debug && LOGGED_WRAPPER_ACTIVE.compareAndSet(false, true)) {
                LOGGER.info(
                    "OreVeinifier coal-vein wrapper active. enableCoalAndesiteVeins={}, enableCoalTuffVeins={}",
                    Config.enableCoalAndesiteVeins,
                    Config.enableCoalTuffVeins
                );
            }
            if (!Config.enableCoalAndesiteVeins && !Config.enableCoalTuffVeins) {
                if (debug && LOGGED_DISABLED.compareAndSet(false, true)) {
                    LOGGER.info("Coal veins disabled by config (both coal toggles are false). Mojang-style coal veins will not generate.");
                }
                return original.calculate(ctx);
            }

            BlockState vanilla = original.calculate(ctx);
            if (vanilla != null) {
                return vanilla;
            }

            if (Config.enableCoalTuffVeins) {
                BlockState deep = bitsandbalance$calculateDeepCoalVein(
                    veinToggle,
                    veinRidged,
                    veinGap,
                    randomFactory,
                    ctx
                );
                if (deep != null) {
                    bitsandbalance$logCoalVeinPlacement(debug, ctx, deep, COAL_DEEP_PRIMARY_FILLER, COAL_DEEP_SECONDARY_FILLER, true);
                    return deep;
                }
            }

            if (Config.enableCoalAndesiteVeins) {
                BlockState shallow = bitsandbalance$calculateCoalVein(
                        veinToggle,
                        veinRidged,
                        veinGap,
                        randomFactory,
                        ctx,
                        COAL_SHALLOW_MIN_Y,
                        COAL_SHALLOW_MAX_Y,
                        COAL_SHALLOW_ORE,
                        COAL_SHALLOW_ORE,
                        COAL_SHALLOW_RAW_BLOCK,
                        COAL_SHALLOW_FILLER,
                        COAL_SHALLOW_FILLER,
                        COAL_SHALLOW_SAMPLE_OFFSET_XZ,
                        0.0D,
                        0,
                        0,
                        true
                );
                if (shallow != null) {
                    bitsandbalance$logCoalVeinPlacement(debug, ctx, shallow, COAL_SHALLOW_FILLER, COAL_SHALLOW_FILLER, false);
                }
                return shallow;
            }

            return null;
        });
    }

    private static BlockState bitsandbalance$calculateDeepCoalVein(
            DensityFunction veinToggle,
            DensityFunction veinRidged,
            DensityFunction veinGap,
            PositionalRandomFactory randomFactory,
            DensityFunction.FunctionContext ctx
    ) {
        BlockState firstPass = bitsandbalance$calculateCoalVein(
                veinToggle,
                veinRidged,
                veinGap,
                randomFactory,
                ctx,
                COAL_DEEP_MIN_Y,
                COAL_DEEP_MAX_Y,
                bitsandbalance$deepCoalOre(),
                COAL_DEEP_EDGE_ORE,
                COAL_DEEP_RAW_BLOCK,
                COAL_DEEP_PRIMARY_FILLER,
                COAL_DEEP_SECONDARY_FILLER,
                COAL_DEEP_SAMPLE_OFFSET_XZ,
                MIN_DEEP_ORE_VEININESS,
                MIN_DEEP_ORE_EDGE_DISTANCE,
                MIN_DEEP_SUPPORTING_NEIGHBORS,
                false
        );
        if (firstPass != null) {
            return firstPass;
        }

        return bitsandbalance$calculateCoalVein(
                veinToggle,
                veinRidged,
                veinGap,
                randomFactory,
                ctx,
                COAL_DEEP_MIN_Y,
                COAL_DEEP_MAX_Y,
                bitsandbalance$deepCoalOre(),
                COAL_DEEP_EDGE_ORE,
                COAL_DEEP_RAW_BLOCK,
                COAL_DEEP_PRIMARY_FILLER,
                COAL_DEEP_SECONDARY_FILLER,
                COAL_DEEP_SECONDARY_SAMPLE_OFFSET_XZ,
                MIN_DEEP_ORE_VEININESS,
                MIN_DEEP_ORE_EDGE_DISTANCE,
                MIN_DEEP_SUPPORTING_NEIGHBORS,
                false
        );
    }

    private static BlockState bitsandbalance$calculateCoalVein(
            DensityFunction veinToggle,
            DensityFunction veinRidged,
            DensityFunction veinGap,
            PositionalRandomFactory randomFactory,
            DensityFunction.FunctionContext ctx,
            int minY,
            int maxY,
            BlockState coreOre,
            BlockState edgeOre,
            BlockState rawBlock,
            BlockState primaryFiller,
            BlockState secondaryFiller,
            int sampleOffsetXZ,
            double minOreVeininess,
            int minOreEdgeDistance,
                int minSupportingNeighbors,
            boolean requirePositiveToggle
    ) {
        int y = ctx.blockY();
        if (y < minY || y > maxY) {
            return null;
        }

        // No per-branch "first hit" logging here anymore; debugVeins placement logs are emitted in the wrapper.

        // If vanilla debug ore veins are enabled, vanilla already returns non-null (AIR/log blocks),
        // so we can skip emitting our own debug visualization.
        BlockState baseReturn = null;

        DensityFunction.FunctionContext coalCtx = new OffsetContext(ctx, sampleOffsetXZ, 0, sampleOffsetXZ);

        double toggle = veinToggle.compute(coalCtx);
        if (requirePositiveToggle) {
            if (toggle <= 0.0D) {
                return baseReturn;
            }
        } else {
            if (toggle > 0.0D) {
                return baseReturn;
            }
        }

        double abs = Math.abs(toggle);

        int toMax = maxY - y;
        int fromMin = y - minY;
        if (fromMin < 0 || toMax < 0) {
            return baseReturn;
        }

        int edge = Math.min(toMax, fromMin);
        double edgeRoundoff = Mth.clampedMap((double) edge, 0.0D, 20.0D, -0.2D, 0.0D);
        if (abs + edgeRoundoff < VEININESS_THRESHOLD) {
            return baseReturn;
        }

        RandomSource rand = randomFactory.at(ctx.blockX() + sampleOffsetXZ, y, ctx.blockZ() + sampleOffsetXZ);
        if (rand.nextFloat() > VEIN_SOLIDNESS) {
            return baseReturn;
        }

        if (veinRidged.compute(coalCtx) >= 0.0D) {
            return baseReturn;
        }

        double richness = Mth.clampedMap(abs, VEININESS_THRESHOLD, MAX_RICHNESS_THRESHOLD, MIN_RICHNESS, MAX_RICHNESS);
        double gap = veinGap.compute(coalCtx);
        int supportCount = minSupportingNeighbors > 0
            ? bitsandbalance$countPlacedNeighbors(
                veinToggle,
                veinRidged,
                veinGap,
                randomFactory,
                ctx,
                minY,
                maxY,
                sampleOffsetXZ,
                requirePositiveToggle,
                minOreVeininess,
                minOreEdgeDistance
            )
            : 0;
        if (minSupportingNeighbors > 0 && supportCount < minSupportingNeighbors) {
            return baseReturn;
        }

        BlockState selectedFiller = bitsandbalance$selectFiller(primaryFiller, secondaryFiller, randomFactory, ctx, sampleOffsetXZ);

        if ((double) rand.nextFloat() < richness
            && gap > SKIP_ORE_IF_GAP_NOISE_IS_BELOW) {
            boolean allowOre = abs >= minOreVeininess && edge >= minOreEdgeDistance;
            boolean allowRawBlock = abs >= MIN_RAW_BLOCK_VEININESS && edge >= MIN_RAW_BLOCK_EDGE_DISTANCE;
            if (allowRawBlock && minSupportingNeighbors > 0 && supportCount <= minSupportingNeighbors) {
                allowRawBlock = false;
            }
            if (allowOre) {
                BlockState ore = bitsandbalance$selectOre(coreOre, edgeOre, selectedFiller, primaryFiller, secondaryFiller);
                return allowRawBlock && rand.nextFloat() < CHANCE_OF_RAW_ORE_BLOCK ? rawBlock : ore;
            }
        }

        if (abs >= MIN_FILLER_VEININESS && edge >= MIN_FILLER_EDGE_DISTANCE && gap > SKIP_ORE_IF_GAP_NOISE_IS_BELOW) {
            return selectedFiller;
        }

        return baseReturn;
    }

    private static BlockState bitsandbalance$deepCoalOre() {
        return BuiltInRegistries.BLOCK.getOptional(TUFF_COAL_ORE_ID)
                .orElse(Blocks.DEEPSLATE_COAL_ORE)
                .defaultBlockState();
    }

    private static BlockState bitsandbalance$selectOre(
            BlockState coreOre,
            BlockState edgeOre,
            BlockState selectedFiller,
            BlockState primaryFiller,
            BlockState secondaryFiller
    ) {
        if (coreOre == edgeOre) {
            return coreOre;
        }

        if (selectedFiller.getBlock() == primaryFiller.getBlock()) {
            return coreOre;
        }
        if (selectedFiller.getBlock() == secondaryFiller.getBlock()) {
            return edgeOre;
        }
        return edgeOre;
    }

    private static BlockState bitsandbalance$selectFiller(
            BlockState primaryFiller,
            BlockState secondaryFiller,
            PositionalRandomFactory randomFactory,
            DensityFunction.FunctionContext ctx,
            int sampleOffsetXZ
    ) {
        if (primaryFiller == secondaryFiller) {
            return primaryFiller;
        }

        RandomSource hostRand = SharedConstants.debugGenerateSquareTerrainWithoutNoise
                ? RandomSource.create(0L)
                : randomFactory.at(ctx.blockX() + sampleOffsetXZ + 2048, ctx.blockY(), ctx.blockZ() + sampleOffsetXZ + 2048);
        return hostRand.nextFloat() < COAL_DEEP_PRIMARY_FILLER_CHANCE ? primaryFiller : secondaryFiller;
    }

    private static int bitsandbalance$countPlacedNeighbors(
            DensityFunction veinToggle,
            DensityFunction veinRidged,
            DensityFunction veinGap,
            PositionalRandomFactory randomFactory,
            DensityFunction.FunctionContext ctx,
            int minY,
            int maxY,
            int sampleOffsetXZ,
            boolean requirePositiveToggle,
            double minVeininess,
            int minEdgeDistance
    ) {
        int support = 0;
            support += bitsandbalance$wouldPlaceVeinMaterial(veinToggle, veinRidged, veinGap, randomFactory, new OffsetContext(ctx, 1, 0, 0), minY, maxY, sampleOffsetXZ, requirePositiveToggle, minVeininess, minEdgeDistance) ? 1 : 0;
            support += bitsandbalance$wouldPlaceVeinMaterial(veinToggle, veinRidged, veinGap, randomFactory, new OffsetContext(ctx, -1, 0, 0), minY, maxY, sampleOffsetXZ, requirePositiveToggle, minVeininess, minEdgeDistance) ? 1 : 0;
            support += bitsandbalance$wouldPlaceVeinMaterial(veinToggle, veinRidged, veinGap, randomFactory, new OffsetContext(ctx, 0, 1, 0), minY, maxY, sampleOffsetXZ, requirePositiveToggle, minVeininess, minEdgeDistance) ? 1 : 0;
            support += bitsandbalance$wouldPlaceVeinMaterial(veinToggle, veinRidged, veinGap, randomFactory, new OffsetContext(ctx, 0, -1, 0), minY, maxY, sampleOffsetXZ, requirePositiveToggle, minVeininess, minEdgeDistance) ? 1 : 0;
            support += bitsandbalance$wouldPlaceVeinMaterial(veinToggle, veinRidged, veinGap, randomFactory, new OffsetContext(ctx, 0, 0, 1), minY, maxY, sampleOffsetXZ, requirePositiveToggle, minVeininess, minEdgeDistance) ? 1 : 0;
            support += bitsandbalance$wouldPlaceVeinMaterial(veinToggle, veinRidged, veinGap, randomFactory, new OffsetContext(ctx, 0, 0, -1), minY, maxY, sampleOffsetXZ, requirePositiveToggle, minVeininess, minEdgeDistance) ? 1 : 0;
        return support;
    }

            private static boolean bitsandbalance$wouldPlaceVeinMaterial(
            DensityFunction veinToggle,
            DensityFunction veinRidged,
            DensityFunction veinGap,
                PositionalRandomFactory randomFactory,
            DensityFunction.FunctionContext sampleCtx,
            int minY,
            int maxY,
            int sampleOffsetXZ,
            boolean requirePositiveToggle,
            double minVeininess,
            int minEdgeDistance
    ) {
        int y = sampleCtx.blockY();
        if (y < minY || y > maxY) {
            return false;
        }

        DensityFunction.FunctionContext coalCtx = new OffsetContext(sampleCtx, sampleOffsetXZ, 0, sampleOffsetXZ);
        double toggle = veinToggle.compute(coalCtx);
        if (requirePositiveToggle ? toggle <= 0.0D : toggle > 0.0D) {
            return false;
        }

        double abs = Math.abs(toggle);
        int edge = Math.min(maxY - y, y - minY);
        if (edge < minEdgeDistance) {
            return false;
        }

        double edgeRoundoff = Mth.clampedMap((double) edge, 0.0D, 20.0D, -0.2D, 0.0D);
        if (abs + edgeRoundoff < VEININESS_THRESHOLD || abs < minVeininess) {
            return false;
        }

        RandomSource rand = SharedConstants.debugGenerateSquareTerrainWithoutNoise
            ? RandomSource.create(0L)
            : randomFactory.at(sampleCtx.blockX() + sampleOffsetXZ, y, sampleCtx.blockZ() + sampleOffsetXZ);
        if (rand.nextFloat() > VEIN_SOLIDNESS) {
            return false;
        }

        if (veinRidged.compute(coalCtx) >= 0.0D) {
            return false;
        }

        double gap = veinGap.compute(coalCtx);
        if (gap <= SKIP_ORE_IF_GAP_NOISE_IS_BELOW) {
            return false;
        }

        double richness = Mth.clampedMap(abs, VEININESS_THRESHOLD, MAX_RICHNESS_THRESHOLD, MIN_RICHNESS, MAX_RICHNESS);
        boolean allowOre = abs >= minVeininess && edge >= minEdgeDistance;
        if ((double) rand.nextFloat() < richness && allowOre) {
            return true;
        }

        return abs >= MIN_FILLER_VEININESS && edge >= MIN_FILLER_EDGE_DISTANCE;
    }

    private static void bitsandbalance$logCoalVeinPlacement(
            boolean debug,
            DensityFunction.FunctionContext ctx,
            BlockState placed,
            BlockState primaryFiller,
            BlockState secondaryFiller,
            boolean deep
    ) {
        if (!debug) {
            return;
        }

        int x = ctx.blockX();
        int y = ctx.blockY();
        int z = ctx.blockZ();

        // Filler blocks occur extremely frequently inside a vein; only log them once per chunk.
        if (placed.getBlock() == primaryFiller.getBlock() || placed.getBlock() == secondaryFiller.getBlock()) {
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
            long key = chunkKey ^ (deep ? 0x8000000000000000L : 0L);
            if (LOGGED_FILLER_CHUNKS.putIfAbsent(key, Boolean.TRUE) != null) {
                return;
            }
            LOGGER.info(
                    "Coal vein filler placed (variant={}, chunk=({}, {})) at ({}, {}, {}) -> {}",
                    deep ? "deep_tuff" : "shallow_andesite",
                    chunkX,
                    chunkZ,
                    x,
                    y,
                    z,
                    placed.getBlock().toString()
            );
            return;
        }

        // Ores/raw blocks are less frequent; log every placement while debugVeins is enabled.
        LOGGER.info(
                "Coal vein ore placed (variant={}) at ({}, {}, {}) -> {}",
                deep ? "deep_tuff" : "shallow_andesite",
                x,
                y,
                z,
                placed.getBlock().toString()
        );
    }

    private static final class OffsetContext implements DensityFunction.FunctionContext {
        private final DensityFunction.FunctionContext delegate;
        private final int dx;
        private final int dy;
        private final int dz;

        private OffsetContext(DensityFunction.FunctionContext delegate, int dx, int dy, int dz) {
            this.delegate = delegate;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        @Override
        public int blockX() {
            return delegate.blockX() + dx;
        }

        @Override
        public int blockY() {
            return delegate.blockY() + dy;
        }

        @Override
        public int blockZ() {
            return delegate.blockZ() + dz;
        }
    }
}
