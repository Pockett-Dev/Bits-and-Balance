package org.onenonly.bitsandbalance.common.client;

import net.minecraft.resources.Identifier;

public interface BiomeItemTintSettings {
    boolean enabled();

    boolean autoDetect();

    boolean isAllowed(Identifier id);

    boolean isGrassFallback(Identifier id);

    boolean isFoliageFallback(Identifier id);

    boolean isDisabled(Identifier id);
}
