package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Minimal Fabric-side config for potion/brewing-related toggles.
 *
 * Config file: config/Bits and Balance/bitsandbalance-potions.json
 */
public final class FabricPotionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-potions-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-content.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-potions.json";

    public static volatile boolean enableCustomBrewing = true;

    public static volatile boolean enableWitheringPotion = true;
    public static volatile boolean enableResurfacingPotion = true;
    public static volatile boolean enableLevitationPotion = true;
    public static volatile boolean enableHastePotion = true;
    public static volatile boolean enableDisplacementPotion = true;
    public static volatile boolean enableReturningPotion = true;
    public static volatile boolean enableGlowingPotion = true;
    public static volatile boolean enableBioluminescencePotion = true;

    private FabricPotionConfig() {
    }

    public static void init() {
        loadOrCreateDefaults();
    }

    private static void loadOrCreateDefaults() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);

        JsonObject obj = FabricJsonConfigFiles.readRoot(configPath, GSON);
        JsonObject legacyObj = FabricJsonConfigFiles.readRoot(LEGACY_FILE_NAME, GSON);

        try {
            enableCustomBrewing = getBoolean(legacyObj, "enableCustomBrewing", enableCustomBrewing);
            JsonObject customBrewing = getObject(obj, "customBrewing");
            if (customBrewing == null) {
                customBrewing = getObject(legacyObj, "customBrewing");
            }
            if (customBrewing != null) {
                enableCustomBrewing = getBoolean(customBrewing, "enabled", enableCustomBrewing);
            }

            JsonObject legacyToggles = getObject(legacyObj, "enabledPotions");

            JsonObject withering = getObject(obj, "withering");
            if (withering != null) {
                enableWitheringPotion = getBoolean(withering, "enabled", enableWitheringPotion);
            } else if ((withering = getObject(legacyObj, "withering")) != null) {
                enableWitheringPotion = getBoolean(withering, "enabled", enableWitheringPotion);
            } else if (legacyToggles != null) {
                enableWitheringPotion = getBoolean(legacyToggles, "withering", enableWitheringPotion);
            } else {
                enableWitheringPotion = getBoolean(legacyObj, "enableWitheringPotion", enableWitheringPotion);
            }

            JsonObject resurfacing = getObject(obj, "resurfacing");
            if (resurfacing != null) {
                enableResurfacingPotion = getBoolean(resurfacing, "enabled", enableResurfacingPotion);
            } else if ((resurfacing = getObject(legacyObj, "resurfacing")) != null) {
                enableResurfacingPotion = getBoolean(resurfacing, "enabled", enableResurfacingPotion);
            } else if (legacyToggles != null) {
                enableResurfacingPotion = getBoolean(legacyToggles, "resurfacing", enableResurfacingPotion);
            } else {
                enableResurfacingPotion = getBoolean(legacyObj, "enableResurfacingPotion", enableResurfacingPotion);
            }

            JsonObject levitation = getObject(obj, "levitation");
            if (levitation != null) {
                enableLevitationPotion = getBoolean(levitation, "enabled", enableLevitationPotion);
            } else if ((levitation = getObject(legacyObj, "levitation")) != null) {
                enableLevitationPotion = getBoolean(levitation, "enabled", enableLevitationPotion);
            } else if (legacyToggles != null) {
                enableLevitationPotion = getBoolean(legacyToggles, "levitation", enableLevitationPotion);
            } else {
                enableLevitationPotion = getBoolean(legacyObj, "enableLevitationPotion", enableLevitationPotion);
            }

            JsonObject haste = getObject(obj, "haste");
            if (haste != null) {
                enableHastePotion = getBoolean(haste, "enabled", enableHastePotion);
            } else if ((haste = getObject(legacyObj, "haste")) != null) {
                enableHastePotion = getBoolean(haste, "enabled", enableHastePotion);
            } else if (legacyToggles != null) {
                enableHastePotion = getBoolean(legacyToggles, "haste", enableHastePotion);
            } else {
                enableHastePotion = getBoolean(legacyObj, "enableHastePotion", enableHastePotion);
            }

            JsonObject glowing = getObject(obj, "glowing");
            if (glowing != null) {
                enableGlowingPotion = getBoolean(glowing, "enabled", enableGlowingPotion);
            } else if ((glowing = getObject(legacyObj, "glowing")) != null) {
                enableGlowingPotion = getBoolean(glowing, "enabled", enableGlowingPotion);
            } else if (legacyToggles != null) {
                enableGlowingPotion = getBoolean(legacyToggles, "glowing", enableGlowingPotion);
            } else {
                enableGlowingPotion = getBoolean(legacyObj, "enableGlowingPotion", enableGlowingPotion);
            }

            JsonObject bioluminescence = getObject(obj, "bioluminescence");
            if (bioluminescence != null) {
                enableBioluminescencePotion = getBoolean(bioluminescence, "enabled", enableBioluminescencePotion);
            } else if ((bioluminescence = getObject(legacyObj, "bioluminescence")) != null) {
                enableBioluminescencePotion = getBoolean(bioluminescence, "enabled", enableBioluminescencePotion);
            } else if (legacyToggles != null) {
                enableBioluminescencePotion = getBoolean(legacyToggles, "bioluminescence", enableBioluminescencePotion);
            } else {
                enableBioluminescencePotion = getBoolean(legacyObj, "enableBioluminescencePotion", enableBioluminescencePotion);
            }

            JsonObject displacement = getObject(obj, "displacement");
            if (displacement != null) {
                enableDisplacementPotion = getBoolean(displacement, "enabled", enableDisplacementPotion);
            } else if ((displacement = getObject(legacyObj, "displacement")) != null) {
                enableDisplacementPotion = getBoolean(displacement, "enabled", enableDisplacementPotion);
            } else if (legacyToggles != null) {
                enableDisplacementPotion = getBoolean(legacyToggles, "displacement", enableDisplacementPotion);
            } else {
                enableDisplacementPotion = getBoolean(legacyObj, "enableDisplacementPotion", enableDisplacementPotion);
            }

            JsonObject returning = getObject(obj, "returning");
            if (returning != null) {
                enableReturningPotion = getBoolean(returning, "enabled", enableReturningPotion);
            } else if ((returning = getObject(legacyObj, "returning")) != null) {
                enableReturningPotion = getBoolean(returning, "enabled", enableReturningPotion);
            } else if (legacyToggles != null) {
                enableReturningPotion = getBoolean(legacyToggles, "returning", enableReturningPotion);
            } else {
                enableReturningPotion = getBoolean(legacyObj, "enableReturningPotion", enableReturningPotion);
            }

            try {
                writeDefaults(configPath);
            } catch (IOException e) {
                LOGGER.warn("Failed writing migrated {} (continuing)", configPath, e);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed reading {} (continuing with in-memory defaults)", configPath, e);
        }
    }

    private static void writeDefaults(Path configPath) throws IOException {
        JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);

        JsonObject customBrewing = new JsonObject();
        FabricJsonComments.put(root, "customBrewing", "Master toggle for Bits and Balance custom brewing recipes.");
        FabricJsonComments.put(customBrewing, "enabled", "Enable Bits and Balance custom brewing recipes.");
        customBrewing.addProperty("enabled", enableCustomBrewing);
        root.add("customBrewing", customBrewing);

        JsonObject withering = new JsonObject();
        FabricJsonComments.put(root, "withering", "Withering potion family settings.");
        FabricJsonComments.put(withering, "enabled", "Enable brewing for Withering potions.");
        FabricJsonComments.put(withering, "recipe", "Poison or Harming Potion + Wither Rose = Withering Potion.");
        FabricJsonComments.put(withering, "upgrades", "Redstone extends duration; Glowstone strengthens the potion.");
        withering.addProperty("enabled", enableWitheringPotion);
        root.add("withering", withering);

        JsonObject resurfacing = new JsonObject();
        FabricJsonComments.put(root, "resurfacing", "Resurfacing potion family settings.");
        FabricJsonComments.put(resurfacing, "enabled", "Enable the Resurfacing potion effect.");
        FabricJsonComments.put(resurfacing, "recipe", "No brewing recipe; obtained via creative mode, commands, or configured loot sources.");
        resurfacing.addProperty("enabled", enableResurfacingPotion);
        root.add("resurfacing", resurfacing);

        JsonObject displacement = new JsonObject();
        FabricJsonComments.put(root, "displacement", "Displacement potion family settings.");
        FabricJsonComments.put(displacement, "enabled", "Enable the Displacement potion effect.");
        FabricJsonComments.put(displacement, "recipe", "Awkward Potion + Ender Eye = Displacement Potion.");
        displacement.addProperty("enabled", enableDisplacementPotion);
        root.add("displacement", displacement);

        JsonObject returning = new JsonObject();
        FabricJsonComments.put(root, "returning", "Returning potion family settings.");
        FabricJsonComments.put(returning, "enabled", "Enable the Returning potion effect.");
        FabricJsonComments.put(returning, "recipe", "Displacement Potion + Echo Shard = Returning Potion.");
        returning.addProperty("enabled", enableReturningPotion);
        root.add("returning", returning);

        JsonObject levitation = new JsonObject();
        FabricJsonComments.put(root, "levitation", "Levitation potion family settings.");
        FabricJsonComments.put(levitation, "enabled", "Enable brewing for Levitation potions.");
        FabricJsonComments.put(levitation, "recipe", "Slow Falling Potion + Shulker Shell = Levitation Potion.");
        FabricJsonComments.put(levitation, "upgrades", "Redstone extends duration; Glowstone strengthens the potion.");
        levitation.addProperty("enabled", enableLevitationPotion);
        root.add("levitation", levitation);

        JsonObject haste = new JsonObject();
        FabricJsonComments.put(root, "haste", "Haste potion family settings.");
        FabricJsonComments.put(haste, "enabled", "Enable brewing for Haste potions.");
        FabricJsonComments.put(haste, "recipe", "Speed Potion + Quartz = Haste Potion.");
        FabricJsonComments.put(haste, "upgrades", "Redstone extends duration; Glowstone strengthens the potion.");
        haste.addProperty("enabled", enableHastePotion);
        root.add("haste", haste);

        JsonObject glowing = new JsonObject();
        FabricJsonComments.put(root, "glowing", "Glowing potion family settings.");
        FabricJsonComments.put(glowing, "enabled", "Enable brewing for Glowing potions.");
        FabricJsonComments.put(glowing, "recipe", "Awkward Potion + Glow Berries = Glowing Potion.");
        FabricJsonComments.put(glowing, "upgrades", "Redstone extends duration.");
        glowing.addProperty("enabled", enableGlowingPotion);
        root.add("glowing", glowing);

        JsonObject bioluminescence = new JsonObject();
        FabricJsonComments.put(root, "bioluminescence", "Bioluminescence potion family settings.");
        FabricJsonComments.put(bioluminescence, "enabled", "Enable brewing for Bioluminescence potions.");
        FabricJsonComments.put(bioluminescence, "recipe", "Glowing Potion + Glow Goo = Bioluminescence Potion.");
        FabricJsonComments.put(bioluminescence, "upgrades", "Glowstone strengthens Bioluminescence; Redstone extends Bioluminescence II to 4 minutes; Long Glowing Potion + Glow Goo brews the 8-minute base variant.");
        bioluminescence.addProperty("enabled", enableBioluminescencePotion);
        root.add("bioluminescence", bioluminescence);

        FabricJsonConfigFiles.writeRoot(configPath, GSON, root);
        if (!FILE_NAME.equals(LEGACY_FILE_NAME)) {
            FabricJsonConfigFiles.deleteIfExists(LEGACY_FILE_NAME);
        }
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el instanceof JsonObject o ? o : null;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean() ? el.getAsBoolean() : def;
    }
}
