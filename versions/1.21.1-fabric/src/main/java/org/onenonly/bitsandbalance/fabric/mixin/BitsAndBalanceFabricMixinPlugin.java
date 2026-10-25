package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BitsAndBalanceFabricMixinPlugin implements IMixinConfigPlugin {

    private static boolean bitsandbalance$hasLambDynamicLights() {
        return FabricLoader.getInstance().isModLoaded("lambdynlights");
    }

    private static boolean bitsandbalance$hasSodium() {
        return FabricLoader.getInstance().isModLoaded("sodium");
    }


    private static boolean bitsandbalance$isLambDynamicLightsVanillaCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("BioluminescenceModelBlockRendererMixin")
                || mixinClassName.endsWith("BioluminescenceEntityRendererMixin")
                || mixinClassName.endsWith("IndigoAoCalculatorBioluminescenceMixin");
    }

    private static boolean bitsandbalance$isLambDynamicLightsCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("LambDynamicLightsBioluminescenceCompatMixin");
    }

    private static boolean bitsandbalance$isSodiumBioluminescenceCompatMixin(String mixinClassName) {
        return mixinClassName.endsWith("SodiumBioluminescenceLightPipelineMixin");
    }

    private static boolean bitsandbalance$hasCreateCompatClasses() {
        return bitsandbalance$classExists("com.simibubi.create.Create")
                || bitsandbalance$classExists("com.zurrtum.create.Create");
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
        if (bitsandbalance$isSodiumBioluminescenceCompatMixin(mixinClassName)) {
            return bitsandbalance$hasSodium();
        }
        if (mixinClassName.contains(".mixin.create.")) {
            try {
                if (!FabricLoader.getInstance().isModLoaded("create")) return false;
                return bitsandbalance$hasCreateCompatClasses();
            } catch (Throwable ignored) {
                return false;
            }
        }
        if (mixinClassName.contains(".mixin.litematica.")) {
            return FabricLoader.getInstance().isModLoaded("litematica");
        }
        if (mixinClassName.endsWith("ControllingNewKeyBindsScreenMixin")
                || mixinClassName.endsWith("ControllingFabricPlatformHelperMixin")) {
            return FabricLoader.getInstance().isModLoaded("controlling");
        }
        if (mixinClassName.endsWith("AxiomServerboundSetBlockStepsMixin")) {
            return FabricLoader.getInstance().isModLoaded("axiom");
        }
        if (mixinClassName.endsWith("EnchancementElytraDurabilityMixin")) {
            return FabricLoader.getInstance().isModLoaded("enchancement");
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
