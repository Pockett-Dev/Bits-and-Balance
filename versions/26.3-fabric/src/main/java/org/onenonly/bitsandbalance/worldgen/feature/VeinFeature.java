package org.onenonly.bitsandbalance.worldgen.feature;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import java.util.HashSet;
import java.util.Set;

/** 26.3 data-driven form of the custom noodle vein feature. */
public final class VeinFeature implements Feature {
    public static final MapCodec<VeinFeature> CODEC = VeinConfiguration.CODEC
            .fieldOf("config")
            .xmap(VeinFeature::new, VeinFeature::configuration);

    private static final long SEED_XOR = 0x9E3779B97F4A7C15L;
    private static final long NOISE_X_XOR = 305441741L;
    private static final long NOISE_Y_XOR = 3203338804L;
    private static final long NOISE_Z_XOR = 4277009102L;

    private final VeinConfiguration configuration;

    public VeinFeature(VeinConfiguration configuration) {
        this.configuration = configuration;
    }

    public VeinConfiguration configuration() {
        return configuration;
    }

    @Override
    public MapCodec<VeinFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        return generate(level, random, origin, origin, configuration, true);
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
        // PerlinSimplexNoise was removed in 26.3. Its single-octave form was one
        // SimplexNoise sampled at the same coordinates and amplitude.
        SimplexNoise noiseX = new SimplexNoise(RandomSource.create(seed ^ NOISE_X_XOR));
        SimplexNoise noiseY = new SimplexNoise(RandomSource.create(seed ^ NOISE_Y_XOR));
        SimplexNoise noiseZ = new SimplexNoise(RandomSource.create(seed ^ NOISE_Z_XOR));

        int length = Mth.nextInt(random, config.minLength(), config.maxLength());
        double stepSize = Math.max(0.75D, config.stepSize());
        boolean fixedLength = config.minLength() == config.maxLength();
        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();
        boolean placedAny = false;
        Set<Long> visited = new HashSet<>();

        for (int i = 0; i < length; i++) {
            double frequency = config.fieldFrequency();
            double dx = noiseX.get(x * frequency, y * frequency);
            double dy = noiseY.get(y * frequency, z * frequency) * 0.65D;
            double dz = noiseZ.get(z * frequency, x * frequency);
            double magnitude = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (magnitude < 1.0E-5D) {
                magnitude = 1.0E-5D;
            }

            dx /= magnitude;
            dy /= magnitude;
            dz /= magnitude;

            double jitter = config.jitter() * 0.5D;
            dx += (random.nextDouble() - 0.5D) * jitter;
            dy += (random.nextDouble() - 0.5D) * jitter * 0.5D;
            dz += (random.nextDouble() - 0.5D) * jitter;
            x += dx * stepSize;
            y += dy * stepSize;
            z += dz * stepSize;

            float t = (float) i / (float) length;
            float radius = fixedLength
                    ? config.maxRadius()
                    : Mth.lerp(Mth.sin(t * (float) Math.PI), config.minRadius(), config.maxRadius());
            placedAny |= fillTube(
                    level,
                    random,
                    BlockPos.containing(x, y, z),
                    radius,
                    config,
                    origin,
                    fixedLength,
                    visited,
                    strictReplaceableTag
            );
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
        int searchRadius = Mth.ceil(radius + 1.0F);
        double radiusSquared = (double) radius * radius;
        boolean placedAny = false;

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    double distanceSquared = (double) dx * dx + (double) dy * dy + (double) dz * dz;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!visited.add(pos.asLong()) || !level.ensureCanWrite(pos)) {
                        continue;
                    }
                    if ((pos.getX() >> 4) != originChunkX || (pos.getZ() >> 4) != originChunkZ) {
                        continue;
                    }
                    if (config.respectAirExposure()
                            && (Math.abs(pos.getX() - origin.getX()) > 24
                            || Math.abs(pos.getZ() - origin.getZ()) > 24)) {
                        continue;
                    }

                    BlockState current = level.getBlockState(pos);
                    if (current.isAir()
                            || !current.getFluidState().isEmpty()
                            || current.is(BlockTags.FEATURES_CANNOT_REPLACE)
                            || strictReplaceableTag && !current.is(config.replaceableTag())) {
                        continue;
                    }
                    if (config.respectAirExposure()
                            && config.discardChanceOnAirExposure() > 0.0D
                            && isExposedToAir(level, pos)
                            && random.nextDouble() < config.discardChanceOnAirExposure()) {
                        continue;
                    }

                    double oreChance;
                    if (fixedLength) {
                        oreChance = config.oreChanceCenter();
                    } else {
                        double ratio = Math.sqrt(distanceSquared) / Math.max(1.0E-4D, radius);
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

                    if (!toPlace.isAir()) {
                        level.setBlock(pos, toPlace, 2);
                        placedAny = true;
                    }
                }
            }
        }
        return placedAny;
    }

    private static BlockState getOreForBlock(BlockState oreState) {
        if (oreState.is(Blocks.COAL_ORE) && BitsAndBalanceCommon.isOreVariantsEnabled()) {
            Identifier id = Identifier.fromNamespaceAndPath("bitsandbalance", "andesite_coal_ore");
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null && block != Blocks.AIR) {
                return block.defaultBlockState();
            }
        }
        return oreState;
    }

    private static BlockState pickHost(VeinConfiguration config, RandomSource random) {
        if (config.hostOptions() == null || config.hostOptions().isEmpty()) {
            return config.host();
        }

        int totalWeight = 0;
        for (VeinConfiguration.WeightedState option : config.hostOptions()) {
            totalWeight += Math.max(0, option.weight());
        }
        if (totalWeight <= 0) {
            return config.host();
        }

        int roll = random.nextInt(totalWeight);
        for (VeinConfiguration.WeightedState option : config.hostOptions()) {
            roll -= Math.max(0, option.weight());
            if (roll < 0) {
                return option.state();
            }
        }
        return config.host();
    }

    private static boolean isExposedToAir(WorldGenLevel level, BlockPos pos) {
        return isAirSafe(level, pos.above())
                || isAirSafe(level, pos.below())
                || isAirSafe(level, pos.north())
                || isAirSafe(level, pos.south())
                || isAirSafe(level, pos.east())
                || isAirSafe(level, pos.west());
    }

    private static boolean isAirSafe(WorldGenLevel level, BlockPos pos) {
        return level.ensureCanWrite(pos) && level.getBlockState(pos).isAir();
    }
}
