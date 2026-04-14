package org.onenonly.bitsandbalance.fabric.compat;

import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class FabricDragonFightCompat {
    private static volatile Class<?> bitsandbalance$serverLevelClass;
    private static volatile Method bitsandbalance$getDragonFightMethod;
    private static volatile Field bitsandbalance$dragonFightField;

    private FabricDragonFightCompat() {
    }

    public static Object getDragonFight(ServerLevel serverLevel) {
        if (serverLevel == null) {
            return null;
        }

        try {
            bitsandbalance$resolveServerLevelAccess(serverLevel.getClass());

            Method method = bitsandbalance$getDragonFightMethod;
            if (method != null) {
                return method.invoke(serverLevel);
            }

            Field field = bitsandbalance$dragonFightField;
            if (field != null) {
                return field.get(serverLevel);
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static boolean hasPreviouslyKilledDragon(ServerLevel serverLevel) {
        return hasPreviouslyKilledDragon(getDragonFight(serverLevel));
    }

    public static boolean hasPreviouslyKilledDragon(Object dragonFight) {
        if (dragonFight == null) {
            return false;
        }

        try {
            Method method = dragonFight.getClass().getMethod("hasPreviouslyKilledDragon");
            method.setAccessible(true);
            Object result = method.invoke(dragonFight);
            return result instanceof Boolean hasPreviouslyKilledDragon && hasPreviouslyKilledDragon;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void bitsandbalance$resolveServerLevelAccess(Class<?> serverLevelClass) {
        if (serverLevelClass == bitsandbalance$serverLevelClass) {
            return;
        }

        synchronized (FabricDragonFightCompat.class) {
            if (serverLevelClass == bitsandbalance$serverLevelClass) {
                return;
            }

            bitsandbalance$getDragonFightMethod = bitsandbalance$findDragonFightGetter(serverLevelClass);
            bitsandbalance$dragonFightField = bitsandbalance$findDragonFightField(serverLevelClass);
            bitsandbalance$serverLevelClass = serverLevelClass;
        }
    }

    private static Method bitsandbalance$findDragonFightGetter(Class<?> serverLevelClass) {
        for (Method method : serverLevelClass.getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (!method.getName().equals("getDragonFight") && !method.getName().equals("dragonFight")) {
                continue;
            }

            method.setAccessible(true);
            return method;
        }

        for (Method method : serverLevelClass.getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }

            String returnTypeName = method.getReturnType().getSimpleName();
            if (!returnTypeName.contains("DragonFight")) {
                continue;
            }

            method.setAccessible(true);
            return method;
        }

        return null;
    }

    private static Field bitsandbalance$findDragonFightField(Class<?> serverLevelClass) {
        for (Field field : serverLevelClass.getDeclaredFields()) {
            String fieldTypeName = field.getType().getSimpleName();
            if (!fieldTypeName.contains("DragonFight") && !field.getName().toLowerCase().contains("dragonfight")) {
                continue;
            }

            field.setAccessible(true);
            return field;
        }

        return null;
    }
}