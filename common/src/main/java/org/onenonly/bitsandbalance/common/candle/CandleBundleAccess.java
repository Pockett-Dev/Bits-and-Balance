package org.onenonly.bitsandbalance.common.candle;

import net.minecraft.server.level.ServerLevel;

public final class CandleBundleAccess {
    private CandleBundleAccess() {
    }

    public static CandleBundleSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CandleBundleSavedData.TYPE);
    }
}
