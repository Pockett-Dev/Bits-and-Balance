package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.fabric.compat.chatpatches.FabricChatPatchesCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Restores Bits and Balance chat heads after Chat Patches rebuilds a message.
 *
 * <p>Chat Patches uses mixin priority 500 for its timestamp rewrite. Applying
 * this mixin at priority 600 makes the priority-500 HEAD hook run first at
 * runtime, so this hook sees the final timestamped component without needing
 * to understand the user's configured timestamp format.</p>
 */
@Mixin(value = ChatComponent.class, priority = 600)
public class ChatComponentChatPatchesCompatMixin {
    @ModifyVariable(
            //? if mc == 1.21.1 || mc == 1.21.11
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            //? else
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            //? endif
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private Component bitsandbalance$restoreChatHeadAfterChatPatches(Component message) {
        if (!FabricLoader.getInstance().isModLoaded("chatpatches")) {
            return message;
        }

        return FabricChatPatchesCompat.restoreChatHead(message);
    }
}
