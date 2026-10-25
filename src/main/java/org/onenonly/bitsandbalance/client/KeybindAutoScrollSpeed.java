package org.onenonly.bitsandbalance.client;

//? if mc == 1.21.1
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
//? else
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
//? endif
import net.minecraft.network.chat.Component;

public final class KeybindAutoScrollSpeed {
    private static final double VANILLA_SPEED_PERIOD_THRESHOLD = 4.0D;
    private static final double LONG_BIND_PERIOD_SCALE = 0.22D;
    private static final double END_HOLD_SECONDS = 1.0D;
    private static final double MIN_ONE_WAY_SCROLL_SECONDS = 0.8D;
    private static final ThreadLocal<Boolean> FAST_KEYBIND_SCROLL = ThreadLocal.withInitial(() -> false);

    private KeybindAutoScrollSpeed() {
    }

    public static void enterKeybindRender() {
        FAST_KEYBIND_SCROLL.set(true);
    }

    public static void exitKeybindRender() {
        FAST_KEYBIND_SCROLL.set(false);
    }

    public static double scrollPeriod(double overflowPeriod, double minimumPeriod) {
        double vanillaPeriod = Math.max(overflowPeriod, minimumPeriod);
        if (!FAST_KEYBIND_SCROLL.get() || vanillaPeriod <= VANILLA_SPEED_PERIOD_THRESHOLD) {
            return vanillaPeriod;
        }

        return VANILLA_SPEED_PERIOD_THRESHOLD + (vanillaPeriod - VANILLA_SPEED_PERIOD_THRESHOLD) * LONG_BIND_PERIOD_SCALE;
    }

    //? if mc == 1.21.1
    public static boolean renderScrollingString(GuiGraphics guiGraphics, Font font, Component message, int centerX, int left, int top, int right, int bottom, int color) {
        if (!FAST_KEYBIND_SCROLL.get()) {
            return false;
        }

        int textWidth = font.width(message);
        int textY = (top + bottom - 9) / 2 + 1;
        int availableWidth = right - left;
        if (textWidth <= availableWidth) {
            int textX = Math.max(left, Math.min(centerX - textWidth / 2, right - textWidth));
            guiGraphics.drawString(font, message, textX, textY, color);
            return true;
        }

        int overflowPixels = textWidth - availableWidth;
        double vanillaPeriod = Math.max(overflowPixels * 0.5D, 3.0D);
        if (vanillaPeriod <= VANILLA_SPEED_PERIOD_THRESHOLD) {
            return false;
        }

        double adjustedPeriod = scrollPeriod(overflowPixels * 0.5D, 3.0D);
        double oneWayScrollSeconds = Math.max(MIN_ONE_WAY_SCROLL_SECONDS, (adjustedPeriod - END_HOLD_SECONDS * 2.0D) / 2.0D);
        double cycleSeconds = END_HOLD_SECONDS * 2.0D + oneWayScrollSeconds * 2.0D;
        double cycleTime = (System.currentTimeMillis() / 1000.0D) % cycleSeconds;

        double scrollOffset;
        if (cycleTime < END_HOLD_SECONDS) {
            scrollOffset = 0.0D;
        } else if (cycleTime < END_HOLD_SECONDS + oneWayScrollSeconds) {
            scrollOffset = overflowPixels * ((cycleTime - END_HOLD_SECONDS) / oneWayScrollSeconds);
        } else if (cycleTime < END_HOLD_SECONDS + oneWayScrollSeconds + END_HOLD_SECONDS) {
            scrollOffset = overflowPixels;
        } else {
            scrollOffset = overflowPixels * (1.0D - ((cycleTime - END_HOLD_SECONDS - oneWayScrollSeconds - END_HOLD_SECONDS) / oneWayScrollSeconds));
        }

        guiGraphics.enableScissor(left, top, right, bottom);
        guiGraphics.drawString(font, message, left - (int) scrollOffset, textY, color);
        guiGraphics.disableScissor();
        return true;
    }
    //? else
    public static boolean acceptScrollingString(ActiveTextCollector output, Component message, int centerX, int left, int right, int top, int bottom, int lineWidth, int lineHeight, ActiveTextCollector.Parameters parameters) {
        if (!FAST_KEYBIND_SCROLL.get()) {
            return false;
        }

        int textY = (top + bottom - lineHeight) / 2 + 1;
        int availableWidth = right - left;
        if (lineWidth <= availableWidth) {
            return false;
        }

        int overflowPixels = lineWidth - availableWidth;
        double vanillaPeriod = Math.max(overflowPixels * 0.5D, 3.0D);
        if (vanillaPeriod <= VANILLA_SPEED_PERIOD_THRESHOLD) {
            return false;
        }

        double adjustedPeriod = scrollPeriod(overflowPixels * 0.5D, 3.0D);
        double oneWayScrollSeconds = Math.max(MIN_ONE_WAY_SCROLL_SECONDS, (adjustedPeriod - END_HOLD_SECONDS * 2.0D) / 2.0D);
        double cycleSeconds = END_HOLD_SECONDS * 2.0D + oneWayScrollSeconds * 2.0D;
        double cycleTime = (System.currentTimeMillis() / 1000.0D) % cycleSeconds;

        double scrollOffset;
        if (cycleTime < END_HOLD_SECONDS) {
            scrollOffset = 0.0D;
        } else if (cycleTime < END_HOLD_SECONDS + oneWayScrollSeconds) {
            scrollOffset = overflowPixels * ((cycleTime - END_HOLD_SECONDS) / oneWayScrollSeconds);
        } else if (cycleTime < END_HOLD_SECONDS + oneWayScrollSeconds + END_HOLD_SECONDS) {
            scrollOffset = overflowPixels;
        } else {
            scrollOffset = overflowPixels * (1.0D - ((cycleTime - END_HOLD_SECONDS - oneWayScrollSeconds - END_HOLD_SECONDS) / oneWayScrollSeconds));
        }

        output.accept(TextAlignment.LEFT, left - (int) scrollOffset, textY, parameters.withScissor(left, right, top, bottom), message.getVisualOrderText());
        return true;
    }
    //? endif
}
