package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

import java.lang.reflect.Method;

/**
 * Shared wood types.
 *
 * Note: On 1.21.10 with official Mojang mappings, the registration methods may not be accessible.
 * We use reflection to register the custom types when possible, otherwise we fall back to OAK.
 */
public final class ModWoodTypes {
    public static final BlockSetType AZALEA_BLOCK_SET;
    public static final WoodType AZALEA_WOOD_TYPE;

    static {
        String id = BitsAndBalanceCommon.MOD_ID + ":azalea";

        BlockSetType blockSet = BlockSetType.OAK;
        WoodType woodType = WoodType.OAK;

        try {
            BlockSetType candidate = new BlockSetType(id);
            Method register = findStaticRegister(BlockSetType.class, BlockSetType.class);
            if (register != null) {
                register.setAccessible(true);
                Object result = register.invoke(null, candidate);
                if (result instanceof BlockSetType typed) {
                    blockSet = typed;
                }
            }
        } catch (Throwable ignored) {
            // Fall back to OAK
        }

        try {
            WoodType candidate = new WoodType(id, blockSet);
            Method register = findStaticRegister(WoodType.class, WoodType.class);
            if (register != null) {
                register.setAccessible(true);
                Object result = register.invoke(null, candidate);
                if (result instanceof WoodType typed) {
                    woodType = typed;
                }
            }
        } catch (Throwable ignored) {
            // Fall back to OAK
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
