package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.fabricmc.loader.api.FabricLoader;

public final class EnhancedSlabRendererCompat {
    private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");
    private static final boolean INDIUM_LOADED = FabricLoader.getInstance().isModLoaded("indium");
    private static final boolean FABRIC_RENDERER_AVAILABLE = bitsandbalance$hasFabricRenderer();

    private EnhancedSlabRendererCompat() {
    }

    public static boolean isSodiumLoaded() {
        return SODIUM_LOADED;
    }

    public static boolean isIndiumLoaded() {
        return INDIUM_LOADED;
    }

    public static boolean hasFabricRenderer() {
        return FABRIC_RENDERER_AVAILABLE;
    }

    public static boolean preferChunkRenderPath() {
        return preferFabricMeshPath();
    }

    public static boolean preferVanillaTerrainPath() {
        return true;
    }

    public static boolean preferFabricMeshPath() {
        return FABRIC_RENDERER_AVAILABLE && (!SODIUM_LOADED || INDIUM_LOADED);
    }

    public static String chunkPathStatus() {
        if (preferFabricMeshPath()) {
            return "enabled (Fabric Renderer mesh path)";
        }
        if (SODIUM_LOADED && !INDIUM_LOADED) {
            return "disabled (Sodium detected without Indium)";
        }
        if (!FABRIC_RENDERER_AVAILABLE) {
            return "disabled (Fabric Renderer API implementation unavailable)";
        }
        return "disabled";
    }

    private static boolean bitsandbalance$hasFabricRenderer() {
        try {
            Class<?> accessClass = Class.forName("net.fabricmc.fabric.api.renderer.v1.RendererAccess");
            Object instance = accessClass.getField("INSTANCE").get(null);
            Object result = accessClass.getMethod("hasRenderer").invoke(instance);
            return result instanceof Boolean available && available;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
