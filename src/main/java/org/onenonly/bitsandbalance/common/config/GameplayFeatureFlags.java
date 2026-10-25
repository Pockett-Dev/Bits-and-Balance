package org.onenonly.bitsandbalance.common.config;

//? if fabric
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
//? else
import org.onenonly.bitsandbalance.Config;
//? endif

public final class GameplayFeatureFlags {
    private GameplayFeatureFlags() {
    }

    public static boolean foodAlwaysEdibleEnabled() {
        //? if fabric
        return FabricBalanceConfig.enableFoodAlwaysEdible;
        //? else
        return Config.enableFoodAlwaysEdible;
        //? endif
    }

    public static boolean fullEnderDragonXpEnabled() {
        //? if fabric
        return FabricBalanceConfig.enableFullEnderDragonXp;
        //? else
        return Config.enableFullEnderDragonXp;
        //? endif
    }

    public static boolean campfiresIgniteEntitiesEnabled() {
        //? if fabric
        return FabricTweaksConfig.enableCampfiresIgniteEntities;
        //? else
        return Config.enableCampfiresIgniteEntities;
        //? endif
    }

    public static boolean automaticToolRestockEnabled() {
        //? if fabric
        return FabricTweaksConfig.enableAutomaticToolRestock;
        //? else
        return Config.enableAutomaticToolRestock;
        //? endif
    }

    public static boolean automaticBlockRestockEnabled() {
        //? if fabric
        return FabricTweaksConfig.enableAutomaticBlockRestock;
        //? else
        return Config.enableAutomaticBlockRestock;
        //? endif
    }
}
