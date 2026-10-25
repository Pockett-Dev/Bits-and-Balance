package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Renders an animated "Auto-Walking" HUD label centered above the hotbar
 * whenever Auto Walk is active on the client.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class AutoWalkHudOverlay {
    private static int ticks = 0;
    // Optional client toggle to show/hide the HUD; exposed for future config wiring if desired.
    public static boolean drawHud = true;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // advance smooth animation counter on client tick
        ticks++;
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!drawHud) return;
        if (!AutoWalkKeyHandler.active) return;
        
        // Navigator compass has highest priority, then Recovery compass, then Auto-Walking text
        if (NavigatorCompassHudOverlay.isActive) return;
        if (RecoveryCompassHudOverlay.isActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        GuiGraphics gg = event.getGuiGraphics();
        Font font = mc.font;
        Window window = mc.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        // Localized base text
        Component base = Component.translatable("mymod.misc.autowalking");

        // Animate flourish every ~10 ticks
        boolean variant = ((ticks / 10) % 2) == 0;
        String prefix = variant ? "OoO " : "oOo ";
        String suffix = variant ? " oOo" : " OoO";
        Component rendered = Component.literal(prefix).append(base).append(suffix);

        int textWidth = font.width(rendered);
        // Center text horizontally - this should be perfectly centered
        int x = (screenWidth - textWidth) / 2;
        // Place just above the hotbar with dynamic positioning (armor + item name aware)
        int baseY = screenHeight - 54; // moved 6 pixels higher (was 8, now 6)
        int y = HudPositioningHelper.getDynamicYPosition(mc.player, baseY);

        // Draw in white with shadow as requested
        gg.drawString(font, rendered, x, y, 0xFFFFFFFF, true);
    }
}
