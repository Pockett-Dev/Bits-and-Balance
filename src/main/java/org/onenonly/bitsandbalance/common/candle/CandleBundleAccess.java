package org.onenonly.bitsandbalance.common.candle;

import net.minecraft.server.level.ServerLevel;

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import java.lang.reflect.Method;
//? endif

public final class CandleBundleAccess {
    private CandleBundleAccess() {
    }

    public static CandleBundleSavedData get(ServerLevel level) {
        //? if mc == 1.21.1
        return level.getDataStorage().computeIfAbsent(CandleBundleSavedData.FACTORY, CandleBundleSavedData.DATA_NAME);
        //? else
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        return getReflectively(level);
        //? else
        return level.getDataStorage().computeIfAbsent(CandleBundleSavedData.TYPE);
        //? endif
        //? endif
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static CandleBundleSavedData getReflectively(ServerLevel level) {
        try {
            Method getDataStorage = level.getClass().getMethod("getDataStorage");
            Object storage = getDataStorage.invoke(level);
            if (storage == null) {
                throw new IllegalStateException("ServerLevel data storage is unavailable");
            }

            Method computeIfAbsent = storage.getClass().getMethod(
                    "computeIfAbsent",
                    CandleBundleSavedData.TYPE.getClass()
            );
            Object savedData = computeIfAbsent.invoke(storage, CandleBundleSavedData.TYPE);
            if (savedData instanceof CandleBundleSavedData typedSavedData) {
                return typedSavedData;
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access candle bundle saved data", exception);
        }

        throw new IllegalStateException("Candle bundle saved data lookup returned an unexpected type");
    }

    //? endif
}
