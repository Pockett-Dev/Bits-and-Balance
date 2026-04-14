package org.onenonly.bitsandbalance.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModRawQuartz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A deterministic, multi-chunk vein pass that mirrors the *shape/decision* logic of vanilla
 * OreVeinifier, but runs as a normal placed feature.
 */
public class MojangStyleVeinFeature extends Feature<MojangStyleVeinConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-veins");

    // Vanilla-ish constants (1.21.x)
    private static final double VEININESS_THRESHOLD = 0.4000000059604645D;
    private static final double MAX_RICHNESS_THRESHOLD = 0.6000000238418579D;
    private static final double MIN_RICHNESS = 0.10000000149011612D;
    private static final double MAX_RICHNESS = 0.30000001192092896D;
    private static final double SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.30000001192092896D;

    private static final long SEED_XOR = 0x6A09E667F3BCC909L;
    private static final long TOGGLE_XOR = 0xBB67AE8584CAA73BL;
    private static final long RIDGED_XOR = 0x3C6EF372FE94F82BL;
    private static final long GAP_XOR = 0xA54FF53A5F1D36F1L;

    public MojangStyleVeinFeature(Codec<MojangStyleVeinConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<MojangStyleVeinConfiguration> context) {
        MojangStyleVeinConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        boolean debug = BitsAndBalanceCommon.isVeinsDebugEnabled();

        int minY = Math.max(config.minY(), level.getMinY());
        int maxY = Math.min(config.maxY(), level.getMaxY() - 1);
        if (minY > maxY) {
            return false;
        }

        // Frequency controls noise scale (vein shape), sampleChance controls perf sampling.
        double noiseScale = Math.max(1.0E-9D, config.frequency());
        double sampleChance = Mth.clamp(config.sampleChance(), 0.0D, 1.0D);
        int sampleOffsetXZ = config.sampleOffsetXZ();

        // Keep this feature chunk-local to avoid huge scan loops and cross-chunk writes.
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        int chunkMinX = originChunkX << 4;
        int chunkMinZ = originChunkZ << 4;

        double chunkChance = Mth.clamp(config.chunkChance(), 0.0D, 1.0D);
        if (chunkChance <= 0.0D) {
            return false;
        }

        int patchSizeXZ = Mth.clamp(config.patchSizeXZ(), 1, 16);
        int half = patchSizeXZ / 2;
        int patchMinX = Mth.clamp(origin.getX() - half, chunkMinX, chunkMinX + 16 - patchSizeXZ);
        int patchMinZ = Mth.clamp(origin.getZ() - half, chunkMinZ, chunkMinZ + 16 - patchSizeXZ);

        long worldSeed = level.getSeed();
        long baseSeed = worldSeed ^ SEED_XOR;

        if (chunkChance < 1.0D) {
            long h = mix(baseSeed ^ ((long) originChunkX * 341873128712L) ^ ((long) originChunkZ * 132897987541L));
            double v = ((h >>> 11) * 0x1.0p-53);
            if (v >= chunkChance) {
                return false;
            }
        }
        PerlinSimplexNoise toggleNoise = new PerlinSimplexNoise(RandomSource.create(baseSeed ^ TOGGLE_XOR), List.of(0));
        PerlinSimplexNoise ridgedNoise = new PerlinSimplexNoise(RandomSource.create(baseSeed ^ RIDGED_XOR), List.of(0));
        PerlinSimplexNoise gapNoise = new PerlinSimplexNoise(RandomSource.create(baseSeed ^ GAP_XOR), List.of(0));

        boolean placedAny = false;

        int placedTotal = 0;
        int placedOre = 0;
        int placedRaw = 0;
        int placedFiller = 0;

        for (int dx = 0; dx < patchSizeXZ; dx++) {
            int x = patchMinX + dx;
            for (int dz = 0; dz < patchSizeXZ; dz++) {
                int z = patchMinZ + dz;
                // Small perf win: precompute the scaled XZ inputs used by our 2D-noise mixing.
                int sx = x + sampleOffsetXZ;
                int sz = z + sampleOffsetXZ;
                double nx = sx * noiseScale;
                double nz = sz * noiseScale;

                for (int y = minY; y <= maxY; y++) {
                    if (!sampleFrequency(baseSeed, x, y, z, sampleChance)) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.ensureCanWrite(pos)) {
                        continue;
                    }

                    if ((pos.getX() >> 4) != originChunkX || (pos.getZ() >> 4) != originChunkZ) {
                        continue;
                    }

                    BlockState current = level.getBlockState(pos);
                    if (current.isAir()) {
                        continue;
                    }
                    if (!current.getFluidState().isEmpty()) {
                        continue;
                    }
                    if (current.is(BlockTags.FEATURES_CANNOT_REPLACE)) {
                        continue;
                    }
                    if (!current.is(config.replaceableTag())) {
                        continue;
                    }

                    BlockState decided = decideState(
                            toggleNoise,
                            ridgedNoise,
                            gapNoise,
                            worldSeed,
                            x,
                            y,
                            z,
                            nx,
                            nz,
                            minY,
                            maxY,
                            config
                    );
                    if (decided == null || decided.isAir()) {
                        continue;
                    }

                    level.setBlock(pos, decided, 2);
                    placedAny = true;

                    placedTotal++;
                    if (decided.getBlock() == config.ore().getBlock()) {
                        placedOre++;
                        if (debug) {
                            LOGGER.info("Vein ore placed (ore={}) at ({}, {}, {}) -> {}",
                                    config.ore().getBlock().toString(), x, y, z, decided.getBlock().toString());
                        }
                    } else if (!config.rawBlock().isAir() && decided.getBlock() == config.rawBlock().getBlock()) {
                        placedRaw++;
                        if (debug) {
                            LOGGER.info("Vein raw block placed (raw={}) at ({}, {}, {}) -> {}",
                                    config.rawBlock().getBlock().toString(), x, y, z, decided.getBlock().toString());
                        }
                    } else if (decided.getBlock() == config.filler().getBlock()) {
                        placedFiller++;
                    }
                }
            }
        }

        if (debug && placedAny) {
            LOGGER.info(
                    "Vein placed: ore={}, raw={}, filler={}, origin=({}, {}, {}), chunk=({}, {}), yRange=[{},{}], patchSizeXZ={}, placedTotal={} (ore={}, raw={}, filler={})",
                    config.ore().getBlock().toString(),
                    config.rawBlock().isAir() ? "(none)" : config.rawBlock().getBlock().toString(),
                    config.filler().getBlock().toString(),
                    origin.getX(),
                    origin.getY(),
                    origin.getZ(),
                    originChunkX,
                    originChunkZ,
                    minY,
                    maxY,
                    patchSizeXZ,
                    placedTotal,
                    placedOre,
                    placedRaw,
                    placedFiller
            );
        }

        return placedAny;
    }

    private static BlockState decideState(
            PerlinSimplexNoise toggleNoise,
            PerlinSimplexNoise ridgedNoise,
            PerlinSimplexNoise gapNoise,
            long worldSeed,
            int x,
            int y,
            int z,
            double nx,
            double nz,
            int minY,
            int maxY,
            MojangStyleVeinConfiguration config
    ) {
        BlockState rawBlock = config.rawBlock();
        float chanceOfRawBlock = config.chanceOfRawBlock();
        if (!BitsAndBalanceCommon.isEnableRawQuartzBlockInQuartzVeins()
                && rawBlock.getBlock() == ModRawQuartz.rawQuartzBlock()) {
            rawBlock = Blocks.AIR.defaultBlockState();
            chanceOfRawBlock = 0.0F;
        }

        // Mix Y into the second axis so we get a stable 3D-ish field.
        double ny = y * Math.max(1.0E-9D, config.frequency());

        double toggle = toggleNoise.getValue(nx, ny + nz, false);
        if (config.requirePositiveToggle()) {
            if (toggle <= 0.0D) {
                return null;
            }
        } else {
            if (toggle > 0.0D) {
                return null;
            }
        }

        double abs = Math.abs(toggle);

        int toMax = maxY - y;
        int fromMin = y - minY;
        if (fromMin < 0 || toMax < 0) {
            return null;
        }

        int edge = Math.min(toMax, fromMin);
        double edgeRoundoff = Mth.clampedMap((double) edge, 0.0D, 20.0D, -0.2D, 0.0D);
        if (abs + edgeRoundoff < VEININESS_THRESHOLD) {
            return null;
        }

        // Deterministic per-block RNG (similar role to vanilla's PositionalRandomFactory.at).
        long posSeed = Mth.getSeed(x + config.sampleOffsetXZ(), y, z + config.sampleOffsetXZ());
        RandomSource rand = RandomSource.create(posSeed ^ (worldSeed * 31L) ^ 0xD1B54A32D192ED03L);

        if (rand.nextFloat() > config.veinSolidness()) {
            return null;
        }

        double ridged = ridgedNoise.getValue(ny + nx, nz, false);
        if (ridged >= 0.0D) {
            return null;
        }

        double richness = Mth.clampedMap(abs, VEININESS_THRESHOLD, MAX_RICHNESS_THRESHOLD, MIN_RICHNESS, MAX_RICHNESS);
        richness = Mth.clamp(richness * Math.max(0.0D, config.richnessMultiplier()), 0.0D, 1.0D);
        if ((double) rand.nextFloat() < richness) {
            double gap = gapNoise.getValue(nx, ny - nz, false);
            if (gap > config.gapThreshold()) {
                if (!rawBlock.isAir() && rand.nextFloat() < chanceOfRawBlock) {
                    return rawBlock;
                }
                return config.ore();
            }
        }

        return config.filler();
    }

    private static boolean sampleFrequency(long seed, int x, int y, int z, double probability) {
        if (probability <= 0.0D) {
            return false;
        }
        if (probability >= 1.0D) {
            return true;
        }

        // Deterministic hash -> [0,1)
        long h = mix(seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L) ^ ((long) y * 42317861L));
        double v = ((h >>> 11) * 0x1.0p-53);
        return v < probability;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }
}
