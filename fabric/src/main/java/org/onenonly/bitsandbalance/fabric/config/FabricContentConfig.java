package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class FabricContentConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-content-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-content.json";
    private static final String LEGACY_GAMEPLAY_FILE_NAME = "bitsandbalance-gameplay.json";

    public static volatile boolean enableGlowGoo = true;
    public static volatile boolean glowGooTwoGlowInkRecipeEnabled = true;
    public static volatile boolean glowGooFourGlowInkRecipeEnabled = true;
    public static volatile boolean glowGooImpactParticlesEnabled = true;
    public static volatile int glowGooImpactParticleCount = 16;
    public static volatile boolean glowGooAmbientSplatterParticlesEnabled = true;
    public static volatile boolean glowGooBioluminescenceEnabled = true;
    public static volatile int glowGooBioluminescenceDurationSeconds = 60;
    public static volatile int glowGooBioluminescenceLightLevel = 14;

    private FabricContentConfig() {
    }

    public static void init() {
        loadOrCreateDefaults();
        applyRuntime();
    }

    private static void loadOrCreateDefaults() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);

        JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);
        JsonObject legacyGameplayRoot = FabricJsonConfigFiles.readRoot(FabricConfigPaths.resolve(LEGACY_GAMEPLAY_FILE_NAME), GSON);

        JsonObject glowGoo = getObject(root, "glowGoo");
        if (glowGoo != null) {
            enableGlowGoo = getBoolean(glowGoo, "enabled", enableGlowGoo);
            glowGooTwoGlowInkRecipeEnabled = getBoolean(glowGoo, "twoGlowInkRecipeEnabled", glowGooTwoGlowInkRecipeEnabled);
            glowGooFourGlowInkRecipeEnabled = getBoolean(glowGoo, "fourGlowInkRecipeEnabled", glowGooFourGlowInkRecipeEnabled);
            glowGooImpactParticlesEnabled = getBoolean(glowGoo, "impactParticlesEnabled", glowGooImpactParticlesEnabled);
            glowGooImpactParticleCount = getInt(glowGoo, "impactParticleCount", glowGooImpactParticleCount);
            glowGooAmbientSplatterParticlesEnabled = getBoolean(glowGoo, "ambientSplatterParticlesEnabled", glowGooAmbientSplatterParticlesEnabled);
            glowGooBioluminescenceEnabled = getBoolean(glowGoo, "bioluminescenceEnabled", glowGooBioluminescenceEnabled);
            glowGooBioluminescenceDurationSeconds = getInt(glowGoo, "bioluminescenceDurationSeconds", glowGooBioluminescenceDurationSeconds);
            glowGooBioluminescenceLightLevel = getInt(glowGoo, "bioluminescenceLightLevel", glowGooBioluminescenceLightLevel);
        } else {
            JsonObject legacyGlowGoo = getObject(legacyGameplayRoot, "glowGoo");
            if (legacyGlowGoo != null) {
                enableGlowGoo = getBoolean(legacyGlowGoo, "enabled", enableGlowGoo);
                glowGooTwoGlowInkRecipeEnabled = getBoolean(legacyGlowGoo, "twoGlowInkRecipeEnabled", glowGooTwoGlowInkRecipeEnabled);
                glowGooFourGlowInkRecipeEnabled = getBoolean(legacyGlowGoo, "fourGlowInkRecipeEnabled", glowGooFourGlowInkRecipeEnabled);
                glowGooImpactParticlesEnabled = getBoolean(legacyGlowGoo, "impactParticlesEnabled", glowGooImpactParticlesEnabled);
                glowGooImpactParticleCount = getInt(legacyGlowGoo, "impactParticleCount", glowGooImpactParticleCount);
                glowGooAmbientSplatterParticlesEnabled = getBoolean(legacyGlowGoo, "ambientSplatterParticlesEnabled", glowGooAmbientSplatterParticlesEnabled);
                glowGooBioluminescenceEnabled = getBoolean(legacyGlowGoo, "bioluminescenceEnabled", glowGooBioluminescenceEnabled);
                glowGooBioluminescenceDurationSeconds = getInt(legacyGlowGoo, "bioluminescenceDurationSeconds", glowGooBioluminescenceDurationSeconds);
                glowGooBioluminescenceLightLevel = getInt(legacyGlowGoo, "bioluminescenceLightLevel", glowGooBioluminescenceLightLevel);
            } else {
                enableGlowGoo = getBoolean(legacyGameplayRoot, "enableGlowGoo", enableGlowGoo);
            }
        }

        glowGooImpactParticleCount = Math.max(0, Math.min(64, glowGooImpactParticleCount));
        glowGooBioluminescenceDurationSeconds = Math.max(1, Math.min(600, glowGooBioluminescenceDurationSeconds));
        glowGooBioluminescenceLightLevel = Math.max(0, Math.min(15, glowGooBioluminescenceLightLevel));

        try {
            writeDefaults(configPath, root);
        } catch (IOException exception) {
            LOGGER.warn("Failed writing {} (continuing with in-memory defaults)", configPath, exception);
        }
    }

    private static void writeDefaults(Path configPath, JsonObject root) throws IOException {
        JsonObject glowGoo = new JsonObject();

        FabricJsonComments.put(root, "glowGoo", "Glow Goo item settings.");
        FabricJsonComments.put(glowGoo, "enabled", "Enable Glow Goo: throw a blob that splats into a bright, waterloggable light source.");
        FabricJsonComments.put(glowGoo, "twoGlowInkRecipeEnabled", "If true, Glow Goo can be crafted from 2 glow ink sacs plus 1 slime ball.");
        FabricJsonComments.put(glowGoo, "fourGlowInkRecipeEnabled", "If true, Glow Goo can also be crafted from 4 glow ink sacs.");
        FabricJsonComments.put(glowGoo, "impactParticlesEnabled", "If true, Glow Goo plays glow-ink particles when it hits a block or entity.");
        FabricJsonComments.put(glowGoo, "impactParticleCount", "How many glow-ink particles Glow Goo spawns on impact.");
        FabricJsonComments.put(glowGoo, "ambientSplatterParticlesEnabled", "If true, placed Goo Splatter emits ambient glow particles.");
        FabricJsonComments.put(glowGoo, "bioluminescenceEnabled", "If true, entities hit by Glow Goo gain Bioluminescence and emit light while the effect lasts.");
        FabricJsonComments.put(glowGoo, "bioluminescenceDurationSeconds", "How long Bioluminescence lasts after a Glow Goo hit, in seconds.");
        FabricJsonComments.put(glowGoo, "bioluminescenceLightLevel", "Light level emitted by Bioluminescence while active.");

        glowGoo.addProperty("enabled", enableGlowGoo);
        glowGoo.addProperty("twoGlowInkRecipeEnabled", glowGooTwoGlowInkRecipeEnabled);
        glowGoo.addProperty("fourGlowInkRecipeEnabled", glowGooFourGlowInkRecipeEnabled);
        glowGoo.addProperty("impactParticlesEnabled", glowGooImpactParticlesEnabled);
        glowGoo.addProperty("impactParticleCount", glowGooImpactParticleCount);
        glowGoo.addProperty("ambientSplatterParticlesEnabled", glowGooAmbientSplatterParticlesEnabled);
        glowGoo.addProperty("bioluminescenceEnabled", glowGooBioluminescenceEnabled);
        glowGoo.addProperty("bioluminescenceDurationSeconds", glowGooBioluminescenceDurationSeconds);
        glowGoo.addProperty("bioluminescenceLightLevel", glowGooBioluminescenceLightLevel);
        root.add("glowGoo", glowGoo);

        FabricJsonConfigFiles.writeRoot(configPath, GSON, root);
    }

    private static void applyRuntime() {
        GlowGooRuntime.enabled = enableGlowGoo;
        GlowGooRuntime.impactParticlesEnabled = glowGooImpactParticlesEnabled;
        GlowGooRuntime.impactParticleCount = glowGooImpactParticleCount;
        GlowGooRuntime.splatterAmbientParticlesEnabled = glowGooAmbientSplatterParticlesEnabled;
        GlowGooRuntime.bioluminescenceEnabled = glowGooBioluminescenceEnabled;
        GlowGooRuntime.bioluminescenceDurationTicks = glowGooBioluminescenceDurationSeconds * 20;
        GlowGooRuntime.bioluminescenceLightLevel = glowGooBioluminescenceLightLevel;
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return element instanceof JsonObject child ? child : null;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() ? element.getAsBoolean() : def;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsInt() : def;
    }
}