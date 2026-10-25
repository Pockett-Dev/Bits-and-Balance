package org.onenonly.bitsandbalance.mixin;

import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BitsAndBalanceMixinPlugin implements IMixinConfigPlugin {

    private static boolean bitsandbalance$classExists(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }


    private static boolean bitsandbalance$isSableCompatMixin(String mixinClassName) {
        return mixinClassName.contains(".mixin.sable.");
    }

    private static boolean bitsandbalance$hasSableAssembly() {
        try {
            boolean knownSableStack = ModList.get().isLoaded("sable")
                    || ModList.get().isLoaded("simulated")
                    || ModList.get().isLoaded("aeronautics")
                    || ModList.get().isLoaded("aeronautics_bundled");
            return knownSableStack && bitsandbalance$classExists("dev.ryanhcode.sable.api.SubLevelAssemblyHelper");
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
        if (bitsandbalance$isSableCompatMixin(mixinClassName)) {
            return bitsandbalance$hasSableAssembly();
        }
        if (mixinClassName.contains(".mixin.create.")) {
            try {
                if (!ModList.get().isLoaded("create")) return false;
                return bitsandbalance$classExists("com.simibubi.create.Create");
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
