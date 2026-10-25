package org.onenonly.bitsandbalance.common.config;

//? if fabric
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
//? else
import org.onenonly.bitsandbalance.Config;
//? endif

public final class BalanceFeatureFlags {
    private BalanceFeatureFlags() {
    }

    public static boolean balancedElytraEnabled() {
        //? if fabric
        return FabricBalanceConfig.enableBalancedElytra;
        //? else
        return Config.enableBalancedElytra;
        //? endif
    }

    public static int balancedElytraBlocksPerDurability() {
        //? if fabric
        return FabricBalanceConfig.balancedElytraBlocksPerDurability;
        //? else
        return Config.balancedElytraBlocksPerDurability;
        //? endif
    }

    public static boolean nerfedMendingEnabled() {
        //? if fabric
        return FabricBalanceConfig.enableNerfedMending;
        //? else
        return Config.enableNerfedMending;
        //? endif
    }

    public static int nerfedMendingMultiplier() {
        //? if fabric
        return FabricBalanceConfig.nerfedMendingMultiplier;
        //? else
        return Config.nerfedMendingMultiplier;
        //? endif
    }

    public static boolean sourceDependentIFramesEnabled() {
        //? if fabric
        return FabricBalanceConfig.enableSourceDependentIFrames;
        //? else
        return Config.enableSourceDependentIFrames;
        //? endif
    }

    public static boolean sourceIFrameBlacklisted(String damageTypeId) {
        if (damageTypeId == null) {
            return false;
        }

        //? if fabric
        return FabricBalanceConfig.sourceIFrameBlacklist != null
            && FabricBalanceConfig.sourceIFrameBlacklist.contains(damageTypeId);
        //? else
        if (Config.sourceIFrameBlacklist == null) {
            return false;
        }
        for (Object entry : Config.sourceIFrameBlacklist) {
            if (damageTypeId.equals(String.valueOf(entry))) {
                return true;
            }
        }
        return false;
        //? endif
    }

    public static boolean nerfedDiscountsEnabled() {
        //? if fabric
        return FabricBalanceConfig.enableNerfedDiscounts;
        //? else
        return Config.enableNerfedDiscounts;
        //? endif
    }

    public static boolean removeZombieCureDiscountsEnabled() {
        //? if fabric
        return FabricBalanceConfig.removeZombieCureDiscounts;
        //? else
        return Config.removeZombieCureDiscounts;
        //? endif
    }

    public static double maxVillagerDiscount() {
        //? if fabric
        return FabricBalanceConfig.maxVillagerDiscount;
        //? else
        return Config.maxVillagerDiscount;
        //? endif
    }
}
