package org.onenonly.bitsandbalance.fabric.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;

/** Optional runtime bridge for Carpet's movable-block-entities rule. */
public final class FabricCarpetPistonCompat {
    private static final String CARPET_MOD_ID = "carpet";
    private static final String CARPET_SETTINGS_CLASS = "carpet.CarpetSettings";
    private static final String MOVABLE_BLOCK_ENTITIES_FIELD = "movableBlockEntities";

    private static volatile boolean fieldResolved;
    private static volatile Field movableBlockEntitiesField;

    private FabricCarpetPistonCompat() {
    }

    /**
     * Returns whether Carpet currently owns generic block-entity piston moves.
     * The field is read on every call because Carpet rules can change at runtime.
     */
    public static boolean ownsGenericBlockEntityMovement() {
        if (!FabricLoader.getInstance().isModLoaded(CARPET_MOD_ID)) {
            return false;
        }

        Field field = bitsandbalance$resolveMovableBlockEntitiesField();
        if (field == null) {
            return false;
        }
        try {
            return field.getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static Field bitsandbalance$resolveMovableBlockEntitiesField() {
        if (fieldResolved) {
            return movableBlockEntitiesField;
        }
        synchronized (FabricCarpetPistonCompat.class) {
            if (fieldResolved) {
                return movableBlockEntitiesField;
            }
            try {
                Class<?> settings = Class.forName(
                        CARPET_SETTINGS_CLASS,
                        false,
                        Thread.currentThread().getContextClassLoader()
                );
                movableBlockEntitiesField = settings.getField(MOVABLE_BLOCK_ENTITIES_FIELD);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                movableBlockEntitiesField = null;
            }
            fieldResolved = true;
            return movableBlockEntitiesField;
        }
    }
}
