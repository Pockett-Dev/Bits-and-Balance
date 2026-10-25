package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HangingSignRenderer.class)
public abstract class HangingSignRendererPlankSplitMixin {
	private static final String BB_PLANK_WOOD_LEFT = "bitsandbalance$plankWoodLeft";
	private static final String BB_PLANK_WOOD_MID = "bitsandbalance$plankWoodMid";
	private static final String BB_PLANK_WOOD_RIGHT = "bitsandbalance$plankWoodRight";
	private static final String BB_PLANK_CONNECTOR_LEFT = "bitsandbalance$plankConnectorLeft";
	private static final String BB_PLANK_CONNECTOR_RIGHT = "bitsandbalance$plankConnectorRight";

	@Redirect(
			method = "createHangingSignLayer(Lnet/minecraft/client/renderer/blockentity/HangingSignRenderer$AttachmentType;)Lnet/minecraft/client/model/geom/builders/LayerDefinition;",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/model/geom/builders/PartDefinition;addOrReplaceChild(Ljava/lang/String;Lnet/minecraft/client/model/geom/builders/CubeListBuilder;Lnet/minecraft/client/model/geom/PartPose;)Lnet/minecraft/client/model/geom/builders/PartDefinition;"
			)
	)
	private static PartDefinition bitsandbalance$splitPlank(
			PartDefinition parent,
			String name,
			CubeListBuilder cubes,
			PartPose pose
	) {
		if (!"plank".equals(name)) {
			return parent.addOrReplaceChild(name, cubes, pose);
		}

		// Replace the single plank cube with 5 sub-parts so we can tint only the wood columns.
		// This mirrors the GUI logic that avoids tinting connector columns at x=2-3 and x=12-13 (in a 16px-wide plank).
		PartDefinition plank = parent.addOrReplaceChild("plank", CubeListBuilder.create(), pose);

		// Original plank (vanilla): texOffs(0,0) addBox(-8, -6, -2, 16, 2, 4)
		// Split into: [0-1] wood, [2-3] connector, [4-11] wood, [12-13] connector, [14-15] wood.
		plank.addOrReplaceChild(
				BB_PLANK_WOOD_LEFT,
				CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -6.0f, -2.0f, 2.0f, 2.0f, 4.0f),
				PartPose.ZERO
		);
		plank.addOrReplaceChild(
				BB_PLANK_CONNECTOR_LEFT,
				CubeListBuilder.create().texOffs(2, 0).addBox(-6.0f, -6.0f, -2.0f, 2.0f, 2.0f, 4.0f),
				PartPose.ZERO
		);
		plank.addOrReplaceChild(
				BB_PLANK_WOOD_MID,
				CubeListBuilder.create().texOffs(4, 0).addBox(-4.0f, -6.0f, -2.0f, 8.0f, 2.0f, 4.0f),
				PartPose.ZERO
		);
		plank.addOrReplaceChild(
				BB_PLANK_CONNECTOR_RIGHT,
				CubeListBuilder.create().texOffs(12, 0).addBox(4.0f, -6.0f, -2.0f, 2.0f, 2.0f, 4.0f),
				PartPose.ZERO
		);
		plank.addOrReplaceChild(
				BB_PLANK_WOOD_RIGHT,
				CubeListBuilder.create().texOffs(14, 0).addBox(6.0f, -6.0f, -2.0f, 2.0f, 2.0f, 4.0f),
				PartPose.ZERO
		);

		return plank;
	}
}
