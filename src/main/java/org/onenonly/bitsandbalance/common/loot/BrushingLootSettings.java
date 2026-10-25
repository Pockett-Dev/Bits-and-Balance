package org.onenonly.bitsandbalance.common.loot;

import java.util.Map;

/** Config switches for built-in archaeology tables. Rewards belong to datapack loot tables. */
public final class BrushingLootSettings {
    public enum Site {
        DESERT_WELL("desert_well"), DESERT_PYRAMID("desert_pyramid"),
        TRAIL_RUINS("trail_ruins_common"), COLD_OCEAN_RUINS("ocean_ruin_cold"),
        WARM_OCEAN_RUINS("ocean_ruin_warm");
        private final String path;
        Site(String path) { this.path = path; }
        public String path() { return path; }
    }
    public record State(boolean enabled, Map<Site, Boolean> sites) {}
    private static volatile State state = new State(true, Map.of(
            Site.DESERT_WELL, true, Site.DESERT_PYRAMID, true, Site.TRAIL_RUINS, true,
            Site.COLD_OCEAN_RUINS, true, Site.WARM_OCEAN_RUINS, true));
    private BrushingLootSettings() {}
    public static State state() { return state; }
    public static void configure(boolean enabled, Map<Site, Boolean> sites) {
        state = new State(enabled, Map.copyOf(sites));
    }
    public static boolean enabled(String path) {
        State current = state;
        if (!current.enabled()) return false;
        for (Site site : Site.values())
            if (site.path().equals(path)) return current.sites().getOrDefault(site, false);
        return false;
    }
}
