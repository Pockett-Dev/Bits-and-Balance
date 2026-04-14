package org.onenonly.bitsandbalance.mixin;

import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BitsAndBalanceMixinPlugin implements IMixinConfigPlugin {

    private static boolean bitsandbalance$hasLambDynamicLights() {
        try {
            return ModList.get().isLoaded("lambdynlights");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean bitsandbalance$isLambDynamicLightsVanillaCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("BioluminescenceBrightnessGetterMixin")
                || mixinClassName.endsWith("BioluminescenceModelBlockRendererMixin")
                || mixinClassName.endsWith("BioluminescenceEntityRendererMixin");
    }

    private static boolean bitsandbalance$isLambDynamicLightsCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("LambDynamicLightsBioluminescenceCompatMixin");
    }

    private static boolean bitsandbalance$classExists(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (bitsandbalance$isLambDynamicLightsVanillaCompatMixin(mixinClassName)) {
            return !bitsandbalance$hasLambDynamicLights();
        }
        if (bitsandbalance$isLambDynamicLightsCompatMixin(mixinClassName)) {
            return bitsandbalance$hasLambDynamicLights();
        }
        if (mixinClassName.contains(".mixin.create.")) {
            try {
                if (!ModList.get().isLoaded("create")) return false;
                return bitsandbalance$classExists("com.simibubi.create.Create");
            } catch (Throwable ignored) {
                return false;
            }
        }
        if (mixinClassName.endsWith("GoldToolsHaveFortuneTooltipCompatMixin")) {
            try {
                return ModList.get().isLoaded("quark")
                        && bitsandbalance$classExists("org.violetmoon.quark.content.tweaks.module.GoldToolsHaveFortuneModule");
            } catch (Throwable ignored) {
                return false;
            }
        }
        if (mixinClassName.endsWith("ControllingNeoForgePlatformHelperMixin")) {
            try {
                return ModList.get().isLoaded("controlling");
            } catch (Throwable ignored) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
