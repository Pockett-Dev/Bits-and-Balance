package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.systems.RenderSystem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.block.Blocks;

/**
 * Client-side handler for rendering shared items inline with chat messages.
 */
public final class ItemShareClient {
    private ItemShareClient() {
    }

    public static float alphaValue = 1.0F;

    /**
     * In 1.21.10+ the chat fade is not always encoded into the drawString ARGB color; it may be
     * provided separately as an opacity argument higher up the call stack.
     *
     * We capture that per-line opacity in the ChatComponent mixin and multiply it into the
     * effective alpha used for inline item rendering.
     */
    private static final ThreadLocal<Float> bitsandbalance$chatLineAlphaTL = ThreadLocal.withInitial(() -> 1.0F);

    private static volatile Method bitsandbalance$getShaderColorMethod;

    private static volatile boolean bitsandbalance$guiGraphicsAlphaResolved = false;
    private static volatile Method bitsandbalance$guiGraphicsGetColorMethod;
    private static volatile Field bitsandbalance$guiGraphicsColorField;

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

    public static void renderItemForMessage(GuiGraphics guiGraphics, FormattedCharSequence sequence, float x, float y, int color) {
        Minecraft mc = Minecraft.getInstance();
        StringBuilder before = new StringBuilder();
        int halfSpace = mc.font.width(" ") / 2;

        sequence.accept((counter_, style, character) -> {
            String sofar = before.toString();

            HoverEvent hoverEvent = style.getHoverEvent();

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
            return true;
        });
    }

    private static void renderItemInline(Minecraft mc, GuiGraphics guiGraphics, String before,
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

        HoverEvent hoverEvent = style.getHoverEvent();

        if (hoverEvent instanceof HoverEvent.ShowItem showItem) {
            ItemStack stack = showItem.item();

            if (stack.isEmpty()) {
                stack = new ItemStack(Blocks.BARRIER);
            }

            float shift = mc.font.width(before) + extraShift + 4.0F;

            boolean isBlockItem = stack.getItem() instanceof BlockItem;
            boolean isBoatItem = stack.getItem() instanceof BoatItem;
            boolean isPotionItem = stack.getItem() instanceof PotionItem;
            boolean hasGlint = stack.hasFoil();

            if (isBlockItem || isBoatItem || isPotionItem || hasGlint) {
                beginAlphaRender(alpha);

                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(shift + x, y);
                guiGraphics.pose().scale(0.5f, 0.5f);
                guiGraphics.renderItem(stack, 0, 0);
                guiGraphics.pose().popMatrix();

                endAlphaRender();
                return;
            }

            TextureAtlasSprite sprite = bitsandbalance$getParticleIconSprite(mc, stack);
            if (sprite != null) {
                int argb = ARGB.color(alpha, -1);
                int drawX = (int) (shift + x);
                int drawY = (int) y;
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, drawX, drawY, 8, 8, argb);
            }
        }
    }

    private static TextureAtlasSprite bitsandbalance$getParticleIconSprite(Minecraft mc, ItemStack stack) {
        ItemModelResolver resolver = mc.getItemModelResolver();
        if (resolver == null) return null;

        ItemStackRenderState renderState = new ItemStackRenderState();
        resolver.updateForTopItem(renderState, stack, ItemDisplayContext.GUI, mc.level, null, 0);
        if (renderState.isEmpty()) return null;
        return renderState.pickParticleIcon(RandomSource.create());
    }

    public static boolean isRenderingWithAlpha() {
        return isRenderingWithAlpha;
    }

    private static float bitsandbalance$getGuiGraphicsAlpha(GuiGraphics guiGraphics) {
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
