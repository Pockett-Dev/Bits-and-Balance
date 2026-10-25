package org.onenonly.bitsandbalance.common.config;

//? if fabric
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
//? else
import org.onenonly.bitsandbalance.Config;
//? endif

public final class DoubleDoorFeatureFlags {
    private DoubleDoorFeatureFlags() {
    }

    public static boolean doubleDoorOpeningEnabled() {
        //? if fabric
        return FabricTweaksConfig.enableDoubleDoorOpening;
        //? else
        return Config.enableDoubleDoorOpening;
        //? endif
    }

    public static boolean doubleDoorCrouchSingleEnabled() {
        //? if fabric
        return FabricTweaksConfig.doubleDoorCrouchSingle;
        //? else
        return Config.doubleDoorCrouchSingle;
        //? endif
    }

    public static boolean doubleDoorChainTrapdoorsEnabled() {
        //? if fabric
        return FabricTweaksConfig.doubleDoorChainTrapdoors;
        //? else
        return Config.doubleDoorChainTrapdoors;
        //? endif
    }

    public static boolean doubleDoorSameBlockOnly() {
        //? if fabric
        return FabricTweaksConfig.doubleDoorSameBlockOnly;
        //? else
        return Config.doubleDoorSameBlockOnly;
        //? endif
    }

    public static boolean doubleDoorsWithRedstoneEnabled() {
        //? if fabric
        return FabricTweaksConfig.doubleDoorsWithRedstone;
        //? else
        return Config.doubleDoorsWithRedstone;
        //? endif
    }

    public static boolean doubleDoorRedstoneIncludesIron() {
        //? if fabric
        return FabricTweaksConfig.doubleDoorRedstoneIncludeIron;
        //? else
        return Config.doubleDoorRedstoneIncludeIron;
        //? endif
    }

    public static boolean doubleDoorRedstoneIncludesTrapdoors() {
        //? if fabric
        return FabricTweaksConfig.doubleDoorRedstoneIncludeTrapdoors;
        //? else
        return Config.doubleDoorRedstoneIncludeTrapdoors;
        //? endif
    }
}
