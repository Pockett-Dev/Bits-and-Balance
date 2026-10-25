package org.onenonly.bitsandbalance.worldgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModRawQuartz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 26.3 data-driven form of the deterministic Mojang-style vein pass. */
public final class MojangStyleVeinFeature implements Feature {
    public static final MapCodec<MojangStyleVeinFeature> CODEC = MojangStyleVeinConfiguration.CODEC
            .fieldOf("config")
            .xmap(MojangStyleVeinFeature::new, MojangStyleVeinFeature::configuration);

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-veins");
    private static final double VEININESS_THRESHOLD = 0.4000000059604645D;
    private static final double MAX_RICHNESS_THRESHOLD = 0.6000000238418579D;
    private static final double MIN_RICHNESS = 0.10000000149011612D;
    private static final double MAX_RICHNESS = 0.30000001192092896D;
    private static final long SEED_XOR = 0x6A09E667F3BCC909L;
    private static final long TOGGLE_XOR = 0xBB67AE8584CAA73BL;
    private static final long RIDGED_XOR = 0x3C6EF372FE94F82BL;
    private static final long GAP_XOR = 0xA54FF53A5F1D36F1L;

    private final MojangStyleVeinConfiguration configuration;

    public MojangStyleVeinFeature(MojangStyleVeinConfiguration configuration) {
        this.configuration = configuration;
    }

    public MojangStyleVeinConfiguration configuration() {
        return configuration;
    }

    @Override
    public MapCodec<MojangStyleVeinFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        MojangStyleVeinConfiguration config = configuration;
        boolean debug = BitsAndBalanceCommon.isVeinsDebugEnabled();
        int minY = Math.max(config.minY(), level.getMinY());
        int maxY = Math.min(config.maxY(), level.getMaxY() - 1);
        if (minY > maxY) {
            return false;
        }

        double noiseScale = Math.max(1.0E-9D, config.frequency());
        double sampleChance = Mth.clamp(config.sampleChance(), 0.0D, 1.0D);
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
            long hash = mix(baseSeed
                    ^ (long) originChunkX * 341873128712L
                    ^ (long) originChunkZ * 132897987541L);
            if ((hash >>> 11) * 0x1.0p-53 >= chunkChance) {
                return false;
            }
        }

        // These replace the removed single-octave PerlinSimplexNoise instances.
        SimplexNoise toggleNoise = new SimplexNoise(RandomSource.create(baseSeed ^ TOGGLE_XOR));
        SimplexNoise ridgedNoise = new SimplexNoise(RandomSource.create(baseSeed ^ RIDGED_XOR));
        SimplexNoise gapNoise = new SimplexNoise(RandomSource.create(baseSeed ^ GAP_XOR));
        boolean placedAny = false;
        int placedTotal = 0;
        int placedOre = 0;
        int placedRaw = 0;
        int placedFiller = 0;

        for (int dx = 0; dx < patchSizeXZ; dx++) {
            int x = patchMinX + dx;
            for (int dz = 0; dz < patchSizeXZ; dz++) {
                int z = patchMinZ + dz;
                double nx = (x + config.sampleOffsetXZ()) * noiseScale;
                double nz = (z + config.sampleOffsetXZ()) * noiseScale;

                for (int y = minY; y <= maxY; y++) {
                    if (!sampleFrequency(baseSeed, x, y, z, sampleChance)) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.ensureCanWrite(pos)
                            || (pos.getX() >> 4) != originChunkX
                            || (pos.getZ() >> 4) != originChunkZ) {
                        continue;
                    }

                    BlockState current = level.getBlockState(pos);
                    if (current.isAir()
                            || !current.getFluidState().isEmpty()
                            || current.is(BlockTags.FEATURES_CANNOT_REPLACE)
                            || !current.is(config.replaceableTag())) {
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
                                    config.ore().getBlock(), x, y, z, decided.getBlock());
                        }
                    } else if (!config.rawBlock().isAir() && decided.getBlock() == config.rawBlock().getBlock()) {
                        placedRaw++;
                        if (debug) {
                            LOGGER.info("Vein raw block placed (raw={}) at ({}, {}, {}) -> {}",
                                    config.rawBlock().getBlock(), x, y, z, decided.getBlock());
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
                    config.ore().getBlock(),
                    config.rawBlock().isAir() ? "(none)" : config.rawBlock().getBlock(),
                    config.filler().getBlock(),
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
            SimplexNoise toggleNoise,
            SimplexNoise ridgedNoise,
            SimplexNoise gapNoise,
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

        double ny = y * Math.max(1.0E-9D, config.frequency());
        double toggle = toggleNoise.get(nx, ny + nz);
        if (config.requirePositiveToggle() ? toggle <= 0.0D : toggle > 0.0D) {
            return null;
        }

        double absoluteToggle = Math.abs(toggle);
        int distanceFromEdge = Math.min(maxY - y, y - minY);
        if (distanceFromEdge < 0) {
            return null;
        }
        double edgeRoundoff = Mth.clampedMap(distanceFromEdge, 0.0D, 20.0D, -0.2D, 0.0D);
        if (absoluteToggle + edgeRoundoff < VEININESS_THRESHOLD) {
            return null;
        }

        long posSeed = Mth.getSeed(
                x + config.sampleOffsetXZ(),
                y,
                z + config.sampleOffsetXZ()
        );
        RandomSource positionalRandom = RandomSource.create(
                posSeed ^ worldSeed * 31L ^ 0xD1B54A32D192ED03L
        );
        if (positionalRandom.nextFloat() > config.veinSolidness()) {
            return null;
        }
        if (ridgedNoise.get(ny + nx, nz) >= 0.0D) {
            return null;
        }

        double richness = Mth.clampedMap(
                absoluteToggle,
                VEININESS_THRESHOLD,
                MAX_RICHNESS_THRESHOLD,
                MIN_RICHNESS,
                MAX_RICHNESS
        );
        richness = Mth.clamp(richness * Math.max(0.0D, config.richnessMultiplier()), 0.0D, 1.0D);
        if (positionalRandom.nextFloat() < richness
                && gapNoise.get(nx, ny - nz) > config.gapThreshold()) {
            if (!rawBlock.isAir() && positionalRandom.nextFloat() < chanceOfRawBlock) {
                return rawBlock;
            }
            return config.ore();
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
        long hash = mix(seed
                ^ (long) x * 341873128712L
                ^ (long) z * 132897987541L
                ^ (long) y * 42317861L);
        return (hash >>> 11) * 0x1.0p-53 < probability;
    }

    private static long mix(long value) {
        value = (value ^ value >>> 33) * 0xff51afd7ed558ccdL;
        value = (value ^ value >>> 33) * 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }
}
