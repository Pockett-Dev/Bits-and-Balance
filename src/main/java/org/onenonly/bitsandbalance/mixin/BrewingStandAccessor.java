package org.onenonly.bitsandbalance.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandAccessor {
    @Accessor("brewTime")
    int getBrewTime();

    @Accessor("brewTime")
    void setBrewTime(int value);

    @Accessor("fuel")
    int getFuel();

    @Accessor("fuel")
    void setFuel(int value);
}
