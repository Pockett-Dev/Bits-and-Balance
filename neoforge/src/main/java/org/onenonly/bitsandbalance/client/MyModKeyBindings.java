package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.mechanics.SitPayload;
import org.onenonly.bitsandbalance.Config;
/**
 * Keybindings for Sitting using ToggleKeyMapping and a toggle/hold supplier.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public final class MyModKeyBindings {
    private static final KeyMapping.Category CATEGORY = BnbKeyCategories.MAIN;
    private MyModKeyBindings() {}


    public static KeyMapping SIT = new ToggleKeyMapping(
            "key.bitsandbalance.sit",
            -1, // Unbound by default
            CATEGORY,
            () -> Config.toggleSitting,
            false
    );

    public static KeyMapping ITEM_SHARE = new KeyMapping(
            "key.bitsandbalance.item_share",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T, // Default bound to T (with Shift modifier in config)
            CATEGORY
    );

    private static boolean lastDesiredSit = false;
    private static boolean sitKeyRegistered = false;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        // Prevent duplicate key registration if event fires multiple times
        if (sitKeyRegistered) {
            return;
        }

        event.register(SIT);
        event.register(ITEM_SHARE);
        // IN_GAME conflict context API is optional in NeoForge 1.21; omitted for minimal compatibility

        sitKeyRegistered = true;
    }


    @SubscribeEvent
    public static void onClientTickEnd(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        // ToggleKeyMapping maintains a pressed/consumed state consistent with toggle/hold
        boolean desired = SIT.isDown();
        if (desired != lastDesiredSit) {
            // Do not allow attempting to sit while airborne/jumping: ignore keybind change if not on ground
            boolean onGround = false;
            if (mc.player != null) {
                try { onGround = mc.player.onGround(); } catch (Throwable ignored) {}
            }
            if (!onGround) {
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
}
