package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 1.21.1 fallback inline chat item rendering.
 */
public final class ItemShareClient {
    private ItemShareClient() {
    }

    public static float alphaValue = 1.0F;
    private static final ThreadLocal<Float> CHAT_LINE_ALPHA = ThreadLocal.withInitial(() -> 1.0F);
    private static final ThreadLocal<String> CURRENT_RENDERED_ITEM_KEY = ThreadLocal.withInitial(() -> "");
    private static final ThreadLocal<Boolean> CURRENT_RENDERED_BLOCK_ITEM = ThreadLocal.withInitial(() -> false);
    private static volatile Method getShaderColorMethod;
    private static volatile boolean guiGraphicsAlphaResolved = false;
    private static volatile Method guiGraphicsGetColorMethod;
    private static volatile Field guiGraphicsColorField;
    private static boolean renderingWithAlpha = false;

    public static void beginChatLineAlpha(float alpha) {
        CHAT_LINE_ALPHA.set(alpha);
    }

    public static void endChatLineAlpha() {
        CHAT_LINE_ALPHA.set(1.0F);
    }

    public static void beginAlphaRender(float alpha) {
        alphaValue = alpha;
        renderingWithAlpha = true;
    }

    public static void endAlphaRender() {
        alphaValue = 1.0F;
        renderingWithAlpha = false;
    }

    public static void beginTrackedItemRender(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            CURRENT_RENDERED_ITEM_KEY.set("");
            CURRENT_RENDERED_BLOCK_ITEM.set(false);
            return;
        }

        CURRENT_RENDERED_ITEM_KEY.set(String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        CURRENT_RENDERED_BLOCK_ITEM.set(stack.getItem() instanceof BlockItem);
    }

    public static void endTrackedItemRender() {
        CURRENT_RENDERED_ITEM_KEY.set("");
        CURRENT_RENDERED_BLOCK_ITEM.set(false);
    }

    public static String getTrackedRenderedItemKey() {
        return CURRENT_RENDERED_ITEM_KEY.get();
    }

    public static boolean isTrackedRenderedBlockItem() {
        return CURRENT_RENDERED_BLOCK_ITEM.get();
    }

    public static boolean isTrackedSpecialBuiltInItem() {
        String itemKey = CURRENT_RENDERED_ITEM_KEY.get();
        return "minecraft:shield".equals(itemKey)
                || itemKey.endsWith("_banner")
                || itemKey.endsWith("_bed")
                || itemKey.endsWith("_shulker_box");
    }

    public static float getTrackedSpecialBuiltInItemAlpha() {
        if (!renderingWithAlpha || alphaValue >= 1.0F || !isTrackedSpecialBuiltInItem()) {
            return 1.0F;
        }

        return alphaValue;
    }

    public static float getEffectiveAlpha(GuiGraphics guiGraphics, int color) {
        float colorAlpha = (color >> 24 & 255) / 255.0F;
        float shaderAlpha = 1.0F;
        try {
            Method method = getShaderColorMethod;
            if (method == null) {
                method = RenderSystem.class.getDeclaredMethod("getShaderColor");
                method.setAccessible(true);
                getShaderColorMethod = method;
            }
            Object result = method.invoke(null);
            if (result instanceof float[] shaderColor && shaderColor.length >= 4) {
                shaderAlpha = shaderColor[3];
            }
        } catch (Throwable ignored) {
        }

        float guiAlpha = getGuiGraphicsAlpha(guiGraphics);
        return Math.max(0.0F, Math.min(1.0F, colorAlpha * CHAT_LINE_ALPHA.get() * shaderAlpha * guiAlpha));
    }

    public static void renderItemForMessage(GuiGraphics guiGraphics, FormattedCharSequence sequence, float x, float y, int color) {
        Minecraft mc = Minecraft.getInstance();
        StringBuilder before = new StringBuilder();
        int halfSpace = mc.font.width(" ") / 2;

        sequence.accept((counter, style, character) -> {
            HoverEvent hoverEvent = style.getHoverEvent();
            if (hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_ITEM) {
                int markerSpaces = markerSpaces(before);
                if (markerSpaces > 0) {
                    renderItemInline(mc, guiGraphics, before.substring(0, before.length() - markerSpaces), character == ' ' ? 0 : -halfSpace, x, y, style, color);
                    return false;
                }
            }
            before.append((char) character);
            return true;
        });
    }

    private static int markerSpaces(StringBuilder builder) {
        int length = builder.length();
        if (length >= 3
                && builder.charAt(length - 1) == ' '
                && builder.charAt(length - 2) == ' '
                && builder.charAt(length - 3) == ' ') {
            return 3;
        }
        if (length >= 2
                && builder.charAt(length - 1) == ' '
                && builder.charAt(length - 2) == ' ') {
            return 2;
        }
        return 0;
    }

    private static void renderItemInline(Minecraft mc, GuiGraphics guiGraphics, String before, float extraShift, float x, float y, Style style, int color) {
        HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_ITEM) {
            return;
        }

        HoverEvent.ItemStackInfo showItem = hoverEvent.getValue(HoverEvent.Action.SHOW_ITEM);
        if (showItem == null) {
            return;
        }

        ItemStack stack = showItem.getItemStack();
        if (stack.isEmpty()) {
            stack = new ItemStack(Blocks.BARRIER);
        }

        float alpha = getEffectiveAlpha(guiGraphics, color);
        float shift = mc.font.width(before) + extraShift + 4.0F;

        beginAlphaRender(alpha);
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.pose().translate(shift + x, y, 0.0F);
            guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
            guiGraphics.renderItem(stack, 0, 0);
        } finally {
            try {
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            } catch (Throwable ignored) {
            }
            try {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } catch (Throwable ignored) {
            }
            guiGraphics.pose().popPose();
            endAlphaRender();
        }
    }

    public static boolean isRenderingWithAlpha() {
        return renderingWithAlpha;
    }

    private static float getGuiGraphicsAlpha(GuiGraphics guiGraphics) {
        if (guiGraphics == null) {
            return 1.0F;
        }

        try {
            if (!guiGraphicsAlphaResolved) {
                guiGraphicsAlphaResolved = true;
                for (Method method : guiGraphics.getClass().getDeclaredMethods()) {
                    if (method.getParameterCount() != 0) {
                        continue;
                    }
                    Class<?> returnType = method.getReturnType();
                    String name = method.getName().toLowerCase();
                    if (!name.contains("color")) {
                        continue;
                    }
                    if (returnType == int.class || returnType == float[].class) {
                        method.setAccessible(true);
                        guiGraphicsGetColorMethod = method;
                        break;
                    }
                }
                if (guiGraphicsGetColorMethod == null) {
                    for (Field field : guiGraphics.getClass().getDeclaredFields()) {
                        Class<?> fieldType = field.getType();
                        String name = field.getName().toLowerCase();
                        if (!name.contains("color")) {
                            continue;
                        }
                        if (fieldType == int.class || fieldType == float[].class) {
                            field.setAccessible(true);
                            guiGraphicsColorField = field;
                            break;
                        }
                    }
                }
            }

            Method method = guiGraphicsGetColorMethod;
            if (method != null) {
                Object result = method.invoke(guiGraphics);
                if (result instanceof Integer argb) {
                    return ((argb >> 24) & 255) / 255.0F;
                }
                if (result instanceof float[] rgba && rgba.length >= 4) {
                    return rgba[3];
                }
            }

            Field field = guiGraphicsColorField;
            if (field != null) {
                Object result = field.get(guiGraphics);
                if (result instanceof Integer argb) {
                    return ((argb >> 24) & 255) / 255.0F;
                }
                if (result instanceof float[] rgba && rgba.length >= 4) {
                    return rgba[3];
                }
            }
        } catch (Throwable ignored) {
        }

        return 1.0F;
    }
}
