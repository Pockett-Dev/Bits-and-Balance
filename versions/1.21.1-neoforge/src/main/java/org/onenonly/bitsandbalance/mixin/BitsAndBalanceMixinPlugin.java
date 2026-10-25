package org.onenonly.bitsandbalance.mixin;

import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BitsAndBalanceMixinPlugin implements IMixinConfigPlugin {
    private static final String LAMB_DYNAMIC_LIGHTS_CLASS_RESOURCE =
            "dev/lambdaurora/lambdynlights/LambDynLights.class";

    private static boolean bitsandbalance$hasLambDynamicLights() {
        try {
            if (ModList.get().isLoaded("lambdynlights")) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        // NeoForge can evaluate this config before ModList exposes nested mods.
        // Lamb's class resource is already visible at that point and is the same
        // early-safe signal used by the shared mixin config.
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return bitsandbalance$hasLambDynamicLightsResource(contextLoader)
                || bitsandbalance$hasLambDynamicLightsResource(BitsAndBalanceMixinPlugin.class.getClassLoader());
    }

    private static boolean bitsandbalance$hasLambDynamicLightsResource(ClassLoader classLoader) {
        return classLoader != null && classLoader.getResource(LAMB_DYNAMIC_LIGHTS_CLASS_RESOURCE) != null;
    }

    private static boolean bitsandbalance$hasEmbeddium() {
        try {
            return ModList.get().isLoaded("embeddium");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean bitsandbalance$hasSodium() {
        try {
            return ModList.get().isLoaded("sodium");
        } catch (Throwable ignored) {
            return false;
        }
    }


    private static boolean bitsandbalance$isLambDynamicLightsVanillaCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("BioluminescenceEntityRendererMixin");
    }

    private static boolean bitsandbalance$isLambDynamicLightsCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("LambDynamicLightsBioluminescenceCompatMixin");
    }

    private static boolean bitsandbalance$isEmbeddiumBioluminescenceCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("EmbeddiumBioluminescenceLightPipelineMixin");
    }

    private static boolean bitsandbalance$isSodiumBioluminescenceCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("SodiumBioluminescenceLightPipelineMixin");
    }

    private static boolean bitsandbalance$isSableAssemblyCompatMixin(String mixinClassName) {
        return mixinClassName.contains(".mixin.sable.");
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
        if (bitsandbalance$isEmbeddiumBioluminescenceCompatMixin(mixinClassName)) {
            return bitsandbalance$hasEmbeddium();
        }
        if (bitsandbalance$isSodiumBioluminescenceCompatMixin(mixinClassName)) {
            return bitsandbalance$hasSodium();
        }
        if (bitsandbalance$isSableAssemblyCompatMixin(mixinClassName)) {
            // NeoForge can ask this during mixin preparation before Sable is reliably visible to ModList.
            // The Sable mixins are @Pseudo, so absent targets are handled by Mixin itself.
            return true;
        }
        if (mixinClassName.contains(".mixin.create.")) {
            // NeoForge asks this during mixin preparation before Create is reliably visible to ModList.
            // The Create mixins are @Pseudo, so absent targets are handled by Mixin itself.
            return true;
        }
        if (mixinClassName.endsWith("ControllingNeoForgePlatformHelperMixin")) {
            try {
                return ModList.get().isLoaded("controlling");
            } catch (Throwable ignored) {
                return false;
            }
        }
        if (mixinClassName.endsWith("EnchancementElytraDurabilityMixin")) {
            try {
                return ModList.get().isLoaded("enchancement");
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
