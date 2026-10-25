package org.onenonly.bitsandbalance.fabric.worldgen;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class FabricWorldgen {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-fabric-worldgen");

    private static final Gson GSON = new Gson();

    /**
     * Keep Fabric behavior aligned with NeoForge by consuming the same biome modifier data.
     *
     * NeoForge uses data-driven biome modifiers at:
     * data/bitsandbalance/neoforge/biome_modifier/*.json
     *
     * On Fabric, we interpret those files and apply equivalent biome injections.
     */
    private static final String NEOF_MIMIC_DIR = "data/" + BitsAndBalanceCommon.MOD_ID + "/neoforge/biome_modifier";

    private FabricWorldgen() {
    }

    public static void init() {
        int applied = applyNeoForgeBiomeModifiers();
        if (applied == 0) {
            LOGGER.warn("No NeoForge biome modifiers were applied on Fabric; worldgen parity data was not loaded from {}", NEOF_MIMIC_DIR);
        }
    }

    private static int applyNeoForgeBiomeModifiers() {
        var containerOpt = FabricLoader.getInstance().getModContainer(BitsAndBalanceCommon.MOD_ID);
        if (containerOpt.isEmpty()) {
            LOGGER.warn("Mod container not found; cannot load biome modifiers");
            return 0;
        }

        var dirOpt = containerOpt.get().findPath(NEOF_MIMIC_DIR);
        if (dirOpt.isEmpty()) {
            LOGGER.warn("Biome modifier directory not found: {}", NEOF_MIMIC_DIR);
            return 0;
        }

        Path dir = dirOpt.get();
        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(dir)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(files::add);
        } catch (IOException e) {
            LOGGER.warn("Failed listing biome modifier directory: {}", dir, e);
            return 0;
        }

        int applied = 0;
        for (Path file : files) {
            try {
                String raw = Files.readString(file, StandardCharsets.UTF_8);
                JsonElement parsed = GSON.fromJson(raw, JsonElement.class);
                if (!(parsed instanceof JsonObject obj)) {
                    throw new JsonParseException("Expected JSON object");
                }

                // We only care about neoforge:add_features
                String type = getString(obj, "type");
                if (!"neoforge:add_features".equals(type)) {
                    continue;
                }

                String biomesSpec = getString(obj, "biomes");
                String stepSpec = getString(obj, "step");
                if (biomesSpec == null || stepSpec == null) {
                    continue;
                }

                var selector = parseBiomeSelector(biomesSpec);
                var step = parseDecorationStep(stepSpec);
                if (selector == null || step == null) {
                    continue;
                }

                List<Identifier> placedIds = parseFeatures(obj.get("features"));
                if (!placedIds.isEmpty()) {
                    LOGGER.info(
                            "Applying biome modifier {}: biomes={} step={} features={}",
                            file.getFileName(),
                            biomesSpec,
                            stepSpec,
                            placedIds
                    );
                }

                for (Identifier placedId : placedIds) {
                    BiomeModifications.addFeature(selector, step, ResourceKey.create(Registries.PLACED_FEATURE, placedId));
                    applied++;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed parsing biome modifier: {}", file, e);
            }
        }

        if (applied > 0) {
            LOGGER.info("Applied {} biome modifier feature injections from {}", applied, NEOF_MIMIC_DIR);
        }

        return applied;
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            return null;
        }
        return el.getAsString();
    }

    private static Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext> parseBiomeSelector(String spec) {
        // NeoForge uses either biome tags ("#minecraft:is_nether") or direct biome keys.
        if (spec.startsWith("#")) {
            // NeoForge commonly uses these dimension-ish tags.
            // On Fabric, different datapacks/mods (notably Terralith/Tectonic) may not populate
            // the vanilla biome tags consistently, so we widen selection to include:
            // - vanilla tag
            // - common "c:" tag (when present)
            // - Fabric's built-in selector
            // - a conservative fallback (for overworld only): "not nether" and "not end"
            if ("#minecraft:is_overworld".equals(spec)) {
                TagKey<Biome> vanilla = TagKey.create(Registries.BIOME, Identifier.parse("minecraft:is_overworld"));
                TagKey<Biome> common = TagKey.create(Registries.BIOME, Identifier.parse("c:is_overworld"));

                Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext> notNether = BiomeSelectors.foundInTheNether().negate();
                Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext> notEnd = BiomeSelectors.foundInTheEnd().negate();

                return BiomeSelectors.tag(vanilla)
                        .or(BiomeSelectors.tag(common))
                        .or(BiomeSelectors.foundInOverworld())
                        .or(BiomeSelectors.all().and(notNether).and(notEnd));
            }
            if ("#minecraft:is_nether".equals(spec)) {
                TagKey<Biome> vanilla = TagKey.create(Registries.BIOME, Identifier.parse("minecraft:is_nether"));
                TagKey<Biome> common = TagKey.create(Registries.BIOME, Identifier.parse("c:is_nether"));
                return BiomeSelectors.tag(vanilla)
                        .or(BiomeSelectors.tag(common))
                        .or(BiomeSelectors.foundInTheNether());
            }
            if ("#minecraft:is_end".equals(spec)) {
                TagKey<Biome> vanilla = TagKey.create(Registries.BIOME, Identifier.parse("minecraft:is_end"));
                TagKey<Biome> common = TagKey.create(Registries.BIOME, Identifier.parse("c:is_end"));
                return BiomeSelectors.tag(vanilla)
                        .or(BiomeSelectors.tag(common))
                        .or(BiomeSelectors.foundInTheEnd());
            }

            Identifier tagId = Identifier.parse(spec.substring(1));
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, tagId);
            return BiomeSelectors.tag(tag);
        }

        Identifier biomeId = Identifier.parse(spec);
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
        return BiomeSelectors.includeByKey(biomeKey);
    }

    private static GenerationStep.Decoration parseDecorationStep(String spec) {
        // NeoForge JSON uses lowercase snake-case (e.g. "underground_ores").
        // The vanilla enum is UPPER_SNAKE.
        try {
            return GenerationStep.Decoration.valueOf(spec.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown decoration step: {}", spec);
            return null;
        }
    }

    private static List<Identifier> parseFeatures(JsonElement el) {
        List<Identifier> out = new ArrayList<>();
        if (el == null) {
            return out;
        }

        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            out.add(Identifier.parse(el.getAsString()));
            return out;
        }

        if (el.isJsonArray()) {
            for (JsonElement entry : el.getAsJsonArray()) {
                if (entry != null && entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                    out.add(Identifier.parse(entry.getAsString()));
                }
            }
        }

        return out;
    }
}
