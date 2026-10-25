package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.onenonly.bitsandbalance.fabric.client.FabricChatMentions;
import org.onenonly.bitsandbalance.fabric.client.FabricChatHeadsRenderer;
import org.onenonly.bitsandbalance.fabric.compat.chatpatches.FabricChatPatchesCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;

@Mixin(ChatListener.class)
public class ChatListenerMixin {
    private static final ThreadLocal<PlayerInfo> CURRENT_SENDER = new ThreadLocal<>();

    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureSender(PlayerChatMessage playerChatMessage, GameProfile gameProfile, ChatType.Bound bound, CallbackInfo ci) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            ClientPacketListener connection = minecraft.getConnection();
            if (connection == null) {
                return;
            }

            PlayerInfo sender = connection.getPlayerInfo(playerChatMessage.sender());
            CURRENT_SENDER.set(sender);
            FabricChatPatchesCompat.captureSender(sender);
        } catch (Throwable ignored) {
        }
    }

    @ModifyArg(
        method = "showMessageToPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            ordinal = 0
        ),
        index = 0,
        require = 0
    )
    private Component bitsandbalance$processPlayerChatMentions(Component message) {
        Component processed = FabricChatMentions.processChatMessage(message, CURRENT_SENDER.get());
        if (processed == null) {
            processed = message;
        }

        FabricChatHeadsRenderer.rememberChatHeadOwner(processed, CURRENT_SENDER.get(), false);
        return processed;
    }

    @Inject(method = "showMessageToPlayer", at = @At("RETURN"), require = 0)
    private void bitsandbalance$clearSender(ChatType.Bound bound, PlayerChatMessage playerChatMessage, Component message, GameProfile gameProfile, boolean onlyShowSecureChat, Instant timestamp, CallbackInfoReturnable<?> cir) {
        CURRENT_SENDER.remove();
        FabricChatPatchesCompat.clearSender();
    }

    @Inject(method = "handleSystemMessage", at = @At("HEAD"), require = 0)
    private void bitsandbalance$rememberSystemMessageHead(Component message, boolean overlay, CallbackInfo ci) {
        if (message == null || !FabricChatHeadsRenderer.isAdvancementMessage(message.getString())) {
            return;
        }

        FabricChatHeadsRenderer.rememberChatHeadOwner(message, CURRENT_SENDER.get(), false);
    }
}
