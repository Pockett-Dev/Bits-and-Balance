package org.onenonly.bitsandbalance.common.client;

//? if mc != 1.21.1
import net.minecraft.client.renderer.RenderPipelines;
//? endif
//? if mc != 1.21.1
import net.minecraft.client.gui.Gui;
//? endif
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.client.gui.GuiGraphics;
//? endif
//? if mc == 1.21.1
import net.minecraft.client.gui.components.toasts.ToastComponent;
//? endif
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? endif
//? if mc != 1.21.1
import net.minecraft.client.gui.components.toasts.ToastManager;
//? endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.toasts.Toast;
//? if mc == 1.21.1
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? endif
import net.minecraft.network.chat.Component;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.world.effect.MobEffects;

import java.util.concurrent.ThreadLocalRandom;

public final class SecondChanceToast implements Toast {
    private static final int DISPLAY_MS = 5000;
    private static final int ICON_SIZE = 16;
    private static final int TITLE_COLOR = 0xFFFFFF00;
//? if mc == 1.21.1
    private static final ResourceLocation ADVANCEMENT_BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
//? else
    private static final Identifier ADVANCEMENT_BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");
//? endif
//? if mc == 26.3
    private static final Identifier REGENERATION_ICON = Identifier.withDefaultNamespace("textures/mob_effect/regeneration.png");
//? endif
    private static final Object TOKEN = new Object();
    private static final Component TITLE = Component.literal("Second Chance!");
    private static final Component[] SUBTITLES = {
            Component.literal("You barely scraped by."),
            Component.literal("Make it count!"),
            Component.literal("It's not your time yet."),
            Component.literal("You've cheated death.")
    };

    private final Component subtitle = SUBTITLES[ThreadLocalRandom.current().nextInt(SUBTITLES.length)];
    private long firstSeenMs = -1L;
    private Visibility visibility = Visibility.SHOW;

    public static void show() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
//? if mc == 1.21.1
        minecraft.getToasts().addToast(new SecondChanceToast());
//? endif
//? if mc == 26.2 || mc == 26.3
        minecraft.gui.toastManager().addToast(new SecondChanceToast());
//? endif
//? if mc != 1.21.1 && mc != 26.2 && mc != 26.3
        minecraft.getToastManager().addToast(new SecondChanceToast());
//? endif
    }

//? if mc == 1.21.1
    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        renderContents(graphics, toastComponent.getMinecraft().font);
        return timeSinceLastVisible >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
    }
//? else
    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager toastManager, long timeMs) {
        if (firstSeenMs < 0L) {
            firstSeenMs = timeMs;
        }
        visibility = timeMs - firstSeenMs >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
    }

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long timeMs) {
        renderContents(graphics, font);
    }
//? else
    @Override
    public void render(GuiGraphics graphics, Font font, long timeMs) {
        renderContents(graphics, font);
    }
//? endif
//? endif

    @Override
    public Object getToken() {
        return TOKEN;
    }

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private void renderContents(GuiGraphicsExtractor graphics, Font font) {
//? else
    private void renderContents(GuiGraphics graphics, Font font) {
//? endif
        drawBackground(graphics);
        drawRegenerationIcon(graphics, 8, 8);
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
        graphics.text(font, TITLE, 30, 7, TITLE_COLOR);
        graphics.text(font, subtitle, 30, 18, 0xFFFFFFFF);
//? else
        graphics.drawString(font, TITLE, 30, 7, TITLE_COLOR, false);
        graphics.drawString(font, subtitle, 30, 18, 0xFFFFFFFF, false);
//? endif
    }

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private void drawBackground(GuiGraphicsExtractor graphics) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ADVANCEMENT_BACKGROUND, 0, 0, width(), height());
    }
//? endif
//? if mc == 1.21.1
    private void drawBackground(GuiGraphics graphics) {
        graphics.blitSprite(ADVANCEMENT_BACKGROUND, 0, 0, width(), height());
    }
//? endif
//? if mc != 26.1 && mc != 26.2 && mc != 26.3 && mc != 1.21.1
    private void drawBackground(GuiGraphics graphics) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ADVANCEMENT_BACKGROUND, 0, 0, width(), height());
    }
//? endif

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static void drawRegenerationIcon(GuiGraphicsExtractor graphics, int x, int y) {
//? if mc == 26.2 || mc == 26.3
//? if mc == 26.3
        graphics.blit(RenderPipelines.GUI_TEXTURED, REGENERATION_ICON, x, y, 0.0F, 0.0F,
                ICON_SIZE, ICON_SIZE, 18, 18, 18, 18);
//? else
        return;
//? endif
//? else
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(MobEffects.REGENERATION), x, y, ICON_SIZE, ICON_SIZE);
//? endif
    }
//? endif
//? if mc == 1.21.1
    private static void drawRegenerationIcon(GuiGraphics graphics, int x, int y) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(MobEffects.REGENERATION);
        graphics.blit(x, y, 0, ICON_SIZE, ICON_SIZE, sprite);
    }
//? endif
//? if mc != 26.1 && mc != 26.2 && mc != 26.3 && mc != 1.21.1
    private static void drawRegenerationIcon(GuiGraphics graphics, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(MobEffects.REGENERATION), x, y, ICON_SIZE, ICON_SIZE);
    }
//? endif
}
