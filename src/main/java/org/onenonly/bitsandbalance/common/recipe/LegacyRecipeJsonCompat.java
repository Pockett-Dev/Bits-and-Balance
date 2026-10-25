package org.onenonly.bitsandbalance.common.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LegacyRecipeJsonCompat {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LegacyRecipeJsonCompat() {
    }

    public static Map<String, String> normalizeFiles(Map<String, String> files) {
        Map<String, String> normalized = new LinkedHashMap<>(files.size());
        for (Map.Entry<String, String> entry : files.entrySet()) {
            normalized.put(entry.getKey(), normalizeFile(entry.getKey(), entry.getValue()));
        }
        return normalized;
    }

    public static boolean normalizeDatapackInPlace(Path datapackPath) throws IOException {
        if (datapackPath == null || !Files.isDirectory(datapackPath)) {
            return false;
        }

        boolean changed = false;
        try (var stream = Files.walk(datapackPath)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String relative = datapackPath.relativize(path).toString().replace('\\', '/');
                String original = Files.readString(path, StandardCharsets.UTF_8);
                String normalized = normalizeFile(relative, original);
                if (!original.equals(normalized)) {
                    Files.writeString(path, normalized, StandardCharsets.UTF_8);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static String normalizeFile(String path, String json) {
        if (!path.contains("/recipe/")) {
            return json;
        }

        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return json;
            }

            JsonObject object = root.getAsJsonObject();
            normalizeIngredientField(object, "ingredient");
            normalizeIngredientField(object, "ingredients");

            JsonElement key = object.get("key");
            if (key != null && key.isJsonObject()) {
                JsonObject keyObject = key.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : keyObject.entrySet()) {
                    entry.setValue(normalizeIngredientValue(entry.getValue()));
                }
            }

            return GSON.toJson(object);
        } catch (Exception ignored) {
            return json;
        }
    }

    private static void normalizeIngredientField(JsonObject object, String fieldName) {
        JsonElement value = object.get(fieldName);
        if (value != null) {
            object.add(fieldName, normalizeIngredientValue(value));
        }
    }

    private static JsonElement normalizeIngredientValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return value;
        }

        //? if mc != 1.21.1
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return value;
        }

        if (value.isJsonArray()) {
            JsonArray normalized = new JsonArray();
            for (JsonElement element : value.getAsJsonArray()) {
                normalized.add(normalizeIngredientValue(element));
            }
            return normalized;
        }

        if (!value.isJsonObject()) {
            return value;
        }

        JsonObject object = value.getAsJsonObject();
        if (object.has("fabric:type")) {
            return value;
        }

        if (object.entrySet().size() == 1 && object.has("item") && object.get("item").isJsonPrimitive()) {
            return GSON.toJsonTree(object.get("item").getAsString());
        }

        if (object.entrySet().size() == 1 && object.has("tag") && object.get("tag").isJsonPrimitive()) {
            return GSON.toJsonTree("#" + object.get("tag").getAsString());
        }

        return value;
        //? else
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            JsonObject ingredient = new JsonObject();
            String raw = value.getAsString();
            if (raw.startsWith("#")) {
                ingredient.addProperty("tag", raw.substring(1));
            } else {
                ingredient.addProperty("item", raw);
            }
            return ingredient;
        }

        if (value.isJsonArray()) {
            JsonArray normalized = new JsonArray();
            for (JsonElement element : value.getAsJsonArray()) {
                normalized.add(normalizeIngredientValue(element));
            }
            return normalized;
        }

        return value;
        //? endif
    }
}
