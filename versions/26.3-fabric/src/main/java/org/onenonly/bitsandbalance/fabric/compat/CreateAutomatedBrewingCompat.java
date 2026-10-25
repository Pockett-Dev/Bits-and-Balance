package org.onenonly.bitsandbalance.fabric.compat;

public final class CreateAutomatedBrewingCompat {
    private CreateAutomatedBrewingCompat() {
    }

    public static void init() {
        // Minecraft 26.3 loads brewing mixes from recipe JSONs. Create can consume
        // the same recipe data without reflective PotionBrewing registration.
    }
}
