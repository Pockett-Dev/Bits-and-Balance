package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerDreamMotionMixin {
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$keepBodyKnockbackOffGhost(ClientboundSetEntityMotionPacket packet,
                                                           CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread() || minecraft.player == null) {
            return;
        }
        //? if mc == 1.21.1 || mc == 1.21.11
        int entityId = packet.getId();
        //? else
        int entityId = packet.id();
        //? endif
        if (minecraft.player.getId() == entityId && DreamProjectionClient.ignoreBodyMotion(minecraft.player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void bitsandbalance$acceptDreamReturn(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread() && minecraft.player != null) {
            DreamProjectionClient.beginReturn(minecraft.player);
        }
    }
}
