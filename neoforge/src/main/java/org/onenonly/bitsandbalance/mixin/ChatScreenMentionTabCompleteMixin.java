package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.ChatMentionInputHighlight;
import org.onenonly.bitsandbalance.common.client.ChatMentionTabCompletion;
import org.onenonly.bitsandbalance.common.client.ChatMentionSuggestionOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMentionTabCompleteMixin {
    @Shadow
    protected EditBox input;

    @Unique
    private boolean bitsandbalance$mentionFormatterInstalled;

    @Inject(method = "extractRenderState", at = @At("HEAD"), require = 0)
    private void bitsandbalance$installMentionFormatter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.bitsandbalance$mentionFormatterInstalled || this.input == null) {
            return;
        }

        this.input.addFormatter(this::bitsandbalance$formatMentionInput);
        this.bitsandbalance$mentionFormatterInstalled = true;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$completeMention(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableChatMentions || !Config.mentionTabCompleteUsernames) {
            return;
        }
        if (keyEvent.key() != GLFW.GLFW_KEY_TAB || this.input == null) {
            return;
        }

        String currentValue = this.input.getValue();
        if (currentValue == null || currentValue.isBlank() || currentValue.startsWith("/")) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return;
        }

        ChatMentionTabCompletion.SuggestionState suggestions = bitsandbalance$resolveMentionSuggestions(connection, currentValue);
        ChatMentionTabCompletion.CompletionResult completion = ChatMentionTabCompletion.complete(currentValue, suggestions);
        if (completion == null) {
            return;
        }

        this.input.setValue(completion.updatedText());
        this.input.setCursorPosition(completion.cursorPosition());
        cir.setReturnValue(true);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void bitsandbalance$renderMentionSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.enableChatMentions || !Config.mentionTabCompleteUsernames || this.input == null) {
            return;
        }

        String currentValue = this.input.getValue();
        if (currentValue == null || currentValue.isBlank() || currentValue.startsWith("/")) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return;
        }

        ChatMentionTabCompletion.SuggestionState suggestions = bitsandbalance$resolveMentionSuggestions(connection, currentValue);
        if (suggestions == null) {
            return;
        }

        ChatMentionSuggestionOverlay.render(graphics, minecraft.font, this.input, suggestions);
    }

    @Unique
    private FormattedCharSequence bitsandbalance$formatMentionInput(String visibleText, int cursorPosition) {
        if (!Config.enableChatMentions || !Config.mentionInputHighlightUsernames || this.input == null) {
            return null;
        }

        String currentValue = this.input.getValue();
        if (currentValue == null || currentValue.isBlank() || currentValue.startsWith("/")) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return null;
        }

        return ChatMentionInputHighlight.format(
                this.input,
                visibleText,
                bitsandbalance$getOnlineNames(connection),
                bitsandbalance$isTriggerEnabled("AT_NAME"),
                bitsandbalance$isTriggerEnabled("PLAIN_NAME"),
                Config.mentionCaseInsensitive,
                Config.mentionWordBoundary,
                Config.mentionHighlightColorHex
        );
    }

    private ChatMentionTabCompletion.SuggestionState bitsandbalance$resolveMentionSuggestions(ClientPacketListener connection, String currentValue) {
        return ChatMentionTabCompletion.suggestions(currentValue, this.input.getCursorPosition(), bitsandbalance$getOnlineNames(connection));
    }

    @Unique
    private List<String> bitsandbalance$getOnlineNames(ClientPacketListener connection) {
        List<String> onlineNames = new ArrayList<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            if (info == null) {
                continue;
            }
            String name = info.getProfile().name();
            if (name != null && !name.isBlank()) {
                onlineNames.add(name);
            }
        }
        return onlineNames;
    }

    @Unique
    private boolean bitsandbalance$isTriggerEnabled(String triggerMode) {
        if (Config.mentionTriggerModes == null || Config.mentionTriggerModes.isEmpty()) {
            return true;
        }

        for (String configuredMode : Config.mentionTriggerModes) {
            if (configuredMode != null && configuredMode.equalsIgnoreCase(triggerMode)) {
                return true;
            }
        }
        return false;
    }
}