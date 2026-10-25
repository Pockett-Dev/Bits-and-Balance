package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.onenonly.bitsandbalance.common.client.ClientFeatureFlags;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererSoulFireFlamesMixin {
    @Unique
    private static final ThreadLocal<Entity> bitsandbalance$renderFlameEntity = new ThreadLocal<>();

    @Unique
    private static final Material bitsandbalance$SOUL_FIRE_0 = new Material(
	    TextureAtlas.LOCATION_BLOCKS,
	    ResourceLocation.parse("block/soul_fire_0")
    );

    @Unique
    private static final Material bitsandbalance$SOUL_FIRE_1 = new Material(
	    TextureAtlas.LOCATION_BLOCKS,
	    ResourceLocation.parse("block/soul_fire_1")
    );

    @Inject(
	    method = "renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;Lorg/joml/Quaternionf;)V",
	    at = @At("HEAD"),
	    require = 0
    )
    private void bitsandbalance$captureFlameEntity(PoseStack poseStack, MultiBufferSource multiBufferSource, Entity entity, Quaternionf cameraOrientation, CallbackInfo ci) {
	bitsandbalance$renderFlameEntity.set(entity);
    }

    @Inject(
	    method = "renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;Lorg/joml/Quaternionf;)V",
	    at = @At("RETURN"),
	    require = 0
    )
    private void bitsandbalance$clearFlameEntity(PoseStack poseStack, MultiBufferSource multiBufferSource, Entity entity, Quaternionf cameraOrientation, CallbackInfo ci) {
	bitsandbalance$renderFlameEntity.remove();
    }

    @Redirect(
	    method = "renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;Lorg/joml/Quaternionf;)V",
	    at = @At(
		    value = "FIELD",
		    target = "Lnet/minecraft/client/resources/model/ModelBakery;FIRE_0:Lnet/minecraft/client/resources/model/Material;"
	    ),
	    require = 0
    )
    private Material bitsandbalance$swapFire0Material() {
	return bitsandbalance$shouldUseSoulFireFlames() ? bitsandbalance$SOUL_FIRE_0 : ModelBakery.FIRE_0;
    }

    @Redirect(
	    method = "renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;Lorg/joml/Quaternionf;)V",
	    at = @At(
		    value = "FIELD",
		    target = "Lnet/minecraft/client/resources/model/ModelBakery;FIRE_1:Lnet/minecraft/client/resources/model/Material;"
	    ),
	    require = 0
    )
    private Material bitsandbalance$swapFire1Material() {
	return bitsandbalance$shouldUseSoulFireFlames() ? bitsandbalance$SOUL_FIRE_1 : ModelBakery.FIRE_1;
    }

    @Unique
    private static boolean bitsandbalance$shouldUseSoulFireFlames() {
	if (!ClientFeatureFlags.soulFireOverlayEnabled()) {
	    return false;
	}

	Entity entity = bitsandbalance$renderFlameEntity.get();
	return entity != null && SoulFireOverlayClient.shouldUseSoulFireFlamesNow(entity.level(), entity);
    }
}
