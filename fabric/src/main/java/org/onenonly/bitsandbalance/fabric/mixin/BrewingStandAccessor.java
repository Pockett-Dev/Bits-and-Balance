package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandAccessor {
    @Accessor("brewTime")
    int bitsandbalance$getBrewTime();

    @Accessor("brewTime")
    void bitsandbalance$setBrewTime(int value);

    @Accessor("fuel")
    int bitsandbalance$getFuel();

    @Accessor("fuel")
    void bitsandbalance$setFuel(int value);
}
