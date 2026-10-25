package org.onenonly.bitsandbalance.worldgen.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

/**
 * Data carried directly by the 26.3 {@link VeinFeature} codec.
 *
 * <p>Minecraft 26.3 removed {@code FeatureConfiguration}; feature instances now own their
 * configuration. Keeping this record separate preserves the existing JSON shape and the
 * shared generation logic without depending on the removed interface.</p>
 */
public record VeinConfiguration(
        TagKey<Block> replaceableTag,
        BlockState host,
        List<WeightedState> hostOptions,
        BlockState ore,
        double oreChanceCenter,
        double oreChanceEdge,
        double hostFill,
        int minLength,
        int maxLength,
        float minRadius,
        float maxRadius,
        double fieldFrequency,
        double stepSize,
        double jitter,
        boolean respectAirExposure,
        double discardChanceOnAirExposure
) {
    /**
     * Accepts both the historical SNBT-like object format and a plain block identifier.
     * Keeping this decoder preserves compatibility with existing Bits & Balance datapacks.
     */
    public static final Codec<BlockState> FLEX_BLOCKSTATE_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<BlockState, T>> decode(DynamicOps<T> ops, T input) {
            if (ops instanceof JsonOps && input instanceof JsonElement json) {
                return decodeFromJson(json).map(state -> Pair.of(state, input));
            }

            return ops.getStringValue(input)
                    .flatMap(this::decodeFromString)
                    .map(state -> Pair.of(state, input));
        }

        @Override
        public <T> DataResult<T> encode(BlockState input, DynamicOps<T> ops, T prefix) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(input.getBlock());
            if (id == null) {
                return DataResult.error(() -> "Unknown block for state: " + input);
            }
            return ops.mergeToPrimitive(prefix, ops.createString(id.toString()));
        }

        private DataResult<BlockState> decodeFromJson(JsonElement json) {
            if (json == null || json.isJsonNull()) {
                return DataResult.error(() -> "Expected blockstate, got null");
            }

            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                return decodeFromString(json.getAsString());
            }

            if (!json.isJsonObject()) {
                return DataResult.error(() -> "Expected blockstate object or string, got: " + json);
            }

            JsonObject obj = json.getAsJsonObject();
            String idString = getString(obj, "Name");
            if (idString == null) idString = getString(obj, "name");
            if (idString == null) idString = getString(obj, "id");
            if (idString == null) {
                return DataResult.error(() -> "Missing block id field (Name/name/id) in: " + obj);
            }

            return decodeFromString(idString).map(state -> {
                JsonObject props = null;
                JsonElement propsEl = obj.get("Properties");
                if (propsEl == null) propsEl = obj.get("properties");
                if (propsEl != null && propsEl.isJsonObject()) {
                    props = propsEl.getAsJsonObject();
                }

                if (props == null || props.isEmpty()) {
                    return state;
                }

                BlockState current = state;
                for (String key : props.keySet()) {
                    JsonElement value = props.get(key);
                    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                        continue;
                    }

                    Property<?> property = current.getBlock().getStateDefinition().getProperty(key);
                    if (property != null) {
                        current = setPropertyFromString(current, property, value.getAsString());
                    }
                }
                return current;
            });
        }

        private static String getString(JsonObject obj, String key) {
            JsonElement element = obj.get(key);
            if (element == null || !element.isJsonPrimitive()) return null;
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.isString() ? primitive.getAsString() : null;
        }

        private DataResult<BlockState> decodeFromString(String idString) {
            Identifier id;
            try {
                id = Identifier.parse(idString);
            } catch (Exception exception) {
                return DataResult.error(() -> "Invalid block id: " + idString);
            }

            var block = BuiltInRegistries.BLOCK.getOptional(id);
            if (block.isEmpty() || block.get() == Blocks.AIR) {
                return DataResult.error(() -> "Unknown block id (resolved to air): " + idString);
            }
            return DataResult.success(block.get().defaultBlockState());
        }

        private static <T extends Comparable<T>> BlockState setPropertyFromString(
                BlockState state,
                Property<T> property,
                String value
        ) {
            return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
        }
    };

    public static final Codec<WeightedState> WEIGHTED_STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FLEX_BLOCKSTATE_CODEC.fieldOf("state").forGetter(WeightedState::state),
            Codec.INT.fieldOf("weight").forGetter(WeightedState::weight)
    ).apply(instance, WeightedState::new));

    public static final Codec<VeinConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.hashedCodec(Registries.BLOCK).fieldOf("replaceable_tag").forGetter(VeinConfiguration::replaceableTag),
            FLEX_BLOCKSTATE_CODEC.fieldOf("host").forGetter(VeinConfiguration::host),
            WEIGHTED_STATE_CODEC.listOf().optionalFieldOf("host_options", List.of()).forGetter(VeinConfiguration::hostOptions),
            FLEX_BLOCKSTATE_CODEC.fieldOf("ore").forGetter(VeinConfiguration::ore),
            Codec.doubleRange(0.0, 1.0).fieldOf("ore_chance_center").forGetter(VeinConfiguration::oreChanceCenter),
            Codec.doubleRange(0.0, 1.0).fieldOf("ore_chance_edge").forGetter(VeinConfiguration::oreChanceEdge),
            Codec.doubleRange(0.0, 1.0).fieldOf("host_fill").forGetter(VeinConfiguration::hostFill),
            Codec.INT.fieldOf("min_length").forGetter(VeinConfiguration::minLength),
            Codec.INT.fieldOf("max_length").forGetter(VeinConfiguration::maxLength),
            Codec.floatRange(0.0f, 64.0f).fieldOf("min_radius").forGetter(VeinConfiguration::minRadius),
            Codec.floatRange(0.0f, 64.0f).fieldOf("max_radius").forGetter(VeinConfiguration::maxRadius),
            Codec.DOUBLE.fieldOf("field_frequency").forGetter(VeinConfiguration::fieldFrequency),
            Codec.DOUBLE.fieldOf("step_size").forGetter(VeinConfiguration::stepSize),
            Codec.DOUBLE.fieldOf("jitter").forGetter(VeinConfiguration::jitter),
            Codec.BOOL.fieldOf("respect_air_exposure").forGetter(VeinConfiguration::respectAirExposure),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("discard_chance_on_air_exposure", 0.0).forGetter(VeinConfiguration::discardChanceOnAirExposure)
    ).apply(instance, VeinConfiguration::new));

    public record WeightedState(BlockState state, int weight) {
    }
}
