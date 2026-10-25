package org.onenonly.bitsandbalance.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

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
        return bitsandbalance$isLoaded(CREATE_MOD_ID)
                || bitsandbalance$isLoaded(CREATE_FLY_HYPHEN_MOD_ID)
                || bitsandbalance$isLoaded(CREATE_FLY_MOD_ID)
                || bitsandbalance$isLoaded(CREATE_FLY_LEGACY_MOD_ID);
    }

    private static boolean bitsandbalance$isLoaded(String modId) {
        try {
            return ModList.get().isLoaded(modId);
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