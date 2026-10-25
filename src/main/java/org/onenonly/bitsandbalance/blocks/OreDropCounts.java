package org.onenonly.bitsandbalance.blocks;

import net.minecraft.util.RandomSource;

final class OreDropCounts {
    private OreDropCounts() {
    }

    static int randomBetween(RandomSource random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    static int applyVanillaOreFortune(RandomSource random, int count, int fortuneLevel) {
        if (fortuneLevel <= 0) {
            return count;
        }

        int multiplier = random.nextInt(fortuneLevel + 2) - 1;
        if (multiplier < 0) {
            multiplier = 0;
        }
        return count * (multiplier + 1);
    }
}
