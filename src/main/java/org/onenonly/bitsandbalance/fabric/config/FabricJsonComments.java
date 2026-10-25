package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Helper for embedding per-setting descriptions in JSON configs.
 *
 * JSON doesn't support comments; we store descriptions in a reserved "description" object.
 * Gson ignores unknown fields when reading into typed config POJOs.
 */
public final class FabricJsonComments {
    public static final String COMMENTS_KEY = "description";

    private FabricJsonComments() {
    }

    public static void put(JsonObject obj, String key, String description) {
        if (obj == null || key == null || key.isBlank() || description == null || description.isBlank()) {
            return;
        }

        JsonObject comments = getOrCreate(obj);
        comments.addProperty(key, description);
    }

    public static JsonObject getOrCreate(JsonObject obj) {
        JsonElement existing = obj.get(COMMENTS_KEY);
        if (existing instanceof JsonObject o) {
            return o;
        }
        JsonObject created = new JsonObject();
        obj.add(COMMENTS_KEY, created);
        return created;
    }
}
