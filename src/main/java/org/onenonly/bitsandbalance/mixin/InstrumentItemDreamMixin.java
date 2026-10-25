package org.onenonly.bitsandbalance.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
//? if mc == 1.21.1
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.Items;
//? else
import net.minecraft.world.InteractionResult;
//? endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.onenonly.bitsandbalance.common.mechanics.GoatHornCooldowns;
import org.onenonly.bitsandbalance.common.mechanics.GoatHornPurposes;
import org.onenonly.bitsandbalance.common.mechanics.PonderLightAudit;
import org.onenonly.bitsandbalance.common.mechanics.YearnEcho;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if mc == 1.21.1
import org.spongepowered.asm.mixin.injection.Redirect;
//? endif
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InstrumentItem.class)
public abstract class InstrumentItemDreamMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$checkIndividualHornCooldown(Level level, Player player, InteractionHand hand,
            //? if mc == 1.21.1
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
            //? else
            CallbackInfoReturnable<InteractionResult> cir) {
            //? endif
        String horn = GoatHornPurposes.horn(player.getItemInHand(hand));
        boolean customCooldown = GoatHornPurposes.separateCooldowns
                || "dream".equals(horn) && GoatHornPurposes.isEnabled("dream");
        if (customCooldown && !horn.isEmpty() && GoatHornCooldowns.remainingTicks(player, horn) > 0) {
            //? if mc == 1.21.1
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
            //? else
            cir.setReturnValue(InteractionResult.FAIL);
            //? endif
        }
    }

    //? if mc == 1.21.1
    @Redirect(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"))
    private void bitsandbalance$skipSharedGoatHornCooldown(ItemCooldowns cooldowns, Item item, int ticks,
                                                            Level level, Player player, InteractionHand hand) {
        String horn = GoatHornPurposes.horn(player.getItemInHand(hand));
        boolean customCooldown = GoatHornPurposes.separateCooldowns
                || "dream".equals(horn) && GoatHornPurposes.isEnabled("dream");
        if (!customCooldown || item != Items.GOAT_HORN || horn.isEmpty()) {
            cooldowns.addCooldown(item, ticks);
        }
    }
    //? endif

    @Inject(method = "use", at = @At("RETURN"))
    private void bitsandbalance$projectDream(Level level, Player player, InteractionHand hand,
                                               CallbackInfoReturnable<?> cir) {
        if (player.isUsingItem() && player.getUsedItemHand() == hand) {
            ItemStack stack = player.getItemInHand(hand);
            String horn = GoatHornPurposes.horn(stack);
            if (horn.isEmpty()) {
                return;
            }
            boolean dreamPowerEnabled = "dream".equals(horn) && GoatHornPurposes.isEnabled("dream");
            int cooldownTicks = dreamPowerEnabled ? DreamProjection.COOLDOWN_TICKS
                    : ((InstrumentItem) (Object) this).getUseDuration(stack, player);
            if (player instanceof ServerPlayer serverPlayer) {
                if ("dream".equals(horn)) {
                    if (!DreamProjection.start(serverPlayer)) {
                        int remaining = DreamProjection.cooldownRemainingTicks(serverPlayer);
                        if (remaining > 0) {
                            //? if mc == 1.21.1 || mc == 1.21.11
                            serverPlayer.displayClientMessage(Component.translatable(
                                    "message.bitsandbalance.dream_horn_cooldown", (remaining + 19) / 20), true);
                            //? else
                            serverPlayer.sendSystemMessage(Component.translatable(
                                    "message.bitsandbalance.dream_horn_cooldown", (remaining + 19) / 20), true);
                            //? endif
                        }
                    }
                } else if ("yearn".equals(horn)) {
                    YearnEcho.start(serverPlayer);
                } else if ("ponder".equals(horn)) {
                    PonderLightAudit.start(serverPlayer);
                }
            }
            if (GoatHornPurposes.separateCooldowns) {
                GoatHornCooldowns.begin(player, horn, cooldownTicks);
            }
            //? if mc != 1.21.1
            if (dreamPowerEnabled) {
                player.getCooldowns().addCooldown(stack, cooldownTicks);
            }
            //? endif
        }
    }
}
