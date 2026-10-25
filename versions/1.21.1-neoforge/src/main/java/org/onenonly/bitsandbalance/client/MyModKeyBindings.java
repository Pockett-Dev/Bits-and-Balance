package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.mechanics.SitPayload;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.ClientConfig;
import org.onenonly.bitsandbalance.common.client.ArrowPriorityScreen;
import org.onenonly.bitsandbalance.mechanics.ArrowSelectionSetPayload;
/**
 * Keybindings for Sitting.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public final class MyModKeyBindings {
    private static final String CATEGORY = BnbKeyCategories.MAIN;
    private MyModKeyBindings() {}


        public static KeyMapping SIT = new KeyMapping(
            "key.bitsandbalance.sit",
            InputConstants.Type.KEYSYM,
            -1,
            CATEGORY
    );

    public static KeyMapping ITEM_SHARE = new KeyMapping(
            "key.bitsandbalance.item_share",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T, // Default bound to T (with Shift modifier in config)
            CATEGORY
    );

    public static final KeyMapping ARROW_PRIORITY = new KeyMapping(
            "key.bitsandbalance.arrow_priority",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    private static boolean lastDesiredSit = false;
    private static boolean toggledSit = false;
    private static boolean lastSitKeyDown = false;
    private static boolean sitKeyRegistered = false;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        // Prevent duplicate key registration if event fires multiple times
        if (sitKeyRegistered) {
            return;
        }

        event.register(SIT);
        event.register(ITEM_SHARE);
        event.register(ARROW_PRIORITY);
        // IN_GAME conflict context API is optional in NeoForge 1.21; omitted for minimal compatibility

        sitKeyRegistered = true;
    }


    @SubscribeEvent
    public static void onClientTickEnd(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        if (mc.screen == null && ARROW_PRIORITY.consumeClick()) {
            ArrowPriorityScreen.openIfHolding(mc, ARROW_PRIORITY,
                    (hand, arrow) -> mc.getConnection().send(new ArrowSelectionSetPayload(hand, arrow)));
        }
        ArrowPriorityScreen.tick(mc);

        if (!Config.enableSitting) {
            if (lastDesiredSit || ClientState.sitting || toggledSit) {
                toggledSit = false;
                lastDesiredSit = false;
                ClientState.sitting = false;
                org.onenonly.bitsandbalance.client.SitClientEvents.updateVisualSitting(false);
                mc.getConnection().send(new SitPayload(false));
            }
            return;
        }

        boolean sitKeyDown = SIT.isDown();
        boolean sitJustPressed = sitKeyDown && !lastSitKeyDown;
        lastSitKeyDown = sitKeyDown;
        boolean sittingOnSeat = org.onenonly.bitsandbalance.common.mechanics.SittingSeatHelper.isMountedOnSeat(mc.player);

        boolean desired;
        if (Config.toggleSitting) {
            if (sitJustPressed) {
                if (toggledSit || ClientState.sitting || sittingOnSeat) {
                    toggledSit = false;
                } else if (mc.player.onGround()) {
                    toggledSit = true;
                }
            }
            desired = toggledSit;
        } else {
            desired = sitKeyDown;
            if (!desired) {
                toggledSit = false;
            }
        }

        if (mc.player.isPassenger() && !sittingOnSeat) {
            desired = false;
            toggledSit = false;
        }

        if (desired != lastDesiredSit) {
            // Do not allow attempting to sit while airborne/jumping: ignore keybind change if not on ground
            boolean onGround = false;
            if (mc.player != null) {
                try { onGround = mc.player.onGround(); } catch (Throwable ignored) {}
            }
            if (desired && !onGround) {
                // Consume the change locally so it won't apply automatically upon landing
                lastDesiredSit = desired;
                return;
            }
            // Update client state immediately for responsive movement restriction
            ClientState.sitting = desired;
            // Update visual effects immediately
            org.onenonly.bitsandbalance.client.SitClientEvents.updateVisualSitting(desired);
            // Send desired sit state to server; server will sync back if state is invalid
            var connection = mc.getConnection();
            if (connection != null) {
                connection.send(new SitPayload(desired));
            }
            lastDesiredSit = desired;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        ArrowPriorityScreen.render(event.getGuiGraphics(), ClientConfig.arrowPriorityOffsetX,
                ClientConfig.arrowPriorityOffsetY, ClientConfig.arrowPriorityShowArrowSlot);
    }
}
