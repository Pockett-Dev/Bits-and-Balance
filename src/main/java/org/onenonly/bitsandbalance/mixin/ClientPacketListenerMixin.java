package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import org.onenonly.bitsandbalance.Config;
/**
 * Disables the Elder Guardian appearance ("jumpscare") when Mining Fatigue is inflicted,
 * by canceling the corresponding client game event when enabled in config.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    private static final SoundEvent rebalance$oldLevel30Sound = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("bitsandbalance", "xp_old")
    );

    @Inject(method = "handleGameEvent(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V", at = @At("HEAD"), cancellable = true)
    private void rebalance$disableElderGuardian(ClientboundGameEventPacket packet, CallbackInfo ci) {
        if (!Config.disableGuardianJumpscare) return;
        try {
            if (packet.getEvent() == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
                if (Config.playGuardianJumpscareSound) {
                    Minecraft mc = Minecraft.getInstance();
                    LocalPlayer player = mc.player;
                    if (player != null) {
                        player.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.0F, 1.0F);
                    }
                }
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Safe fallback: do not cancel if any mapping differences arise
        }
    }

    private static int rebalance$lastSeenLevel = -1;

    @Inject(method = "handleSetExperience(Lnet/minecraft/network/protocol/game/ClientboundSetExperiencePacket;)V", at = @At("TAIL"))
    private void rebalance$level30OldSound(ClientboundSetExperiencePacket packet, CallbackInfo ci) {
        if (!Config.enableLevel30OldSound) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        int current = player.experienceLevel;
        if (rebalance$lastSeenLevel == -1) {
            rebalance$lastSeenLevel = current;
            return;
        }
        if (rebalance$lastSeenLevel < 30 && current >= 30) {
            try {
                player.playSound(rebalance$oldLevel30Sound, 1.0F, 1.0F);
            } catch (Throwable ignored) {
            }
        }
        rebalance$lastSeenLevel = current;
    }
}
