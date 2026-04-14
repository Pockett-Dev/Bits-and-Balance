package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client tweaks: disable elder guardian "jumpscare" and play the old level-30 sound.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    private static final SoundEvent bitsandbalance$oldLevel30Sound = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("bitsandbalance", "xp_old")
    );

    @Inject(method = "handleGameEvent(Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;)V", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$disableElderGuardian(ClientboundGameEventPacket packet, CallbackInfo ci) {
        if (!FabricClientConfig.disableGuardianJumpscare) return;
        try {
            if (packet.getEvent() == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
                if (FabricClientConfig.playGuardianJumpscareSound) {
                    Minecraft mc = Minecraft.getInstance();
                    LocalPlayer player = mc.player;
                    if (player != null) {
                        player.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.0F, 1.0F);
                    }
                }
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    private static int bitsandbalance$lastSeenLevel = -1;

    @Inject(method = "handleSetExperience(Lnet/minecraft/network/protocol/game/ClientboundSetExperiencePacket;)V", at = @At("TAIL"))
    private void bitsandbalance$level30OldSound(ClientboundSetExperiencePacket packet, CallbackInfo ci) {
        if (!FabricClientConfig.enableLevel30OldSound) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        int current = player.experienceLevel;
        if (bitsandbalance$lastSeenLevel == -1) {
            bitsandbalance$lastSeenLevel = current;
            return;
        }

        if (bitsandbalance$lastSeenLevel < 30 && current >= 30) {
            try {
                player.playSound(bitsandbalance$oldLevel30Sound, 1.0F, 1.0F);
            } catch (Throwable ignored) {
            }
        }

        bitsandbalance$lastSeenLevel = current;
    }
}
