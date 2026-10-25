package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

public class AzaleaBoatRenderer extends AbstractBoatRenderer {
    private static final Identifier AZALEA_BOAT_TEXTURE =
        Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "textures/entity/boat/azalea.png");
    private static final Identifier AZALEA_CHEST_BOAT_TEXTURE =
        Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "textures/entity/chest_boat/azalea.png");

    private final Model.Simple waterPatchModel;
    private final Identifier texture;
    private final EntityModel<BoatRenderState> model;

    public AzaleaBoatRenderer(EntityRendererProvider.Context context, boolean hasChest) {
        super(context);
        this.texture = hasChest ? AZALEA_CHEST_BOAT_TEXTURE : AZALEA_BOAT_TEXTURE;
        this.waterPatchModel = new Model.Simple(context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), ignored -> RenderTypes.waterMask());
        this.model = new BoatModel(context.bakeLayer(hasChest ? ModelLayers.CHERRY_CHEST_BOAT : ModelLayers.CHERRY_BOAT));
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return this.model;
    }

    @Override
    protected RenderType renderType() {
        return this.model.renderType(this.texture);
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
        if (!renderState.isUnderWater) {
            collector.submitModel(
                this.waterPatchModel,
                Unit.INSTANCE,
                poseStack,
                this.waterPatchModel.renderType(this.texture),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                renderState.outlineColor,
                null
            );
        }
    }
}
