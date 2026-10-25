package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.datafixers.util.Pair;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.mixin.BoatRendererAccessor;

public class AzaleaBoatRenderer extends BoatRenderer {
    private static final ResourceLocation AZALEA_BOAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BitsAndBalanceCommon.MOD_ID,
        "textures/entity/boat/azalea.png"
    );
    private static final ResourceLocation AZALEA_CHEST_BOAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BitsAndBalanceCommon.MOD_ID,
        "textures/entity/chest_boat/azalea.png"
    );

    private final ResourceLocation texture;

    public AzaleaBoatRenderer(EntityRendererProvider.Context context, boolean hasChest) {
        super(context, hasChest);
        this.texture = hasChest ? AZALEA_CHEST_BOAT_TEXTURE : AZALEA_BOAT_TEXTURE;

        Map<Boat.Type, Pair<ResourceLocation, ListModel<Boat>>> azaleaResources = new EnumMap<>(Boat.Type.class);
        ((BoatRendererAccessor) this).bitsandbalance$getBoatResources().forEach((type, resource) ->
            azaleaResources.put(type, Pair.of(this.texture, resource.getSecond()))
        );
        ((BoatRendererAccessor) this).bitsandbalance$setBoatResources(azaleaResources);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat boat) {
        return this.texture;
    }
}
