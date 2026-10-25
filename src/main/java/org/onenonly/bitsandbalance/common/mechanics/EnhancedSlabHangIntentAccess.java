package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.server.level.ServerLevel;

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import java.lang.reflect.Method;
//? endif

public final class EnhancedSlabHangIntentAccess {
    private EnhancedSlabHangIntentAccess() {
    }

    public static EnhancedSlabHangIntentSavedData get(ServerLevel level) {
        //? if mc == 1.21.1
        return level.getDataStorage().computeIfAbsent(
                EnhancedSlabHangIntentSavedData.FACTORY,
                EnhancedSlabHangIntentSavedData.DATA_NAME
        );
        //? else
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        return getReflectively(level);
        //? else
        return level.getDataStorage().computeIfAbsent(EnhancedSlabHangIntentSavedData.TYPE);
        //? endif
        //? endif
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static EnhancedSlabHangIntentSavedData getReflectively(ServerLevel level) {
        try {
            Method getDataStorage = level.getClass().getMethod("getDataStorage");
            Object storage = getDataStorage.invoke(level);
            if (storage == null) {
                throw new IllegalStateException("ServerLevel data storage is unavailable");
            }

            Method computeIfAbsent = storage.getClass().getMethod(
                    "computeIfAbsent",
                    EnhancedSlabHangIntentSavedData.TYPE.getClass()
            );
            Object savedData = computeIfAbsent.invoke(storage, EnhancedSlabHangIntentSavedData.TYPE);
            if (savedData instanceof EnhancedSlabHangIntentSavedData typedSavedData) {
                return typedSavedData;
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access enhanced slab hang-intent saved data", exception);
        }

        throw new IllegalStateException("Enhanced slab hang-intent saved data lookup returned an unexpected type");
    }

    //? endif
}
