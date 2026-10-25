package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabItemBakedModel;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererEnhancedSlabItemMixin {
    private static final Logger BITSANDBALANCE$SPECIAL_ITEM_LOGGER = LoggerFactory.getLogger("bitsandbalance-special-item-debug");
    private static final Set<String> BITSANDBALANCE$LOGGED_MODEL_KEYS = ConcurrentHashMap.newKeySet();

    @Shadow
    @Final
    private ItemModelShaper itemModelShaper;

    private static boolean bitsandbalance$isTrackedSpecialItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.SHIELD) {
            return true;
        }

        if (!(item instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();
        return block instanceof AbstractBannerBlock
                || block instanceof BedBlock
                || block instanceof ShulkerBoxBlock;
    }

    private static String bitsandbalance$getTrackedBlockType(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock().getClass().getSimpleName();
        }

        return "-";
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void bitsandbalance$trackCurrentAlphaRenderedItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            BakedModel model,
            CallbackInfo ci
    ) {
        if (!ItemShareClient.isRenderingWithAlpha()) {
            return;
        }

        ItemShareClient.beginTrackedItemRender(stack);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void bitsandbalance$clearCurrentAlphaRenderedItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            BakedModel model,
            CallbackInfo ci
    ) {
        if (!ItemShareClient.isRenderingWithAlpha()) {
            return;
        }

        ItemShareClient.endTrackedItemRender();
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void bitsandbalance$logTrackedSpecialItemRender(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            BakedModel model,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        if (!bitsandbalance$isTrackedSpecialItem(stack)) {
            return;
        }

        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + displayContext;
        if (!BITSANDBALANCE$LOGGED_MODEL_KEYS.add(key)) {
            return;
        }

        BITSANDBALANCE$SPECIAL_ITEM_LOGGER.info(
                "Tracked item render: item={} context={} leftHand={} model={} customRenderer={} gui3d={} usesBlockLight={} foil={} blockType={}",
                BuiltInRegistries.ITEM.getKey(stack.getItem()),
                displayContext,
                leftHand,
                model == null ? "null" : model.getClass().getName(),
                model != null && model.isCustomRenderer(),
                model != null && model.isGui3d(),
                model != null && model.usesBlockLight(),
                stack.hasFoil(),
                bitsandbalance$getTrackedBlockType(stack)
        );
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getRenderType(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/client/renderer/RenderType;"
            ),
            require = 0
    )
    private RenderType bitsandbalance$forceTranslucentRenderTypeForChatBlockItems(ItemStack stack, boolean renderCutout) {
        if (ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F && stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (!(block instanceof AbstractBannerBlock) && !(block instanceof BedBlock) && !(block instanceof ShulkerBoxBlock)) {
                return Sheets.translucentItemSheet();
            }
        }

        return ItemBlockRenderTypes.getRenderType(stack, renderCutout);
    }

    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true, require = 0)
    private void bitsandbalance$useEnhancedSlabFallbackModel(
            ItemStack stack,
            Level level,
            LivingEntity livingEntity,
            int seed,
            CallbackInfoReturnable<BakedModel> cir
    ) {
        ModelType modelType = EnhancedSlabItemSupport.modelTypeForStack(stack);
        if (modelType == ModelType.NONE) {
            return;
        }

        BakedModel currentModel = cir.getReturnValue();
        if (currentModel instanceof EnhancedSlabItemBakedModel) {
            return;
        }

        Block sourceBlock = SlabFamilyModelSourceSelector.modelSourceBlockForItem(stack.getItem());
        if (sourceBlock == null) {
            return;
        }

        Item sourceItem = sourceBlock.asItem();
        if (sourceItem == null || sourceItem == Items.AIR) {
            return;
        }

        BakedModel missingModel = this.itemModelShaper.getModelManager().getMissingModel();
        BakedModel sourceModel = this.itemModelShaper.getItemModel(sourceItem);
        if (sourceModel == null || sourceModel == missingModel) {
            sourceModel = currentModel;
        }
        if (sourceModel == null || sourceModel == this.itemModelShaper.getModelManager().getMissingModel()) {
            return;
        }

        cir.setReturnValue(EnhancedSlabItemBakedModel.wrap(sourceModel, modelType));
    }
}
