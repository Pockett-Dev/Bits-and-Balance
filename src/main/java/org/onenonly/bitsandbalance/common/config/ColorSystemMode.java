package org.onenonly.bitsandbalance.common.config;

/**
 * Legacy compatibility enum for the dyed-block registration mode.
 *
 * <ul>
 *   <li><b>BLOCK_REGISTRATION</b> (default) — registers hundreds of unique blocks per type
 *       (one per palette colour), uses {@code BlockColor} tinting, and provides creative tabs
 *       for browsing.</li>
 *   <li><b>BLOCK_ENTITY</b> — retained only so older config values can still be parsed
 *       without breaking; runtime always falls back to block registration.</li>
 * </ul>
 */
public enum ColorSystemMode {
    BLOCK_REGISTRATION,
    BLOCK_ENTITY;

    /** Parses legacy config strings, always collapsing to {@link #BLOCK_REGISTRATION}. */
    public static ColorSystemMode fromString(String value) {
        return BLOCK_REGISTRATION;
    }

    /** Returns the canonical config file representation. */
    public String toConfigString() {
        return name();
    }
}
