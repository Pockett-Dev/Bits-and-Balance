package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BitsAndBalanceFabricMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-mixin-plugin");
    private static boolean bitsandbalance$loggedSodiumMixinDecision;

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

    private static String bitsandbalance$sodiumVersion() {
        return FabricLoader.getInstance()
                .getModContainer("sodium")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    private static boolean bitsandbalance$hasSupportedSodiumRenderer() {
        if (!FabricLoader.getInstance().isModLoaded("sodium")) {
            return false;
        }
        String version = bitsandbalance$sodiumVersion();
        if (!bitsandbalance$isSupportedSodiumVersion(version)) {
            return false;
        }
        return bitsandbalance$classResourceExists("net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer")
                && bitsandbalance$classResourceExists("net.caffeinemc.mods.sodium.fabric.model.FabricModelAccess")
                && bitsandbalance$classResourceExists("net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext");
    }

    private static boolean bitsandbalance$hasSupportedSodiumFaceCulling() {
        if (!bitsandbalance$hasSodium()) {
            return false;
        }
        String version = bitsandbalance$sodiumVersion();
        return bitsandbalance$isSupportedSodiumVersion(version)
                && bitsandbalance$classResourceExists("net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext");
    }

    private static boolean bitsandbalance$isSupportedSodiumVersion(String version) {
        return (version.startsWith("0.9.") && version.contains("+mc26.3"))
                || version.startsWith("mc26.3-0.9.")
                || (version.startsWith("mc26.3r") && version.contains("-0.9."));
    }

    private static boolean bitsandbalance$classResourceExists(String className) {
        String resourceName = className.replace('.', '/') + ".class";
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null && contextLoader.getResource(resourceName) != null) {
            return true;
        }

        ClassLoader pluginLoader = BitsAndBalanceFabricMixinPlugin.class.getClassLoader();
        return pluginLoader != null && pluginLoader.getResource(resourceName) != null;
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
            return bitsandbalance$hasSodium() && !bitsandbalance$hasLambDynamicLights();
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
        if (mixinClassName.endsWith("TippedArrowRecipeMixin")
                && targetClassName.equals("net.minecraft.world.item.crafting.ImbueRecipe")) {
            return true;
        }
        if (mixinClassName.endsWith("TippedArrowRecipeMixin")
                || mixinClassName.endsWith("TippedArrowResultMixin")) {
            return bitsandbalance$classExists(targetClassName);
        }
        if (mixinClassName.endsWith("ItemBlockRenderTypesMixin")) {
            return bitsandbalance$classExists("net.minecraft.client.renderer.item.BlockRenderTypes");
        }
        if (mixinClassName.endsWith("EnhancedSlabRenderOffsetMixin")) {
            return true;
        }
        if (mixinClassName.endsWith("EnhancedSlabBlockEntityRenderOffsetMixin")) {
            return true;
        }
        if (mixinClassName.endsWith("SodiumEnhancedSlabFaceCullingMixin")) {
            return bitsandbalance$hasSupportedSodiumFaceCulling();
        }
        if (mixinClassName.endsWith("SodiumEnhancedSlabTerrainRenderMixin")
                || mixinClassName.endsWith("SodiumFabricModelAccessEnhancedSlabMixin")
                || mixinClassName.endsWith("SodiumAbstractBlockRenderContextAccessorMixin")) {
            boolean apply = bitsandbalance$hasSupportedSodiumRenderer();
            if (!bitsandbalance$loggedSodiumMixinDecision) {
                bitsandbalance$loggedSodiumMixinDecision = true;
                LOGGER.info("Sodium enhanced slab terrain mixins apply={} version={}", apply, bitsandbalance$sodiumVersion());
            }
            return apply;
        }
        if (mixinClassName.endsWith("BellRendererEnhancedSlabOffsetMixin")) {
            return true;
        }
        if (mixinClassName.endsWith("BetterBlockEntitiesBellCompatMixin")) {
            return FabricLoader.getInstance().isModLoaded("betterblockentities");
        }
        if (mixinClassName.endsWith("ChatComponentItemShareAlphaScopeMixin")
            || mixinClassName.endsWith("ChatComponentItemShareBackgroundMixin")
            || mixinClassName.endsWith("ChatComponentItemShareFocusedMixin")
            || mixinClassName.endsWith("ChatComponentItemShareMixin")
            || mixinClassName.endsWith("GuiRendererItemShareRenderToAtlasAlphaMixin")) {
            return false;
        }
        if (mixinClassName.endsWith("SheetsMixin")) {
            return false;
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
