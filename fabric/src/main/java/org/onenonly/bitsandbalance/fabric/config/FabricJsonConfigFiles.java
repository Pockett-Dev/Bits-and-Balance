package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FabricJsonConfigFiles {
    private FabricJsonConfigFiles() {
    }

    public static JsonObject readRoot(Path path, Gson gson) {
        if (!Files.exists(path)) {
            return new JsonObject();
        }

        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement element = gson.fromJson(raw, JsonElement.class);
            return element instanceof JsonObject obj ? obj : new JsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    public static JsonObject readRoot(String fileName, Gson gson) {
        return readRoot(FabricConfigPaths.resolve(fileName), gson);
    }

    public static void writeRoot(Path path, Gson gson, JsonObject root) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, gson.toJson(root), StandardCharsets.UTF_8);
    }

    public static void deleteIfExists(String fileName) {
        try {
            Files.deleteIfExists(FabricConfigPaths.resolve(fileName));
        } catch (IOException ignored) {
        }
    }
}
