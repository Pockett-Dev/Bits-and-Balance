package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;

/**
 * Fabric port: Auto Walk
 * Toggle key that simulates holding forward until user manually moves.
 */
public final class FabricAutoWalk {
    private static final Identifier HUD_LAYER_ID = Identifier.parse("bitsandbalance:auto_walk");

    private FabricAutoWalk() {
    }

    public static boolean active = false;

    private static Boolean prevAutoJump = null;
    private static boolean weSetForwardKey = false;
    private static int ticks = 0;

    public static void initClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            tick(client);
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.HELD_ITEM_TOOLTIP, HUD_LAYER_ID, (guiGraphics, tickDelta) -> {
            if (!active) return;
            if (FabricNavigatorCompass.isActive) return;
            if (FabricRecoveryCompassHudOverlay.isActive) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (mc.screen != null) return;

            Font font = mc.font;
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            Component base = Component.translatable("mymod.misc.autowalking");
            boolean variant = ((ticks / 10) % 2) == 0;
            String prefix = variant ? "OoO " : "oOo ";
            String suffix = variant ? " oOo" : " OoO";
            Component rendered = Component.literal(prefix).append(base).append(suffix);

            int textWidth = font.width(rendered);
            int x = (screenWidth - textWidth) / 2;
            int y = screenHeight - 54;

            guiGraphics.text(font, rendered, x, y, 0xFFFFFFFF, true);
        });
    }

    private static void tick(Minecraft mc) {
        if (!FabricClientConfig.enableAutoWalkKey) return;

        LocalPlayer player = mc.player;
        if (player == null) return;

        if (FabricKeyBindings.AUTO_WALK.consumeClick()) {
            active = !active;
            try {
                Options opts = mc.options;
                if (active) {
                    if (prevAutoJump == null) prevAutoJump = opts.autoJump().get();
                    opts.autoJump().set(true);
                } else {
                    restoreSettings(opts, false);
                }
            } catch (Throwable ignored) {
            }
        }

        if (!active) return;

        Options opts = mc.options;

        boolean forwardPhysDown = false;
        try {
            var key = opts.keyUp.getDefaultKey();
            if (key.getType() == InputConstants.Type.KEYSYM) {
                forwardPhysDown = InputConstants.isKeyDown(mc.getWindow(), key.getValue());
            }
        } catch (Throwable ignored) {
        }

        boolean manualMove = forwardPhysDown || opts.keyDown.isDown() || opts.keyLeft.isDown() || opts.keyRight.isDown();
        if (manualMove) {
            active = false;
            try {
                restoreSettings(opts, forwardPhysDown);
            } catch (Throwable ignored) {
            }
            return;
        }

        try {
            opts.keyUp.setDown(true);
            weSetForwardKey = true;
        } catch (Throwable ignored) {
        }
    }

    private static void restoreSettings(Options opts, boolean forwardPhysDown) {
        if (prevAutoJump != null) {
            opts.autoJump().set(prevAutoJump);
            prevAutoJump = null;
        }

        if (weSetForwardKey) {
            if (!forwardPhysDown) {
                try {
                    opts.keyUp.setDown(false);
                } catch (Throwable ignored) {
                }
            }
            weSetForwardKey = false;
        }
    }
}
