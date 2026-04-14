package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.systems.RenderSystem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Client-side handler for rendering shared items inline with chat messages.
 * 
 * This class renders item icons that fade perfectly in sync with chat text by:
 * 1. Detecting the two-space marker that indicates an item position
 * 2. Extracting the alpha value from the text color (same fade as text)
 * 3. Setting alphaValue which is used by ItemRendererMixin and ItemBlockRenderTypesMixin
 * 4. Rendering the item inline at the correct position
 */
public final class ItemShareClient {
    private static final float ITEM_SHARE_SCALE = 0.55F;
    private static final float ITEM_SHARE_X_OFFSET = 0.75F;
    private static final float ITEM_SHARE_Y_OFFSET = 1.0F;

    private ItemShareClient() {}

    /**
     * Global alpha value used by rendering mixins to fade items.
     * This is set during item rendering and used by:
     * - ItemRendererMixin: Applies alpha to all item vertices
     * - ItemBlockRenderTypesMixin: Ensures translucent render type when fading
     */
    public static float alphaValue = 1.0F;

    /**
     * In 1.21.10+ the chat fade is not always encoded into the drawString ARGB color; it may be
     * provided separately as an opacity argument higher up the call stack.
     *
     * We capture that per-line opacity in the ChatComponent mixins and multiply it into the
     * effective alpha used for inline item rendering.
     */
    private static final ThreadLocal<Float> bitsandbalance$chatLineAlphaTL = ThreadLocal.withInitial(() -> 1.0F);

    private static volatile Method bitsandbalance$getShaderColorMethod;

    private static volatile boolean bitsandbalance$guiGraphicsAlphaResolved = false;
    private static volatile Method bitsandbalance$guiGraphicsGetColorMethod;
    private static volatile Field bitsandbalance$guiGraphicsColorField;
    
    /**
     * Flag to track if we're currently rendering an item with alpha.
     * CRITICAL for block items - without this flag, ItemBlockRenderTypesMixin won't
     * force translucent render types, causing block items to stay fully opaque.
     */
    private static boolean isRenderingWithAlpha = false;

    public static void beginChatLineAlpha(float alpha) {
        bitsandbalance$chatLineAlphaTL.set(alpha);
    }

    public static void endChatLineAlpha() {
        bitsandbalance$chatLineAlphaTL.set(1.0F);
    }

    public static void beginAlphaRender(float alpha) {
        alphaValue = alpha;
        isRenderingWithAlpha = true;
    }

    public static void endAlphaRender() {
        alphaValue = 1.0F;
        isRenderingWithAlpha = false;
    }

    /**
     * Renders item icons inline with chat text.
     * Called by ChatComponentMixin before each line of text is drawn.
     * 
     * @param guiGraphics The graphics context for rendering
     * @param sequence The formatted character sequence (chat line)
     * @param x X position of the text
     * @param y Y position of the text
     * @param color The text color (contains alpha for fading)
     */
    public static void renderItemForMessage(GuiGraphicsExtractor guiGraphics, FormattedCharSequence sequence, float x, float y, int color) {
        Minecraft mc = Minecraft.getInstance();
        StringBuilder before = new StringBuilder();
        int halfSpace = mc.font.width(" ") / 2;

        // Iterate through characters to find the two-space marker
        sequence.accept((counter_, style, character) -> {
            String sofar = before.toString();

            HoverEvent hoverEvent = style.getHoverEvent();

            // Check for the marker spaces, but only treat it as a real marker if the *next*
            // character (the current callback) actually carries a ShowItem hover.
            //
            // Preferred format uses THREE spaces so the 3rd space becomes a visible gap between the icon and text.
            // We also keep the legacy TWO-space format for compatibility.
            if (hoverEvent instanceof HoverEvent.ShowItem) {
                if (sofar.endsWith("   ")) {
                    renderItemInline(mc, guiGraphics,
                            sofar.substring(0, sofar.length() - 3),
                            character == ' ' ? 0 : -halfSpace,
                            x, y, style, color);
                    return false;
                }
                if (sofar.endsWith("  ")) {
                    renderItemInline(mc, guiGraphics,
                            sofar.substring(0, sofar.length() - 2),
                            character == ' ' ? 0 : -halfSpace,
                            x, y, style, color);
                    return false;
                }
            }

            before.append((char) character);
            return true; // Continue iteration
        });
    }

    /**
     * Renders a single item icon inline with the text.
     * 
     * @param mc Minecraft instance
     * @param guiGraphics Graphics context
     * @param before Text before the item (used for positioning)
     * @param extraShift Additional horizontal shift adjustment
     * @param x Base X position
     * @param y Base Y position
     * @param style Text style (contains hover event with item data)
     * @param color Text color (alpha channel contains fade value)
     */
    private static void renderItemInline(Minecraft mc, GuiGraphicsExtractor guiGraphics, String before, 
                                        float extraShift, float x, float y, Style style, int color) {
        float colorAlpha = (color >> 24 & 255) / 255.0F;

        // Chat fading is sometimes applied as a separate render-state multiplier (e.g. shader color)
        // rather than being encoded into the ARGB alpha passed to drawString.
        float shaderAlpha = 1.0F;
        try {
            Method method = bitsandbalance$getShaderColorMethod;
            if (method == null) {
                method = RenderSystem.class.getDeclaredMethod("getShaderColor");
                method.setAccessible(true);
                bitsandbalance$getShaderColorMethod = method;
            }
            Object result = method.invoke(null);
            if (result instanceof float[] shaderColor && shaderColor.length >= 4) {
                shaderAlpha = shaderColor[3];
            }
        } catch (Throwable ignored) {
            // If the API changes or isn't available, fall back to 1.0.
        }

        float guiAlpha = bitsandbalance$getGuiGraphicsAlpha(guiGraphics);

        float alpha = colorAlpha * bitsandbalance$chatLineAlphaTL.get() * shaderAlpha * guiAlpha;
        if (alpha < 0.0F) alpha = 0.0F;
        if (alpha > 1.0F) alpha = 1.0F;

        // Check if this style has an item hover event
        HoverEvent hoverEvent = style.getHoverEvent();

        if (hoverEvent instanceof HoverEvent.ShowItem showItem) {
            ItemStack stack = showItem.item().create();

            // Use barrier block as fallback for invalid items
            if (stack.isEmpty()) {
                stack = new ItemStack(Blocks.BARRIER);
            }

            float shift = mc.font.width(before) + extraShift + 4.0F;
            float scaledX = (shift + x - ITEM_SHARE_X_OFFSET) / ITEM_SHARE_SCALE;
            float scaledY = (y - ITEM_SHARE_Y_OFFSET) / ITEM_SHARE_SCALE;

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(ITEM_SHARE_SCALE, ITEM_SHARE_SCALE);
            beginAlphaRender(alpha);
            try {
                guiGraphics.item(stack, Math.round(scaledX), Math.round(scaledY));
            } finally {
                endAlphaRender();
                guiGraphics.pose().popMatrix();
            }
        }
    }
    
    /**
     * Returns whether we're currently rendering an item with alpha.
     * Used by ItemBlockRenderTypesMixin to determine if it should force translucent render types.
     * 
     * @return true if currently rendering an item with alpha, false otherwise
     */
    public static boolean isRenderingWithAlpha() {
        return isRenderingWithAlpha;
    }

    private static float bitsandbalance$getGuiGraphicsAlpha(GuiGraphicsExtractor guiGraphics) {
        if (guiGraphics == null) return 1.0F;

        try {
            if (!bitsandbalance$guiGraphicsAlphaResolved) {
                bitsandbalance$guiGraphicsAlphaResolved = true;

                // Prefer a no-arg method that returns either int ARGB or float[4].
                for (Method m : guiGraphics.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    Class<?> rt = m.getReturnType();
                    String name = m.getName().toLowerCase();
                    if (!name.contains("color")) continue;
                    if (rt == int.class || rt == float[].class) {
                        m.setAccessible(true);
                        bitsandbalance$guiGraphicsGetColorMethod = m;
                        break;
                    }
                }

                // Fall back to a field that looks like the current color.
                if (bitsandbalance$guiGraphicsGetColorMethod == null) {
                    for (Field f : guiGraphics.getClass().getDeclaredFields()) {
                        Class<?> ft = f.getType();
                        String name = f.getName().toLowerCase();
                        if (!name.contains("color")) continue;
                        if (ft == int.class || ft == float[].class) {
                            f.setAccessible(true);
                            bitsandbalance$guiGraphicsColorField = f;
                            break;
                        }
                    }
                }
            }

            Method m = bitsandbalance$guiGraphicsGetColorMethod;
            if (m != null) {
                Object result = m.invoke(guiGraphics);
                if (result instanceof Integer argb) {
                    return ((argb >> 24) & 255) / 255.0F;
                }
                if (result instanceof float[] rgba && rgba.length >= 4) {
                    return rgba[3];
                }
            }

            Field f = bitsandbalance$guiGraphicsColorField;
            if (f != null) {
                Object result = f.get(guiGraphics);
                if (result instanceof Integer argb) {
                    return ((argb >> 24) & 255) / 255.0F;
                }
                if (result instanceof float[] rgba && rgba.length >= 4) {
                    return rgba[3];
                }
            }
        } catch (Throwable ignored) {
            // ignore
        }

        return 1.0F;
    }
}
