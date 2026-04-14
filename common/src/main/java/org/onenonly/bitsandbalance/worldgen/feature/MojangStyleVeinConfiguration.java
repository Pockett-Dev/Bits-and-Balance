package org.onenonly.bitsandbalance.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for a Mojang-style (OreVeinifier-like) multi-chunk vein pass.
 *
 * This is NOT a vanilla copy; it mirrors the core ore-veinifier decision logic (toggle/ridged/gap +
 * positional RNG) while using deterministic noise functions inside a placed feature.
 */
public record MojangStyleVeinConfiguration(
        TagKey<Block> replaceableTag,
        BlockState filler,
        BlockState ore,
        BlockState rawBlock,
        int minY,
        int maxY,
        double frequency,
    double sampleChance,
    double chunkChance,
    int patchSizeXZ,
    double gapThreshold,
    double richnessMultiplier,
        int sampleOffsetXZ,
        boolean requirePositiveToggle,
        float veinSolidness,
        float chanceOfRawBlock
) implements FeatureConfiguration {

    public static final Codec<MojangStyleVeinConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.hashedCodec(Registries.BLOCK).fieldOf("replaceable_tag").forGetter(MojangStyleVeinConfiguration::replaceableTag),
            VeinConfiguration.FLEX_BLOCKSTATE_CODEC.fieldOf("filler").forGetter(MojangStyleVeinConfiguration::filler),
            VeinConfiguration.FLEX_BLOCKSTATE_CODEC.fieldOf("ore").forGetter(MojangStyleVeinConfiguration::ore),
            VeinConfiguration.FLEX_BLOCKSTATE_CODEC.optionalFieldOf("raw_block", Blocks.AIR.defaultBlockState()).forGetter(MojangStyleVeinConfiguration::rawBlock),
            Codec.INT.fieldOf("min_y").forGetter(MojangStyleVeinConfiguration::minY),
            Codec.INT.fieldOf("max_y").forGetter(MojangStyleVeinConfiguration::maxY),
            Codec.DOUBLE.optionalFieldOf("frequency", 0.005D).forGetter(MojangStyleVeinConfiguration::frequency),
            Codec.DOUBLE.optionalFieldOf("sample_chance", 0.02D).forGetter(MojangStyleVeinConfiguration::sampleChance),
            Codec.DOUBLE.optionalFieldOf("chunk_chance", 1.0D).forGetter(MojangStyleVeinConfiguration::chunkChance),
            Codec.INT.optionalFieldOf("patch_size_xz", 16).forGetter(MojangStyleVeinConfiguration::patchSizeXZ),
            Codec.DOUBLE.optionalFieldOf("gap_threshold", -0.30000001192092896D).forGetter(MojangStyleVeinConfiguration::gapThreshold),
            Codec.DOUBLE.optionalFieldOf("richness_multiplier", 1.0D).forGetter(MojangStyleVeinConfiguration::richnessMultiplier),
            Codec.INT.optionalFieldOf("sample_offset_xz", 0).forGetter(MojangStyleVeinConfiguration::sampleOffsetXZ),
            Codec.BOOL.optionalFieldOf("require_positive_toggle", false).forGetter(MojangStyleVeinConfiguration::requirePositiveToggle),
            Codec.FLOAT.optionalFieldOf("vein_solidness", 0.7F).forGetter(MojangStyleVeinConfiguration::veinSolidness),
            Codec.FLOAT.optionalFieldOf("chance_of_raw_block", 0.02F).forGetter(MojangStyleVeinConfiguration::chanceOfRawBlock)
    ).apply(instance, MojangStyleVeinConfiguration::new));
}
