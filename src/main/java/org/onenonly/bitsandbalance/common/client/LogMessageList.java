package org.onenonly.bitsandbalance.common.client;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Lossless text editing for message fragments, which may themselves contain list punctuation. */
public final class LogMessageList {
    private static final Gson GSON = new Gson();

    private LogMessageList() {}

    public static String format(Collection<?> messages) {
        return GSON.toJson(messages);
    }

    public static List<String> parse(String text) {
        if (text == null || text.isBlank()) return List.of();
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            try (JsonReader reader = new JsonReader(new StringReader(trimmed))) {
                reader.setLenient(false);
                reader.beginArray();
                List<String> messages = new ArrayList<>();
                while (reader.hasNext()) {
                    if (reader.peek() == JsonToken.NULL) reader.nextNull();
                    else if (reader.peek() == JsonToken.STRING) messages.add(reader.nextString());
                    else return List.of(trimmed);
                }
                reader.endArray();
                if (reader.peek() == JsonToken.END_DOCUMENT) return messages;
            } catch (IOException | RuntimeException ignored) {
                // Bracket-prefixed log text is also a valid single literal fragment.
            }
        }
        return List.of(trimmed);
    }
}
