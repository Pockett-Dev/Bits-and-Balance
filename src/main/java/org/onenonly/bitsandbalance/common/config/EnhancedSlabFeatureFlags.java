package org.onenonly.bitsandbalance.common.config;

//? if fabric
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
//? else
import org.onenonly.bitsandbalance.Config;
//? endif

public final class EnhancedSlabFeatureFlags {
    private EnhancedSlabFeatureFlags() {
    }

    public static boolean enabled() {
        //? if fabric
        return FabricTweaksConfig.enableEnhancedSlabs;
        //? else
        return Config.enableEnhancedSlabs;
        //? endif
    }
}
