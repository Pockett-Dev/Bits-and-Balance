package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.onenonly.bitsandbalance.common.client.ArrowPriorityScreen;
import org.onenonly.bitsandbalance.fabric.network.ArrowSelectionSetPayload;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.network.SitTogglePayload;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;

/**
 * Fabric keybinds for Sitting.
 * Uses ToggleKeyMapping for toggle/hold based on config.
 */
public final class FabricKeyBindings {
    private FabricKeyBindings() {
    }

    private static final KeyMapping.Category CATEGORY = FabricKeyCategories.MAIN;

        public static final KeyMapping SIT = new KeyMapping(
            "key.bitsandbalance.sit",
            InputConstants.Type.KEYBOARD,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

        public static final KeyMapping ITEM_SHARE = new KeyMapping(
            "key.bitsandbalance.item_share",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_T,
            CATEGORY
        );

            public static final KeyMapping AUTO_WALK = new KeyMapping(
                "key.bitsandbalance.auto_walk",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
            );

            public static final KeyMapping DOOR_KNOCK = new KeyMapping(
                "key.bitsandbalance.door_knock",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
            );

    public static final KeyMapping ARROW_PRIORITY = new KeyMapping(
            "key.bitsandbalance.arrow_priority",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R,
            CATEGORY
    );

    private static boolean lastDesiredSit = false;
    private static boolean toggledSit = false;

    public static void initClient() {
        KeyMappingHelper.registerKeyMapping(SIT);
        KeyMappingHelper.registerKeyMapping(ITEM_SHARE);
        KeyMappingHelper.registerKeyMapping(AUTO_WALK);
        KeyMappingHelper.registerKeyMapping(DOOR_KNOCK);
        KeyMappingHelper.registerKeyMapping(ARROW_PRIORITY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen == null && ARROW_PRIORITY.consumeClick()) {
                ArrowPriorityScreen.openIfHolding(client, ARROW_PRIORITY, (hand, arrow) -> {
                    if (ClientPlayNetworking.canSend(ArrowSelectionSetPayload.TYPE)) {
                        ClientPlayNetworking.send(new ArrowSelectionSetPayload(hand, arrow));
                    }
                });
            }
            ArrowPriorityScreen.tick(client);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.HELD_ITEM_TOOLTIP,
                Identifier.parse("bitsandbalance:arrow_priority"),
                (graphics, tickDelta) -> ArrowPriorityScreen.render(graphics,
                        org.onenonly.bitsandbalance.fabric.config.FabricClientConfig.arrowPriorityOffsetX,
                        org.onenonly.bitsandbalance.fabric.config.FabricClientConfig.arrowPriorityOffsetY,
                        org.onenonly.bitsandbalance.fabric.config.FabricClientConfig.arrowPriorityShowArrowSlot));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            boolean mountedOnSeat = org.onenonly.bitsandbalance.common.mechanics.SittingSeatHelper.isMountedOnSeat(mc.player);
            boolean mounted = mc.player.isPassenger() && !mountedOnSeat;

            boolean onGround = false;
            try {
                onGround = mc.player.onGround();
            } catch (Throwable ignored) {
            }

            if (!FabricTweaksConfig.enableSitting) {
                if (lastDesiredSit || FabricClientState.sitting || toggledSit) {
                    toggledSit = false;
                    lastDesiredSit = false;
                    FabricClientState.sitting = false;
                    try {
                        if (!mc.player.isPassenger() && mc.player.getPose() == net.minecraft.world.entity.Pose.SITTING) {
                            mc.player.setPose(net.minecraft.world.entity.Pose.STANDING);
                            mc.player.refreshDimensions();
                        }
                    } catch (Throwable ignored) {
                    }
                    if (ClientPlayNetworking.canSend(SitTogglePayload.TYPE)) {
                        ClientPlayNetworking.send(new SitTogglePayload(false));
                    }
                }
                return;
            }

            boolean desired;
            if (FabricTweaksConfig.toggleSitting) {
                if (SIT.consumeClick()) {
                    int required = KeybindModifierStore.getRequiredMask(SIT);
                    boolean modifiersOk = required == 0 || KeybindModifierStore.areRequiredModifiersDown(required);
                    if (modifiersOk) {
                        if (toggledSit || FabricClientState.sitting || mountedOnSeat) {
                            toggledSit = false;
                        } else if (onGround) {
                            toggledSit = true;
                        }
                    }
                }
                desired = toggledSit;
            } else {
                desired = SIT.isDown();
                int required = KeybindModifierStore.getRequiredMask(SIT);
                if (required != 0 && !KeybindModifierStore.areRequiredModifiersDown(required)) {
                    desired = false;
                }
                // Keep state consistent when switching configs or releasing key.
                if (!desired) {
                    toggledSit = false;
                }
            }

            if (mounted) {
                desired = false;
                toggledSit = false;
            }

            if (desired == lastDesiredSit) return;

            if (!onGround && desired) {
                // Don't allow sitting mid-air.
                FabricClientState.sitting = false;
                try {
                    if (mc.player.getPose() == net.minecraft.world.entity.Pose.SITTING) {
                        mc.player.setPose(net.minecraft.world.entity.Pose.STANDING);
                        mc.player.refreshDimensions();
                    }
                } catch (Throwable ignored) {
                }
                // Prevent auto-applying the moment you land.
                if (FabricTweaksConfig.toggleSitting) {
                    toggledSit = false;
                }
                lastDesiredSit = false;
                return;
            }

            FabricClientState.sitting = desired;

            // Apply pose immediately client-side to reduce flicker/latency.
            try {
                if (desired && !mc.player.isPassenger()) {
                    mc.player.setPose(net.minecraft.world.entity.Pose.SITTING);
                } else if (!desired && !mc.player.isPassenger() && mc.player.getPose() == net.minecraft.world.entity.Pose.SITTING) {
                    mc.player.setPose(net.minecraft.world.entity.Pose.STANDING);
                }
                mc.player.refreshDimensions();
            } catch (Throwable ignored) {
            }

            if (ClientPlayNetworking.canSend(SitTogglePayload.TYPE)) {
                ClientPlayNetworking.send(new SitTogglePayload(desired));
            }

            lastDesiredSit = desired;
        });
    }
}
