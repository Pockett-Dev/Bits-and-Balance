package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingClickCountAccessor {
    @Accessor("clickCount")
    int bitsandbalance$getClickCount();

    @Accessor("clickCount")
    void bitsandbalance$setClickCount(int value);
}
