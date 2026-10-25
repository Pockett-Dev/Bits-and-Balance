package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import java.lang.reflect.Method;

/**
 * Shared wood types.
 *
 * Note: On newer versions with official Mojang mappings, the registration methods may not be accessible.
 * We use reflection to register the custom types when possible, but keep the custom instances even
 * when registration is unavailable so azalea signs remain distinguishable from oak signs on the client.
 */
public final class ModWoodTypes {
    public static final BlockSetType AZALEA_BLOCK_SET;
    public static final WoodType AZALEA_WOOD_TYPE;

    static {
        String id = BitsAndBalanceCommon.MOD_ID + ":azalea";

        BlockSetType blockSet = new BlockSetType(id);
        WoodType woodType = new WoodType(id, blockSet);

        try {
            Method register = findStaticRegister(BlockSetType.class, BlockSetType.class);
            if (register != null) {
                register.setAccessible(true);
                Object result = register.invoke(null, blockSet);
                if (result instanceof BlockSetType typed) {
                    blockSet = typed;
                }
            }
        } catch (Throwable ignored) {
            // Keep the unregistered custom type.
        }

        try {
            woodType = new WoodType(id, blockSet);
            Method register = findStaticRegister(WoodType.class, WoodType.class);
            if (register != null) {
                register.setAccessible(true);
                Object result = register.invoke(null, woodType);
                if (result instanceof WoodType typed) {
                    woodType = typed;
                }
            }
        } catch (Throwable ignored) {
            // Keep the unregistered custom type.
        }

        AZALEA_BLOCK_SET = blockSet;
        AZALEA_WOOD_TYPE = woodType;
    }

    private ModWoodTypes() {
    }

    public static void register() {
        // No-op.
    }

    private static Method findStaticRegister(Class<?> owner, Class<?> paramType) {
        for (Method method : owner.getDeclaredMethods()) {
            if (!method.getName().equals("register")) continue;
            if ((method.getModifiers() & java.lang.reflect.Modifier.STATIC) == 0) continue;
            if (method.getParameterCount() != 1) continue;
            if (method.getParameterTypes()[0] != paramType) continue;
            if (method.getReturnType() != owner) continue;
            return method;
        }
        return null;
    }
}
