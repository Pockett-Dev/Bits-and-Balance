package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabItemBakedModel;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererEnhancedSlabItemMixin {
    @Shadow
    @Final
    private ItemModelShaper itemModelShaper;

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

        Block familyBlock = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
        Block sourceSlab = familyBlock == null ? null : EnhancedSlabItemSupport.sourceSlabForBlock(familyBlock);
        boolean preserveSourceGeometry = SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(sourceSlab);
        cir.setReturnValue(EnhancedSlabItemBakedModel.wrap(sourceModel, modelType, preserveSourceGeometry));
    }
}
