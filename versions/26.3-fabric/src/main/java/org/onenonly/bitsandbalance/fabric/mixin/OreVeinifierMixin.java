package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.rule.OreVeinRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adds the two configurable coal-vein variants after Minecraft's 26.3 ore material rules.
 *
 * <p>26.3 replaced {@code OreVeinifier#create} and its block-state filler with data-driven
 * {@link OreVeinRule} instances. The copper and iron evaluators are the last vanilla rules in
 * their respective height bands, so wrapping each evaluator preserves every non-null vanilla
 * result before trying the corresponding independent coal pass.</p>
 */
@Mixin(OreVeinRule.class)
public abstract class OreVeinifierMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-coal-veins");
    private static final AtomicBoolean LOGGED_WRAPPER_ACTIVE = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_DISABLED = new AtomicBoolean(false);
    private static final ConcurrentHashMap<Long, Boolean> LOGGED_FILLER_CHUNKS = new ConcurrentHashMap<>();

    private static final float MIN_FILLER_RICHNESS = 0.20F;
    private static final float MIN_RAW_BLOCK_RICHNESS = 0.28F;
    private static final float MIN_DEEP_ORE_RICHNESS = 0.18F;
    private static final int MIN_FILLER_EDGE_DISTANCE = 4;
    private static final int MIN_RAW_BLOCK_EDGE_DISTANCE = 8;
    private static final int MIN_DEEP_ORE_EDGE_DISTANCE = 4;
    private static final int MIN_DEEP_SUPPORTING_NEIGHBORS = 2;
    private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;

    private static final int COAL_SHALLOW_MIN_Y = 24;
    private static final int COAL_SHALLOW_MAX_Y = 90;
    private static final int COPPER_RULE_MIN_Y = 0;
    private static final int COPPER_RULE_MAX_Y = 49;
    private static final BlockState COAL_SHALLOW_ORE = Blocks.COAL_ORE.defaultBlockState();
    private static final BlockState COAL_SHALLOW_RAW_BLOCK = Blocks.COAL_BLOCK.defaultBlockState();
    private static final BlockState COAL_SHALLOW_FILLER = Blocks.ANDESITE.defaultBlockState();

    private static final int COAL_DEEP_MIN_Y = -64;
    private static final int COAL_DEEP_MAX_Y = 0;
    private static final int IRON_RULE_MIN_Y = -60;
    private static final int IRON_RULE_MAX_Y = -9;
    private static final BlockState COAL_DEEP_EDGE_ORE = Blocks.DEEPSLATE_COAL_ORE.defaultBlockState();
    private static final BlockState COAL_DEEP_RAW_BLOCK = Blocks.COAL_BLOCK.defaultBlockState();
    private static final BlockState COAL_DEEP_PRIMARY_FILLER = Blocks.TUFF.defaultBlockState();
    private static final BlockState COAL_DEEP_SECONDARY_FILLER = Blocks.DEEPSLATE.defaultBlockState();
    private static final float COAL_DEEP_PRIMARY_FILLER_CHANCE = 0.35F;
    private static final Identifier TUFF_COAL_ORE_ID =
            Identifier.fromNamespaceAndPath("bitsandbalance", "tuff_coal_ore");

    private static final int COAL_SHALLOW_SAMPLE_OFFSET_XZ = 4096;
    private static final int COAL_DEEP_SAMPLE_OFFSET_XZ = -4096;
    private static final int COAL_DEEP_SECONDARY_SAMPLE_OFFSET_XZ = -12288;
    private static final Identifier COAL_RANDOM_ID =
            Identifier.fromNamespaceAndPath("bitsandbalance", "coal_ore_vein");

    @Inject(method = "compile", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$wrapOreVeinRule(
            MaterialRuleContext context,
            CallbackInfoReturnable<RuleEvaluator> cir
    ) {
        OreVeinRule rule = (OreVeinRule) (Object) this;
        boolean shallowRule = rule.oreBlock().is(Blocks.COPPER_ORE);
        boolean deepRule = rule.oreBlock().is(Blocks.DEEPSLATE_IRON_ORE);
        if (!shallowRule && !deepRule) {
            return;
        }

        RuleEvaluator original = cir.getReturnValue();
        if (original == null) {
            return;
        }

        RandomState randomState = ((OreVeinMaterialRuleContextAccessor) (Object) context)
                .bitsandbalance$getRandomState();
        PositionalRandomFactory randomFactory = context.getOrCreateRandomFactory(COAL_RANDOM_ID);
        DensityFunction density = rule.density();
        DensityFunction richness = rule.richness();
        DensityFunction fillerGap = rule.fillerGap();

        cir.setReturnValue((x, y, z) -> {
            BlockState vanilla = original.tryApply(x, y, z);
            if (vanilla != null) {
                return vanilla;
            }

            boolean debug = FabricWorldgenConfig.isVeinsDebugEnabled();
            if (debug && LOGGED_WRAPPER_ACTIVE.compareAndSet(false, true)) {
                LOGGER.info(
                        "OreVeinRule wrapper active. Mojang-style coal toggles: andesite={}, tuff={}",
                        FabricWorldgenConfig.isCoalAndesiteVeinsEnabled(),
                        FabricWorldgenConfig.isCoalTuffVeinsEnabled()
                );
            }
            if (!FabricWorldgenConfig.isAnyMojangStyleCoalVeinsEnabled()) {
                if (debug && LOGGED_DISABLED.compareAndSet(false, true)) {
                    LOGGER.info("Mojang-style coal veins are disabled by config.");
                }
                return null;
            }

            // The preliminary surface can be higher than the actual terrain. Material rules
            // track depth below the exposed stone in this column, so keep its top eight
            // blocks intact even when the height estimate misses the real surface.
            if (context.stoneDepthAbove() <= 8 || y >= context.getMinSurfaceLevel()) {
                return null;
            }

            BlockState coal = null;
            if (deepRule && FabricWorldgenConfig.isCoalTuffVeinsEnabled()) {
                coal = bitsandbalance$calculateDeepCoalVein(
                        randomState, density, richness, fillerGap, randomFactory, x, y, z
                );
                if (coal != null) {
                    bitsandbalance$logCoalVeinPlacement(
                            debug, x, y, z, coal,
                            COAL_DEEP_PRIMARY_FILLER, COAL_DEEP_SECONDARY_FILLER, true
                    );
                }
            } else if (shallowRule && FabricWorldgenConfig.isCoalAndesiteVeinsEnabled()) {
                coal = bitsandbalance$calculateCoalVein(
                        randomState,
                        density,
                        richness,
                        fillerGap,
                        randomFactory,
                        x,
                        y,
                        z,
                        COAL_SHALLOW_MIN_Y,
                        COAL_SHALLOW_MAX_Y,
                        COPPER_RULE_MIN_Y,
                        COPPER_RULE_MAX_Y,
                        COAL_SHALLOW_ORE,
                        COAL_SHALLOW_ORE,
                        COAL_SHALLOW_RAW_BLOCK,
                        COAL_SHALLOW_FILLER,
                        COAL_SHALLOW_FILLER,
                        COAL_SHALLOW_SAMPLE_OFFSET_XZ,
                        0.0F,
                        0,
                        0
                );
                if (coal != null) {
                    bitsandbalance$logCoalVeinPlacement(
                            debug, x, y, z, coal,
                            COAL_SHALLOW_FILLER, COAL_SHALLOW_FILLER, false
                    );
                }
            }
            return coal;
        });
    }

    private static BlockState bitsandbalance$calculateDeepCoalVein(
            RandomState randomState,
            DensityFunction density,
            DensityFunction richness,
            DensityFunction fillerGap,
            PositionalRandomFactory randomFactory,
            int x,
            int y,
            int z
    ) {
        BlockState result = bitsandbalance$calculateCoalVein(
                randomState, density, richness, fillerGap, randomFactory, x, y, z,
                COAL_DEEP_MIN_Y, COAL_DEEP_MAX_Y, IRON_RULE_MIN_Y, IRON_RULE_MAX_Y,
                bitsandbalance$deepCoalOre(), COAL_DEEP_EDGE_ORE, COAL_DEEP_RAW_BLOCK,
                COAL_DEEP_PRIMARY_FILLER, COAL_DEEP_SECONDARY_FILLER,
                COAL_DEEP_SAMPLE_OFFSET_XZ,
                MIN_DEEP_ORE_RICHNESS, MIN_DEEP_ORE_EDGE_DISTANCE,
                MIN_DEEP_SUPPORTING_NEIGHBORS
        );
        if (result != null) {
            return result;
        }

        return bitsandbalance$calculateCoalVein(
                randomState, density, richness, fillerGap, randomFactory, x, y, z,
                COAL_DEEP_MIN_Y, COAL_DEEP_MAX_Y, IRON_RULE_MIN_Y, IRON_RULE_MAX_Y,
                bitsandbalance$deepCoalOre(), COAL_DEEP_EDGE_ORE, COAL_DEEP_RAW_BLOCK,
                COAL_DEEP_PRIMARY_FILLER, COAL_DEEP_SECONDARY_FILLER,
                COAL_DEEP_SECONDARY_SAMPLE_OFFSET_XZ,
                MIN_DEEP_ORE_RICHNESS, MIN_DEEP_ORE_EDGE_DISTANCE,
                MIN_DEEP_SUPPORTING_NEIGHBORS
        );
    }

    private static BlockState bitsandbalance$calculateCoalVein(
            RandomState randomState,
            DensityFunction density,
            DensityFunction richness,
            DensityFunction fillerGap,
            PositionalRandomFactory randomFactory,
            int x,
            int y,
            int z,
            int minY,
            int maxY,
            int sourceMinY,
            int sourceMaxY,
            BlockState coreOre,
            BlockState edgeOre,
            BlockState rawBlock,
            BlockState primaryFiller,
            BlockState secondaryFiller,
            int sampleOffsetXZ,
            float minOreRichness,
            int minOreEdgeDistance,
            int minSupportingNeighbors
    ) {
        if (y < minY || y > maxY) {
            return null;
        }

        int sampleY = bitsandbalance$remapY(y, minY, maxY, sourceMinY, sourceMaxY);
        int sampleX = x + sampleOffsetXZ;
        int sampleZ = z + sampleOffsetXZ;
        float veinDensity = randomState.sampleBlockValueUncached(density, sampleX, sampleY, sampleZ);
        if (veinDensity <= 0.0F) {
            return null;
        }

        RandomSource random = SharedConstants.debugGenerateSquareTerrainWithoutNoise
                ? RandomSource.create(0L)
                : randomFactory.at(sampleX, y, sampleZ);
        if (random.nextFloat() > veinDensity) {
            return null;
        }

        float richnessValue = Mth.clamp(
                randomState.sampleBlockValueUncached(richness, sampleX, sampleY, sampleZ),
                0.0F,
                1.0F
        );
        float gapValue = randomState.sampleBlockValueUncached(fillerGap, sampleX, sampleY, sampleZ);
        int edge = Math.min(maxY - y, y - minY);

        if (minSupportingNeighbors > 0
                && bitsandbalance$countPlacedNeighbors(
                        randomState, density, richness, fillerGap, randomFactory,
                        x, y, z, minY, maxY, sourceMinY, sourceMaxY,
                        sampleOffsetXZ, minOreRichness, minOreEdgeDistance
                ) < minSupportingNeighbors) {
            return null;
        }

        BlockState selectedFiller = bitsandbalance$selectFiller(
                primaryFiller, secondaryFiller, randomFactory, x, y, z, sampleOffsetXZ
        );
        if (random.nextFloat() < richnessValue && gapValue < 0.0F) {
            boolean allowOre = richnessValue >= minOreRichness && edge >= minOreEdgeDistance;
            boolean allowRawBlock = richnessValue >= MIN_RAW_BLOCK_RICHNESS
                    && edge >= MIN_RAW_BLOCK_EDGE_DISTANCE;
            if (allowOre) {
                BlockState ore = bitsandbalance$selectOre(
                        coreOre, edgeOre, selectedFiller, primaryFiller, secondaryFiller
                );
                return allowRawBlock && random.nextFloat() < CHANCE_OF_RAW_ORE_BLOCK ? rawBlock : ore;
            }
        }

        if (richnessValue >= MIN_FILLER_RICHNESS
                && edge >= MIN_FILLER_EDGE_DISTANCE
                && gapValue < 0.0F) {
            return selectedFiller;
        }
        return null;
    }

    private static int bitsandbalance$countPlacedNeighbors(
            RandomState randomState,
            DensityFunction density,
            DensityFunction richness,
            DensityFunction fillerGap,
            PositionalRandomFactory randomFactory,
            int x,
            int y,
            int z,
            int minY,
            int maxY,
            int sourceMinY,
            int sourceMaxY,
            int sampleOffsetXZ,
            float minOreRichness,
            int minOreEdgeDistance
    ) {
        int support = 0;
        support += bitsandbalance$wouldPlaceVeinMaterial(randomState, density, richness, fillerGap, randomFactory, x + 1, y, z, minY, maxY, sourceMinY, sourceMaxY, sampleOffsetXZ, minOreRichness, minOreEdgeDistance) ? 1 : 0;
        support += bitsandbalance$wouldPlaceVeinMaterial(randomState, density, richness, fillerGap, randomFactory, x - 1, y, z, minY, maxY, sourceMinY, sourceMaxY, sampleOffsetXZ, minOreRichness, minOreEdgeDistance) ? 1 : 0;
        support += bitsandbalance$wouldPlaceVeinMaterial(randomState, density, richness, fillerGap, randomFactory, x, y + 1, z, minY, maxY, sourceMinY, sourceMaxY, sampleOffsetXZ, minOreRichness, minOreEdgeDistance) ? 1 : 0;
        support += bitsandbalance$wouldPlaceVeinMaterial(randomState, density, richness, fillerGap, randomFactory, x, y - 1, z, minY, maxY, sourceMinY, sourceMaxY, sampleOffsetXZ, minOreRichness, minOreEdgeDistance) ? 1 : 0;
        support += bitsandbalance$wouldPlaceVeinMaterial(randomState, density, richness, fillerGap, randomFactory, x, y, z + 1, minY, maxY, sourceMinY, sourceMaxY, sampleOffsetXZ, minOreRichness, minOreEdgeDistance) ? 1 : 0;
        support += bitsandbalance$wouldPlaceVeinMaterial(randomState, density, richness, fillerGap, randomFactory, x, y, z - 1, minY, maxY, sourceMinY, sourceMaxY, sampleOffsetXZ, minOreRichness, minOreEdgeDistance) ? 1 : 0;
        return support;
    }

    private static boolean bitsandbalance$wouldPlaceVeinMaterial(
            RandomState randomState,
            DensityFunction density,
            DensityFunction richness,
            DensityFunction fillerGap,
            PositionalRandomFactory randomFactory,
            int x,
            int y,
            int z,
            int minY,
            int maxY,
            int sourceMinY,
            int sourceMaxY,
            int sampleOffsetXZ,
            float minOreRichness,
            int minOreEdgeDistance
    ) {
        if (y < minY || y > maxY) {
            return false;
        }

        int sampleY = bitsandbalance$remapY(y, minY, maxY, sourceMinY, sourceMaxY);
        int sampleX = x + sampleOffsetXZ;
        int sampleZ = z + sampleOffsetXZ;
        float veinDensity = randomState.sampleBlockValueUncached(density, sampleX, sampleY, sampleZ);
        if (veinDensity <= 0.0F) {
            return false;
        }

        RandomSource random = SharedConstants.debugGenerateSquareTerrainWithoutNoise
                ? RandomSource.create(0L)
                : randomFactory.at(sampleX, y, sampleZ);
        if (random.nextFloat() > veinDensity) {
            return false;
        }

        float richnessValue = Mth.clamp(
                randomState.sampleBlockValueUncached(richness, sampleX, sampleY, sampleZ),
                0.0F,
                1.0F
        );
        float gapValue = randomState.sampleBlockValueUncached(fillerGap, sampleX, sampleY, sampleZ);
        if (gapValue >= 0.0F) {
            return false;
        }

        int edge = Math.min(maxY - y, y - minY);
        if (random.nextFloat() < richnessValue
                && richnessValue >= minOreRichness
                && edge >= minOreEdgeDistance) {
            return true;
        }
        return richnessValue >= MIN_FILLER_RICHNESS && edge >= MIN_FILLER_EDGE_DISTANCE;
    }

    private static int bitsandbalance$remapY(
            int y,
            int minY,
            int maxY,
            int sourceMinY,
            int sourceMaxY
    ) {
        if (maxY == minY) {
            return sourceMinY;
        }
        double fraction = (double) (y - minY) / (double) (maxY - minY);
        return Mth.clamp(
                (int) Math.round(Mth.lerp(fraction, (double) sourceMinY, (double) sourceMaxY)),
                sourceMinY,
                sourceMaxY
        );
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
            int x,
            int y,
            int z,
            int sampleOffsetXZ
    ) {
        if (primaryFiller == secondaryFiller) {
            return primaryFiller;
        }
        RandomSource random = SharedConstants.debugGenerateSquareTerrainWithoutNoise
                ? RandomSource.create(0L)
                : randomFactory.at(
                        x + sampleOffsetXZ + 2048,
                        y,
                        z + sampleOffsetXZ + 2048
                );
        return random.nextFloat() < COAL_DEEP_PRIMARY_FILLER_CHANCE
                ? primaryFiller
                : secondaryFiller;
    }

    private static void bitsandbalance$logCoalVeinPlacement(
            boolean debug,
            int x,
            int y,
            int z,
            BlockState placed,
            BlockState primaryFiller,
            BlockState secondaryFiller,
            boolean deep
    ) {
        if (!debug) {
            return;
        }
        if (placed.getBlock() == primaryFiller.getBlock()
                || placed.getBlock() == secondaryFiller.getBlock()) {
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunkKey = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
            long key = chunkKey ^ (deep ? Long.MIN_VALUE : 0L);
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
                    placed.getBlock()
            );
            return;
        }

        LOGGER.info(
                "Coal vein ore placed (variant={}) at ({}, {}, {}) -> {}",
                deep ? "deep_tuff" : "shallow_andesite",
                x,
                y,
                z,
                placed.getBlock()
        );
    }
}

@Mixin(MaterialRuleContext.class)
interface OreVeinMaterialRuleContextAccessor {
    @Accessor("randomState")
    RandomState bitsandbalance$getRandomState();
}
