package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.color.item.GrassColorSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
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

@Mixin(GrassColorSource.class)
public class GrassColorSourceMixin {
	@Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
	private void bitsandbalance$biomeItemTint(ItemStack stack, ClientLevel level, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
		if (!FabricClientConfig.biomeItemTintEnabled) {
			return;
		}
		if (level == null || entity == null) {
			// Some render paths call ItemTintSource#calculate with null context.
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
		String path = id.getPath();
		boolean isGrassLike = path.equals("grass_block")
				|| path.equals("short_grass")
				|| path.equals("tall_grass")
				|| path.equals("fern")
				|| path.equals("large_fern")
				|| path.equals("potted_fern")
				|| path.equals("sugar_cane");
		if (!isGrassLike) {
			return;
		}

		int color = bitsandbalance$getAverageGrassColor(level, entity.blockPosition());
		cir.setReturnValue(ARGB.opaque(color));
	}

	private static int bitsandbalance$getAverageGrassColor(ClientLevel level, BlockPos pos) {
		try {
			for (java.lang.reflect.Method method : BiomeColors.class.getDeclaredMethods()) {
				if (!method.getName().equals("getAverageGrassColor") || method.getParameterCount() != 2) {
					continue;
				}
				return ((Integer) method.invoke(null, level, pos)).intValue();
			}
		} catch (ReflectiveOperationException ignored) {
		}
		return 0;
	}
}
