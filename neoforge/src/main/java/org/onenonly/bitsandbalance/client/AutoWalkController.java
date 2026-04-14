package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import com.mojang.blaze3d.platform.InputConstants;

import org.onenonly.bitsandbalance.Config;
/**
 * Handles Auto Walk toggle and per-tick behavior.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class AutoWalkController {

    private static Boolean prevAutoJump = null;
    private static boolean weSetForwardKey = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.enableAutoWalkKey) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (AutoWalkKeyHandler.AUTO_WALK_KEY == null) return;

        // Toggle on key press
        if (AutoWalkKeyHandler.AUTO_WALK_KEY.consumeClick()) {
            AutoWalkKeyHandler.active = !AutoWalkKeyHandler.active;
            try {
                Options opts = mc.options;
                if (AutoWalkKeyHandler.active) {
                    // Save and force-enable Auto Jump while Auto Walk is active
                    if (prevAutoJump == null) prevAutoJump = opts.autoJump().get();
                    opts.autoJump().set(true);
                } else {
                    // Turning off: restore Auto Jump and release our simulated forward key if we set it
                    if (prevAutoJump != null) {
                        opts.autoJump().set(prevAutoJump);
                        prevAutoJump = null;
                    }
                    if (weSetForwardKey) {
                        try { opts.keyUp.setDown(false); } catch (Throwable ignored) {}
                        weSetForwardKey = false;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (!AutoWalkKeyHandler.active) return;

        Options opts = mc.options;
        // Detect a physical Forward press (not our simulated press) using OS-level key state
        boolean forwardPhysDown = false;
        try {
            var key = opts.keyUp.getKey();
            if (key.getType() == InputConstants.Type.KEYSYM) {
                forwardPhysDown = InputConstants.isKeyDown(mc.getWindow(), key.getValue());
            }
        } catch (Throwable ignored) { }

        // If the user manually presses any movement key (including Forward physically), cancel Auto Walk and restore settings
        boolean manualMove = forwardPhysDown || opts.keyDown.isDown() || opts.keyLeft.isDown() || opts.keyRight.isDown();
        if (manualMove) {
            AutoWalkKeyHandler.active = false;
            try {
                if (prevAutoJump != null) {
                    opts.autoJump().set(prevAutoJump);
                    prevAutoJump = null;
                }
                if (weSetForwardKey) {
                    // If player physically pressed Forward, don't release the key this tick to avoid a pause.
                    if (!forwardPhysDown) {
                        opts.keyUp.setDown(false);
                    }
                    // Regardless, stop tracking our simulated press so we won't force-release later.
                    weSetForwardKey = false;
                }
            } catch (Throwable ignored) {
            }
            return;
        }

        // Simulate holding forward and reinforce input this tick
        try {
            opts.keyUp.setDown(true);
            weSetForwardKey = true;
        } catch (Throwable ignored) {
        }

        // Note: In 1.21.10 client input internals changed; holding the key mapping down is sufficient.
    }
}
