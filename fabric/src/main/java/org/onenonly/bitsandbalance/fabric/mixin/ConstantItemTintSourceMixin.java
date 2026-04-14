package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Constant.class)
public abstract class ConstantItemTintSourceMixin {
	private static final float LEAF_LITTER_ITEM_LIGHTEN_FACTOR = 1.03F;

	@Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
	private void bitsandbalance$biomeFoliageItemTint(ItemStack stack, ClientLevel level, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
		if (!FabricClientConfig.biomeItemTintEnabled) {
			return;
		}

		Constant self = (Constant) (Object) this;
		if (self.value() != -2) {
			return;
		}
		if (level == null || entity == null) {
			Minecraft mc = Minecraft.getInstance();
			if (level == null) {
				level = mc.level;
			}
			if (entity == null) {
				entity = mc.player;
			}
			if (level == null || entity == null) {
				return;
			}
		}
		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return;
		}

		Identifier id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
		if (id == null) {
			return;
		}
		if (FabricClientConfig.biomeItemTintDisabledItemIds.contains(id)) {
			return;
		}
		if (!id.getNamespace().equals("minecraft")) {
			return;
		}
		if (!bitsandbalance$isBiomeFoliageItem(id.getPath())) {
			return;
		}

		int color = BiomeColors.getAverageFoliageColor(level, entity.blockPosition());
		if (id.getPath().equals("leaf_litter")) {
			color = bitsandbalance$scaleRgb(color, LEAF_LITTER_ITEM_LIGHTEN_FACTOR);
		}
		cir.setReturnValue(ARGB.opaque(color));
	}

	private static boolean bitsandbalance$isBiomeFoliageItem(String path) {
		return path.endsWith("_leaves")
				|| path.equals("vine")
				|| path.equals("lily_pad")
				|| path.equals("leaf_litter");
	}

	private static int bitsandbalance$scaleRgb(int color, float factor) {
		int r = Math.max(0, Math.min(255, Math.round(((color >> 16) & 0xFF) * factor)));
		int g = Math.max(0, Math.min(255, Math.round(((color >> 8) & 0xFF) * factor)));
		int b = Math.max(0, Math.min(255, Math.round((color & 0xFF) * factor)));
		return (r << 16) | (g << 8) | b;
	}
}