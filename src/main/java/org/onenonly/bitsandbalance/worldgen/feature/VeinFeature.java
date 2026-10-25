package org.onenonly.bitsandbalance.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import java.util.HashSet;
import java.util.Set;

public class VeinFeature extends Feature<VeinConfiguration> {
    private static final long SEED_XOR = 0x9E3779B97F4A7C15L;
    private static final long NOISE_X_XOR = 305441741L;
    private static final long NOISE_Y_XOR = 3203338804L;
    private static final long NOISE_Z_XOR = 4277009102L;

    public VeinFeature(com.mojang.serialization.Codec<VeinConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<VeinConfiguration> context) {
        VeinConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // Shared logic only. Loader-specific config gating should happen before placement.
        boolean enforceReplaceableTag = true;

        BlockPos start = origin;

        return generate(level, random, origin, start, config, enforceReplaceableTag);
    }

    private boolean generate(
            WorldGenLevel level,
            RandomSource random,
            BlockPos origin,
            BlockPos start,
            VeinConfiguration config,
            boolean strictReplaceableTag
    ) {
        long seed = level.getSeed() ^ SEED_XOR;
        PerlinSimplexNoise noiseX = new PerlinSimplexNoise(RandomSource.create(seed ^ NOISE_X_XOR), java.util.List.of(0));
        PerlinSimplexNoise noiseY = new PerlinSimplexNoise(RandomSource.create(seed ^ NOISE_Y_XOR), java.util.List.of(0));
        PerlinSimplexNoise noiseZ = new PerlinSimplexNoise(RandomSource.create(seed ^ NOISE_Z_XOR), java.util.List.of(0));

        int length = Mth.nextInt(random, config.minLength(), config.maxLength());
        double stepSize = Math.max(0.75d, config.stepSize());
        boolean fixedLength = config.minLength() == config.maxLength();

        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();

        boolean placedAny = false;
        Set<Long> visited = new HashSet<>();

        for (int i = 0; i < length; i++) {
            double freq = config.fieldFrequency();

            double dx = noiseX.getValue(x * freq, y * freq, false);
            double dy = noiseY.getValue(y * freq, z * freq, false) * 0.65d;
            double dz = noiseZ.getValue(z * freq, x * freq, false);

            double mag = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (mag < 1.0e-5d) {
                mag = 1.0e-5d;
            }

            dx /= mag;
            dy /= mag;
            dz /= mag;

            double jitter = config.jitter() * 0.5d;
            dx += (random.nextDouble() - 0.5d) * jitter;
            dy += (random.nextDouble() - 0.5d) * jitter * 0.5d;
            dz += (random.nextDouble() - 0.5d) * jitter;

            x += dx * stepSize;
            y += dy * stepSize;
            z += dz * stepSize;

            BlockPos center = BlockPos.containing(x, y, z);

            float t = (float) i / (float) length;
            float radius;
            if (fixedLength) {
                radius = config.maxRadius();
            } else {
                radius = Mth.lerp(Mth.sin(t * (float) Math.PI), config.minRadius(), config.maxRadius());
            }

            placedAny |= fillTube(level, random, center, radius, config, origin, fixedLength, visited, strictReplaceableTag);
        }

        return placedAny;
    }

    private boolean fillTube(
            WorldGenLevel level,
            RandomSource random,
            BlockPos center,
            float radius,
            VeinConfiguration config,
            BlockPos origin,
            boolean fixedLength,
            Set<Long> visited,
            boolean strictReplaceableTag
    ) {
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        int r = Mth.ceil(radius + 1.0f);
        boolean placedAny = false;
        double radiusSq = (double) radius * (double) radius;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double distSq = (double) dx * (double) dx + (double) dy * (double) dy + (double) dz * (double) dz;
                    if (distSq > radiusSq) {
                        continue;
                    }

                    BlockPos pos = center.offset(dx, dy, dz);

                    long packed = pos.asLong();
                    if (visited.contains(packed)) {
                        continue;
                    }
                    visited.add(packed);

                    if (!level.ensureCanWrite(pos)) {
                        continue;
                    }

                    // Avoid cross-chunk writes during the FEATURES generation stage.
                    // Writing into a different chunk from the placement origin triggers
                    // spammy "Detected setBlock in a far chunk" errors on modern versions.
                    if ((pos.getX() >> 4) != originChunkX || (pos.getZ() >> 4) != originChunkZ) {
                        continue;
                    }

                    // Match NeoForge behavior: apply a local distance cap only when air-exposure trimming is enabled.
                    if (config.respectAirExposure()) {
                        if (Math.abs(pos.getX() - origin.getX()) > 24 || Math.abs(pos.getZ() - origin.getZ()) > 24) {
                            continue;
                        }
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
                    if (strictReplaceableTag && !current.is(config.replaceableTag())) {
                        continue;
                    }

                    // Optionally reduce placements adjacent to air.
                    if (config.respectAirExposure()) {
                        if (config.discardChanceOnAirExposure() > 0.0d && isExposedToAir(level, pos)) {
                            if (random.nextDouble() < config.discardChanceOnAirExposure()) {
                                continue;
                            }
                        }
                    }

                    double oreChance;
                    if (fixedLength) {
                        oreChance = config.oreChanceCenter();
                    } else {
                        double dist = Math.sqrt(distSq);
                        double ratio = dist / Math.max(1.0e-4d, (double) radius);
                        oreChance = Mth.lerp(ratio, config.oreChanceCenter(), config.oreChanceEdge());
                    }

                    double roll = random.nextDouble();
                    BlockState toPlace;
                    if (roll < oreChance) {
                        toPlace = getOreForBlock(config.ore());
                    } else if (roll < oreChance + config.hostFill()) {
                        toPlace = pickHost(config, random);
                    } else {
                        continue;
                    }

                    // Safety: never place air. If a config decoded incorrectly, placing air would
                    // effectively carve caves and make it look like the vein is "air".
                    if (toPlace.isAir()) {
                        continue;
                    }

                    level.setBlock(pos, toPlace, 2);
                    placedAny = true;
                }
            }
        }

        return placedAny;
    }

    private BlockState getOreForBlock(BlockState oreState) {
        if (oreState.is(Blocks.COAL_ORE) && BitsAndBalanceCommon.isOreVariantsEnabled()) {
            Identifier andesiteCoalId = Identifier.fromNamespaceAndPath("bitsandbalance", "andesite_coal_ore");
            var maybe = BuiltInRegistries.BLOCK.getOptional(andesiteCoalId);
            if (maybe.isPresent()) {
                Block block = maybe.get();
                if (block != Blocks.AIR) {
                    return block.defaultBlockState();
                }
            }
        }
        return oreState;
    }

    private BlockState pickHost(VeinConfiguration config, RandomSource random) {
        var options = config.hostOptions();
        if (options == null || options.isEmpty()) {
            return config.host();
        }

        int totalWeight = 0;
        for (var opt : options) {
            totalWeight += Math.max(0, opt.weight());
        }

        if (totalWeight <= 0) {
            return config.host();
        }

        int roll = random.nextInt(totalWeight);
        for (var opt : options) {
            roll -= Math.max(0, opt.weight());
            if (roll < 0) {
                return opt.state();
            }
        }

        return config.host();
    }

    private boolean isExposedToAir(WorldGenLevel level, BlockPos pos) {
        return isAirSafe(level, pos.above())
                || isAirSafe(level, pos.below())
                || isAirSafe(level, pos.north())
                || isAirSafe(level, pos.south())
                || isAirSafe(level, pos.east())
                || isAirSafe(level, pos.west());
    }

    private boolean isAirSafe(WorldGenLevel level, BlockPos pos) {
        if (!level.ensureCanWrite(pos)) {
            return false;
        }

        return level.getBlockState(pos).isAir();
    }
}
