package org.onenonly.bitsandbalance.common.recipe;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class DynamicRecipeInputFingerprint {
    private DynamicRecipeInputFingerprint() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final MessageDigest digest;

        private Builder() {
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 not available", e);
            }
        }

        public Builder add(String value) {
            String normalized = value == null ? "<null>" : value;
            digest.update(normalized.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            return this;
        }

        public Builder add(String key, boolean value) {
            return add(key + "=" + value);
        }

        public Builder add(String key, int value) {
            return add(key + "=" + value);
        }

        public Builder add(String key, long value) {
            return add(key + "=" + value);
        }

        public Builder add(String key, String value) {
            return add(key + "=" + value);
        }

        public String build() {
            byte[] bytes = digest.digest();
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(Character.forDigit((value >> 4) & 0xF, 16));
                builder.append(Character.forDigit(value & 0xF, 16));
            }
            return builder.toString();
        }
    }
}