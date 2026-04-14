package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Client-side handler for rendering shared items inline with chat messages.
 */
public final class ItemShareClient {
    private static final float ITEM_SHARE_SCALE = 0.55F;
    private static final float ITEM_SHARE_X_OFFSET = 0.75F;
    private static final float ITEM_SHARE_Y_OFFSET = 1.0F;
    private static final int ITEM_SHARE_CACHE_SIZE = 64;
    private static final Pattern CHAT_HEAD_PREFIX_PATTERN = Pattern.compile("^\\[[^\\]]+ head\\]\\s*");

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
    private static volatile Method bitsandbalance$showItemItemMethod;
    private static volatile Method bitsandbalance$showItemValueCreateMethod;
    private static volatile Method bitsandbalance$showItemValueCopyMethod;
    private static volatile Method bitsandbalance$pickParticleMaterialMethod;
    private static volatile Method bitsandbalance$pickParticleIconMethod;
    private static volatile Method bitsandbalance$particleMaterialSpriteMethod;

    private static volatile boolean bitsandbalance$guiGraphicsAlphaResolved = false;
    private static volatile Method bitsandbalance$guiGraphicsGetColorMethod;
    private static volatile Field bitsandbalance$guiGraphicsColorField;

    private static boolean isRenderingWithAlpha = false;
    private static final Map<String, ItemStack> bitsandbalance$sharedMessageItems = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ItemStack> eldest) {
            return size() > ITEM_SHARE_CACHE_SIZE;
        }
    };

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

    public static void renderItemForMessage(GuiGraphicsExtractor guiGraphics, FormattedCharSequence sequence, float x, float y, int color) {
        Minecraft mc = Minecraft.getInstance();
        String fullText = bitsandbalance$extractText(sequence);
        ItemStack cachedStack = bitsandbalance$getCachedSharedItem(fullText);
        if (!cachedStack.isEmpty()) {
            if (bitsandbalance$renderCachedItemFromTextLayout(mc, guiGraphics, fullText, x, y, color, cachedStack)) {
                return;
            }
        }
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

    public static void rememberSharedItemMessage(Component message) {
        if (message == null) {
            return;
        }

        ItemStack stack = bitsandbalance$findSharedItemInComponent(message);
        if (stack.isEmpty()) {
            return;
        }

        String text = message.getString();
        if (text == null || text.isBlank()) {
            return;
        }

        String normalized = bitsandbalance$normalizeSharedMessageKey(text);
        String itemLabel = bitsandbalance$extractItemLabel(text);

        synchronized (bitsandbalance$sharedMessageItems) {
            bitsandbalance$sharedMessageItems.put(text, stack.copy());
            bitsandbalance$sharedMessageItems.put(normalized, stack.copy());
            if (!itemLabel.isBlank()) {
                bitsandbalance$sharedMessageItems.put(itemLabel, stack.copy());
            }
        }
    }

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

        HoverEvent hoverEvent = style.getHoverEvent();

        if (hoverEvent instanceof HoverEvent.ShowItem showItem) {
            ItemStack stack = bitsandbalance$resolveShowItemStack(showItem);

            renderResolvedItemInline(mc, guiGraphics, before, extraShift, x, y, color, stack);
        }
    }

    private static void renderCachedItemInline(Minecraft mc, GuiGraphicsExtractor guiGraphics, String before,
                                               float extraShift, float x, float y, int color, ItemStack stack) {
        renderResolvedItemInline(mc, guiGraphics, before, extraShift, x, y, color, stack.copy());
    }

    private static boolean bitsandbalance$renderCachedItemFromTextLayout(Minecraft mc, GuiGraphicsExtractor guiGraphics,
                                                                         String fullText, float x, float y, int color,
                                                                         ItemStack stack) {
        if (fullText == null || fullText.isBlank()) {
            return false;
        }

        int itemStart = fullText.lastIndexOf('[');
        if (itemStart < 0) {
            return false;
        }

        String beforeItem = fullText.substring(0, itemStart);
        int trailingSpaces = 0;
        for (int index = beforeItem.length() - 1; index >= 0 && beforeItem.charAt(index) == ' '; index--) {
            trailingSpaces++;
        }

        if (trailingSpaces > 0) {
            String prefix = beforeItem.substring(0, beforeItem.length() - trailingSpaces);
            renderCachedItemInline(mc, guiGraphics, prefix, 0.0F, x, y, color, stack);
            return true;
        }

        int drawX = Math.round(x + mc.font.width(beforeItem) - 9.0F);
        int drawY = Math.round(y);
        bitsandbalance$renderResolvedItemAt(mc, guiGraphics, drawX, drawY, color, stack.copy());
        return true;
    }

    private static void renderResolvedItemInline(Minecraft mc, GuiGraphicsExtractor guiGraphics, String before,
                                                 float extraShift, float x, float y, int color, ItemStack stack) {
        float colorAlpha = (color >> 24 & 255) / 255.0F;

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
        }

        float guiAlpha = bitsandbalance$getGuiGraphicsAlpha(guiGraphics);

        float alpha = colorAlpha * bitsandbalance$chatLineAlphaTL.get() * shaderAlpha * guiAlpha;
        if (alpha < 0.0F) alpha = 0.0F;
        if (alpha > 1.0F) alpha = 1.0F;

        if (stack.isEmpty()) {
            stack = new ItemStack(Blocks.BARRIER);
        }

        float shift = mc.font.width(before) + extraShift + 4.0F;
        int drawX = Math.round(shift + x);
        int drawY = Math.round(y);

        bitsandbalance$renderResolvedItemAt(mc, guiGraphics, drawX, drawY, color, stack);
    }

    private static void bitsandbalance$renderResolvedItemAt(Minecraft mc, GuiGraphicsExtractor guiGraphics,
                                                            int drawX, int drawY, int color, ItemStack stack) {
        float colorAlpha = (color >> 24 & 255) / 255.0F;

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
        }

        float guiAlpha = bitsandbalance$getGuiGraphicsAlpha(guiGraphics);

        float alpha = colorAlpha * bitsandbalance$chatLineAlphaTL.get() * shaderAlpha * guiAlpha;
        if (alpha < 0.0F) alpha = 0.0F;
        if (alpha > 1.0F) alpha = 1.0F;

        float scaledX = (drawX - ITEM_SHARE_X_OFFSET) / ITEM_SHARE_SCALE;
        float scaledY = (drawY - ITEM_SHARE_Y_OFFSET) / ITEM_SHARE_SCALE;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(ITEM_SHARE_SCALE, ITEM_SHARE_SCALE);
        beginAlphaRender(alpha);
        try {
            guiGraphics.item(stack, Math.round(scaledX), Math.round(scaledY));
            return;
        } catch (Throwable ignored) {
        } finally {
            endAlphaRender();
            guiGraphics.pose().popMatrix();
        }

        TextureAtlasSprite sprite = bitsandbalance$getParticleIconSprite(mc, stack);
        if (sprite != null) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, drawX, drawY, 8, 8, ARGB.color(alpha, -1));
        }
    }

    public static boolean isRenderingWithAlpha() {
        return isRenderingWithAlpha;
    }

    private static ItemStack bitsandbalance$resolveShowItemStack(HoverEvent.ShowItem showItem) {
        try {
            Method itemMethod = bitsandbalance$showItemItemMethod;
            if (itemMethod == null) {
                itemMethod = showItem.getClass().getDeclaredMethod("item");
                itemMethod.setAccessible(true);
                bitsandbalance$showItemItemMethod = itemMethod;
            }

            Object hoverValue = itemMethod.invoke(showItem);
            if (hoverValue instanceof ItemStack stack) {
                return stack.copy();
            }
            if (hoverValue != null) {
                Method createMethod = bitsandbalance$showItemValueCreateMethod;
                if (createMethod == null) {
                    try {
                        createMethod = hoverValue.getClass().getDeclaredMethod("create");
                        createMethod.setAccessible(true);
                        bitsandbalance$showItemValueCreateMethod = createMethod;
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
                if (createMethod != null) {
                    Object created = createMethod.invoke(hoverValue);
                    if (created instanceof ItemStack stack) {
                        return stack;
                    }
                }

                Method copyMethod = bitsandbalance$showItemValueCopyMethod;
                if (copyMethod == null) {
                    try {
                        copyMethod = hoverValue.getClass().getDeclaredMethod("copy");
                        copyMethod.setAccessible(true);
                        bitsandbalance$showItemValueCopyMethod = copyMethod;
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
                if (copyMethod != null) {
                    Object copied = copyMethod.invoke(hoverValue);
                    if (copied instanceof ItemStack stack) {
                        return stack;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            return showItem.item().copy();
        } catch (Throwable ignored) {
        }

        return ItemStack.EMPTY;
    }

    private static TextureAtlasSprite bitsandbalance$getParticleIconSprite(Minecraft mc, ItemStack stack) {
        ItemModelResolver resolver = mc.getItemModelResolver();
        if (resolver == null) {
            return null;
        }

        ItemStackRenderState renderState = new ItemStackRenderState();
        resolver.updateForTopItem(renderState, stack, ItemDisplayContext.GUI, mc.level, null, 0);
        if (renderState.isEmpty()) {
            return null;
        }

        try {
            Method pickMaterialMethod = bitsandbalance$pickParticleMaterialMethod;
            if (pickMaterialMethod == null) {
                try {
                    pickMaterialMethod = renderState.getClass().getDeclaredMethod("pickParticleMaterial", RandomSource.class);
                    pickMaterialMethod.setAccessible(true);
                    bitsandbalance$pickParticleMaterialMethod = pickMaterialMethod;
                } catch (ReflectiveOperationException ignored) {
                }
            }
            if (pickMaterialMethod != null) {
                Object material = pickMaterialMethod.invoke(renderState, RandomSource.create());
                if (material != null) {
                    Method spriteMethod = bitsandbalance$particleMaterialSpriteMethod;
                    if (spriteMethod == null) {
                        spriteMethod = material.getClass().getDeclaredMethod("sprite");
                        spriteMethod.setAccessible(true);
                        bitsandbalance$particleMaterialSpriteMethod = spriteMethod;
                    }
                    Object sprite = spriteMethod.invoke(material);
                    if (sprite instanceof TextureAtlasSprite textureAtlasSprite) {
                        return textureAtlasSprite;
                    }
                }
            }

            Method pickIconMethod = bitsandbalance$pickParticleIconMethod;
            if (pickIconMethod == null) {
                try {
                    pickIconMethod = renderState.getClass().getDeclaredMethod("pickParticleIcon", RandomSource.class);
                    pickIconMethod.setAccessible(true);
                    bitsandbalance$pickParticleIconMethod = pickIconMethod;
                } catch (ReflectiveOperationException ignored) {
                }
            }
            if (pickIconMethod != null) {
                Object sprite = pickIconMethod.invoke(renderState, RandomSource.create());
                if (sprite instanceof TextureAtlasSprite textureAtlasSprite) {
                    return textureAtlasSprite;
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static ItemStack bitsandbalance$getCachedSharedItem(String fullText) {
        if (fullText == null || fullText.isBlank()) {
            return ItemStack.EMPTY;
        }
        String normalized = bitsandbalance$normalizeSharedMessageKey(fullText);
        String itemLabel = bitsandbalance$extractItemLabel(fullText);
        synchronized (bitsandbalance$sharedMessageItems) {
            ItemStack stack = bitsandbalance$sharedMessageItems.get(fullText);
            if (stack != null) {
                return stack.copy();
            }

            stack = bitsandbalance$sharedMessageItems.get(normalized);
            if (stack != null) {
                return stack.copy();
            }

            if (!itemLabel.isBlank()) {
                stack = bitsandbalance$sharedMessageItems.get(itemLabel);
                if (stack != null) {
                    return stack.copy();
                }
            }

            return ItemStack.EMPTY;
        }
    }

    private static String bitsandbalance$normalizeSharedMessageKey(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return CHAT_HEAD_PREFIX_PATTERN.matcher(text).replaceFirst("").trim();
    }

    private static String bitsandbalance$extractItemLabel(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        int start = text.lastIndexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return "";
    }

    private static String bitsandbalance$extractText(FormattedCharSequence sequence) {
        StringBuilder builder = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            builder.append((char) codePoint);
            return true;
        });
        return builder.toString();
    }

    private static ItemStack bitsandbalance$findSharedItemInComponent(Component component) {
        if (component == null) {
            return ItemStack.EMPTY;
        }

        HoverEvent hoverEvent = component.getStyle().getHoverEvent();
        if (hoverEvent instanceof HoverEvent.ShowItem showItem) {
            ItemStack stack = bitsandbalance$resolveShowItemStack(showItem);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        for (Component sibling : component.getSiblings()) {
            ItemStack stack = bitsandbalance$findSharedItemInComponent(sibling);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
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
