package org.onenonly.bitsandbalance.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class CreateCompat {
    private static final String CREATE_MOD_ID = "create";
    private static final String CREATE_FLY_HYPHEN_MOD_ID = "create-fly";
    private static final String CREATE_FLY_MOD_ID = "create_fly";
    private static final String CREATE_FLY_LEGACY_MOD_ID = "createfly";
    private static final Identifier RAW_ZINC_ID = Identifier.parse("create:raw_zinc");
    private static final Identifier ZINC_ORE_ID = Identifier.parse("create:zinc_ore");
    private static final Identifier DEEPSLATE_ZINC_ORE_ID = Identifier.parse("create:deepslate_zinc_ore");

    private CreateCompat() {
    }

    public static boolean isZincContentAvailable() {
        return bitsandbalance$hasItem(RAW_ZINC_ID)
                || bitsandbalance$hasBlock(ZINC_ORE_ID)
                || bitsandbalance$hasBlock(DEEPSLATE_ZINC_ORE_ID);
    }

    public static boolean isCreateFamilyLoaded() {
        return bitsandbalance$isModLoaded(CREATE_MOD_ID)
                || bitsandbalance$isModLoaded(CREATE_FLY_HYPHEN_MOD_ID)
                || bitsandbalance$isModLoaded(CREATE_FLY_MOD_ID)
                || bitsandbalance$isModLoaded(CREATE_FLY_LEGACY_MOD_ID);
    }

    private static boolean bitsandbalance$isModLoaded(String modId) {
        return bitsandbalance$isFabricModLoaded(modId) || bitsandbalance$isNeoForgeModLoaded(modId);
    }

    private static boolean bitsandbalance$isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (Boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean bitsandbalance$isNeoForgeModLoaded(String modId) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            return (Boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean bitsandbalance$hasItem(Identifier id) {
        try {
            return BuiltInRegistries.ITEM.get(id).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean bitsandbalance$hasBlock(Identifier id) {
        try {
            return BuiltInRegistries.BLOCK.get(id).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }
}