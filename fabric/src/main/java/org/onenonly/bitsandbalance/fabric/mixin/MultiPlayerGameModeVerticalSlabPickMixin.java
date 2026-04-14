package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.fabric.pickblock.FabricPickBlockCompat;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeVerticalSlabPickMixin {
	@Shadow @Final
	private Minecraft minecraft;

	@Inject(method = "handlePickItemFromBlock", at = @At("HEAD"), cancellable = true)
	private void bitsandbalance$handleEnhancedSlabCreativePick(net.minecraft.core.BlockPos pos, boolean includeData, CallbackInfo ci) {
		try {
			Minecraft mc = this.minecraft;
			if (mc == null || mc.level == null || mc.player == null) return;
			if (!(mc.hitResult instanceof BlockHitResult hit)) return;
			if (!pos.equals(hit.getBlockPos())) return;

			FabricPickBlockCompat.PickTarget pickTarget = FabricPickBlockCompat.resolve(mc.level, mc.player, hit);
			if (pickTarget == null || pickTarget.stack().isEmpty()) return;

			int heldSlot = ((InventoryAccessor) (Object) mc.player.getInventory()).bitsandbalance$getSelected();
			if (heldSlot < 0 || heldSlot >= 9) return;

			ItemStack picked = pickTarget.stack().copy();
			ClientPacketListener conn = mc.getConnection();
			if (mc.player.getAbilities().instabuild) {
				mc.player.getInventory().setItem(heldSlot, picked);
				if (conn != null) {
					conn.send(new ServerboundSetCreativeModeSlotPacket(36 + heldSlot, picked));
				}
				ci.cancel();
				return;
			}

			int foundSlot = bitsandbalance$findMatchingInventorySlot(picked);
			if (foundSlot < 0) return;

			if (foundSlot < 9) {
				if (foundSlot != heldSlot) {
					((InventoryAccessor) (Object) mc.player.getInventory()).bitsandbalance$setSelected(foundSlot);
					if (conn != null) {
						conn.send(new ServerboundSetCarriedItemPacket(foundSlot));
					}
				}
				ci.cancel();
				return;
			}

			if (bitsandbalance$swapInventorySlotIntoHotbar(foundSlot, heldSlot)) {
				ci.cancel();
			}
		} catch (Throwable ignored) {
		}
	}

	@ModifyVariable(
			method = "handlePickItemFromBlock",
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"
			),
			require = 0
	)
	private ItemStack bitsandbalance$pickTargetedEnhancedSlabPart(ItemStack original) {
		try {
			Minecraft mc = this.minecraft;
			if (mc == null || mc.level == null || mc.player == null) return original;
			if (!(mc.hitResult instanceof BlockHitResult hit)) return original;

			FabricPickBlockCompat.PickTarget pickTarget =
				FabricPickBlockCompat.resolve(mc.level, mc.player, hit);
			return pickTarget == null || pickTarget.stack().isEmpty() ? original : pickTarget.stack();
		} catch (Throwable ignored) {
			return original;
		}
	}

	private int bitsandbalance$findMatchingInventorySlot(ItemStack target) {
		Minecraft mc = this.minecraft;
		if (mc == null || mc.player == null || target.isEmpty()) return -1;

		for (int i = 0; i < 36; i++) {
			ItemStack candidate = mc.player.getInventory().getItem(i);
			if (candidate == null || candidate.isEmpty()) continue;
			if (ItemStack.isSameItemSameComponents(candidate, target)) return i;
		}
		return -1;
	}

	private boolean bitsandbalance$swapInventorySlotIntoHotbar(int inventorySlot, int hotbarSlot) {
		try {
			Minecraft mc = this.minecraft;
			if (mc == null || mc.player == null) return false;
			Object self = this;
			int containerId;
			try {
				containerId = mc.player.inventoryMenu.containerId;
			} catch (Throwable ignored) {
				containerId = 0;
			}

			try {
				this.getClass().getMethod(
						"handleInventoryMouseClick",
						int.class,
						int.class,
						int.class,
						ClickType.class,
						net.minecraft.world.entity.player.Player.class
				).invoke(self, containerId, inventorySlot, hotbarSlot, ClickType.SWAP, mc.player);
				return true;
			} catch (Throwable ignored) {
			}

			for (var method : this.getClass().getMethods()) {
				if (!method.getName().equals("handleInventoryMouseClick")) continue;
				Class<?>[] params = method.getParameterTypes();
				if (params.length != 5) continue;
				if (params[0] != int.class || params[1] != int.class || params[2] != int.class) continue;
				if (!params[3].getName().endsWith("ClickType")) continue;
				method.invoke(self, containerId, inventorySlot, hotbarSlot, ClickType.SWAP, mc.player);
				return true;
			}
		} catch (Throwable ignored) {
		}
		return false;
	}
}
