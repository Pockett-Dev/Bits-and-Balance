package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;

/**
 * Keeps the half-GUI-pixel chat-head baseline on the framebuffer pixel grid.
 */
public final class ChatHeadPixelAlignment {
    private static final float DESIRED_BASELINE_OFFSET_Y = 0.5F;

    private ChatHeadPixelAlignment() {
    }

    public static float baselineOffsetY() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null || minecraft.getWindow() == null) {
            return DESIRED_BASELINE_OFFSET_Y;
        }

        double guiScale = minecraft.getWindow().getGuiScale();
        double chatScale = minecraft.options.chatScale().get();
        double framebufferPixelsPerChatPixel = guiScale * chatScale;
        if (!Double.isFinite(framebufferPixelsPerChatPixel) || framebufferPixelsPerChatPixel <= 0.0D) {
            return DESIRED_BASELINE_OFFSET_Y;
        }

        return (float) (Math.round(DESIRED_BASELINE_OFFSET_Y * framebufferPixelsPerChatPixel)
                / framebufferPixelsPerChatPixel);
    }
}
