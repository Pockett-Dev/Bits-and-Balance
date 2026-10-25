package org.onenonly.bitsandbalance.mixin;

import com.mojang.datafixers.util.Pair;
import java.util.Map;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BoatRenderer.class)
public interface BoatRendererAccessor {

    @Accessor("boatResources")
    Map<Boat.Type, Pair<ResourceLocation, ListModel<Boat>>> bitsandbalance$getBoatResources();

    @Mutable
    @Accessor("boatResources")
    void bitsandbalance$setBoatResources(Map<Boat.Type, Pair<ResourceLocation, ListModel<Boat>>> boatResources);
}
