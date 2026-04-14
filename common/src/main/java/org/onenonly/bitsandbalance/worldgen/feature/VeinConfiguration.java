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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

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
) implements FeatureConfiguration {

        /**
         * Worldgen JSON historically used the SNBT-ish blockstate object format:
         *   {"Name":"minecraft:stone","Properties":{...}}
         *
         * Some environments/versions can be picky about casing or may provide plain strings.
         * This codec accepts:
         * - "minecraft:stone"
         * - {"Name":"minecraft:stone"} / {"name":"minecraft:stone"} / {"id":"minecraft:stone"}
         * - optional Properties/properties as a string map
         */
        public static final Codec<BlockState> FLEX_BLOCKSTATE_CODEC = new Codec<>() {
                @Override
                public <T> DataResult<Pair<BlockState, T>> decode(DynamicOps<T> ops, T input) {
                        if (ops instanceof JsonOps && input instanceof JsonElement json) {
                                return decodeFromJson(json).map(state -> Pair.of(state, input));
                        }

                        // Fallback: try string value for non-JSON ops.
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
                                        JsonElement v = props.get(key);
                                        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) {
                                                continue;
                                        }

                                        Property<?> property = current.getBlock().getStateDefinition().getProperty(key);
                                        if (property == null) {
                                                continue;
                                        }

                                        current = setPropertyFromString(current, property, v.getAsString());
                                }

                                return current;
                        });
                }

                private static String getString(JsonObject obj, String key) {
                        JsonElement el = obj.get(key);
                        if (el == null || !el.isJsonPrimitive()) return null;
                        JsonPrimitive p = el.getAsJsonPrimitive();
                        return p.isString() ? p.getAsString() : null;
                }

                private DataResult<BlockState> decodeFromString(String idString) {
                        Identifier id;
                        try {
                                id = Identifier.parse(idString);
                        } catch (Exception e) {
                                return DataResult.error(() -> "Invalid block id: " + idString);
                        }

                        var maybe = BuiltInRegistries.BLOCK.getOptional(id);
                        if (maybe.isEmpty() || maybe.get() == Blocks.AIR) {
                                return DataResult.error(() -> "Unknown block id (resolved to air): " + idString);
                        }
                        return DataResult.success(maybe.get().defaultBlockState());
                }

                private static <T extends Comparable<T>> BlockState setPropertyFromString(BlockState state, Property<T> property, String value) {
                        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
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
