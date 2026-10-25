package org.onenonly.bitsandbalance.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Loader-neutral decisions for mixins declared by bitsandbalance.shared.mixins.json.
 *
 * <p>The Fabric and NeoForge mixin plugins cannot control mixins from this
 * separate config. In particular, LambDynamicLights must own
 * EntityRenderer#getBlockLightLevel when it is installed because its hook also
 * supplies lighting for other entities and the first-person hand.</p>
 */
public final class BitsAndBalanceSharedMixinPlugin implements IMixinConfigPlugin {
    private static final String LAMB_DYNAMIC_LIGHTS_CLASS_RESOURCE =
            "dev/lambdaurora/lambdynlights/LambDynLights.class";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("BioluminescenceEntityRendererMixin")) {
            return !isLambDynamicLightsPresent();
        }
        if (mixinClassName.endsWith("LambDynamicLightsEntityRendererMixin")) {
            return isLambDynamicLightsPresent();
        }
        return true;
    }

    private static boolean isLambDynamicLightsPresent() {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (hasClassResource(contextLoader)) {
            return true;
        }
        return hasClassResource(BitsAndBalanceSharedMixinPlugin.class.getClassLoader());
    }

    private static boolean hasClassResource(ClassLoader classLoader) {
        return classLoader != null
                && classLoader.getResource(LAMB_DYNAMIC_LIGHTS_CLASS_RESOURCE) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
