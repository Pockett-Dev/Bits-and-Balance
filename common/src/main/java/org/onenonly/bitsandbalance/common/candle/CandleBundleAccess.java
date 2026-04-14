package org.onenonly.bitsandbalance.common.candle;

import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Method;

public final class CandleBundleAccess {
    private CandleBundleAccess() {
    }

    public static CandleBundleSavedData get(ServerLevel level) {
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
}
