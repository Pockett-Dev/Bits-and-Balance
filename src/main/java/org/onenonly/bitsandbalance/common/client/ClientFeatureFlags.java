package org.onenonly.bitsandbalance.common.client;

//? if fabric
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
//? else
import org.onenonly.bitsandbalance.Config;
//? endif

public final class ClientFeatureFlags {
    private ClientFeatureFlags() {
    }

    public static boolean soulFireOverlayEnabled() {
        //? if fabric
        return FabricClientConfig.soulFireOverlayEnabled;
        //? else
        return Config.soulFireOverlayEnabled;
        //? endif
    }

    public static boolean soulFireCandleFlamesEnabled() {
        //? if fabric
        return FabricClientConfig.soulFireCandleFlamesEnabled;
        //? else
        return Config.soulFireCandleFlamesEnabled;
        //? endif
    }

    public static boolean nightVisionFadeEnabled() {
        //? if fabric
        return FabricClientConfig.enableNightVisionFade;
        //? else
        return Config.enableNightVisionFade;
        //? endif
    }

    public static int nightVisionFadeSeconds() {
        //? if fabric
        return FabricClientConfig.nightVisionFadeSeconds;
        //? else
        return Config.nightVisionFadeSeconds;
        //? endif
    }

    public static boolean showHeldItemWhenRidingEnabled() {
        //? if fabric
        return FabricClientConfig.enableShowHeldItemWhenRiding;
        //? else
        return Config.enableShowHeldItemWhenRiding;
        //? endif
    }

}
