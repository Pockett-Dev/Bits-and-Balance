package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.onenonly.bitsandbalance.BitsAndBalance;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.onenonly.bitsandbalance.mechanics.CrawlPayload;
import org.onenonly.bitsandbalance.mechanics.SneakPayload;
import org.onenonly.bitsandbalance.mechanics.DoorKnockPayload;
import org.onenonly.bitsandbalance.mechanics.RapidFireJump;
import org.onenonly.bitsandbalance.tweaks.CoyoteTimeJump;

import org.onenonly.bitsandbalance.Config;
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class MechanicsClient {
    private static final KeyMapping.Category CATEGORY = BnbKeyCategories.MAIN;

    public static KeyMapping TOGGLE_STANCE;
    public static KeyMapping CRAWL_KEY;
    public static KeyMapping DOOR_KNOCK_KEY;

    private static boolean lastCrawlSent = false;
    private static boolean crawlByToggle = false;

    private static boolean sneakingByToggle = false;
    private static boolean lastSneakSent = false;

    private static boolean lastJumpKeyPressedForCoyoteTime = false;


    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        // All keybinds unbound by default
        if (TOGGLE_STANCE == null) {
            TOGGLE_STANCE = new KeyMapping("key.bitsandbalance.toggle_stance", InputConstants.Type.KEYSYM, -1, CATEGORY); // Unbound
        }
        if (CRAWL_KEY == null) {
            CRAWL_KEY = new KeyMapping("key.bitsandbalance.crawl", InputConstants.Type.KEYSYM, -1, CATEGORY); // Unbound
        }
        if (DOOR_KNOCK_KEY == null) {
            DOOR_KNOCK_KEY = new KeyMapping("key.bitsandbalance.door_knock", InputConstants.Type.KEYSYM, -1, CATEGORY); // Unbound
        }
        event.register(TOGGLE_STANCE);
        event.register(CRAWL_KEY);
        event.register(DOOR_KNOCK_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            lastJumpKeyPressedForCoyoteTime = false;
            return;
        }

        boolean jumpKeyPressed = false;
        try {
            jumpKeyPressed = mc.options != null && mc.options.keyJump.isDown();
        } catch (Throwable ignored) {
        }
        boolean jumpKeyJustPressedForCoyoteTime = jumpKeyPressed && !lastJumpKeyPressedForCoyoteTime;
        lastJumpKeyPressedForCoyoteTime = jumpKeyPressed;

        // Handle crawl state from hold key and toggle-state
        if (Config.enableCrawlingMechanic) {
            boolean holdCrawl = CRAWL_KEY != null && CRAWL_KEY.isDown();
            boolean wantCrawl = holdCrawl || crawlByToggle;
            if (wantCrawl != lastCrawlSent) {
                sendCrawlPacket(wantCrawl);
                lastCrawlSent = wantCrawl;
            }
        } else if (lastCrawlSent || crawlByToggle) {
            // Crawling disabled in config; ensure we exit crawl if previously active
            sendCrawlPacket(false);
            lastCrawlSent = false;
            crawlByToggle = false;
        }

        // Client-side visual enforcement of pose to avoid flicker
        try {
            if (lastCrawlSent) {
                if (player.getForcedPose() != Pose.SWIMMING) player.setForcedPose(Pose.SWIMMING);
            } else {
                if (player.getForcedPose() == Pose.SWIMMING) player.setForcedPose(null);
            }
        } catch (Throwable ignored) {
        }

        // Handle coyote time jump
        if (Config.enableCoyoteTimeJump) {
            CoyoteTimeJump.updateClientState(player);
            handleCoyoteTimeJump(player, jumpKeyJustPressedForCoyoteTime);
        }

        // Handle rapid fire jump
        if (Config.enableRapidFireJump) {
            handleRapidFireJump(player);
        }

        // Evaluate desired sneaking each tick and keep it in sync client/server
        boolean baselineSneak;
        if (lastCrawlSent) {
            // While crawling, do not sneak
            baselineSneak = false;
        } else {
            boolean toggleCrouchEnabled = mc.options != null && mc.options.toggleCrouch().get();
            if (toggleCrouchEnabled) {
                // In vanilla toggle-crouch mode, the actual sneaking state is managed by the game
                baselineSneak = player.isShiftKeyDown();
            } else {
                // In hold mode, use the physical key state
                baselineSneak = mc.options != null && mc.options.keyShift.isDown();
                // In hold mode, reset toggle state when physical key is pressed (to avoid conflicts)
                if (baselineSneak && sneakingByToggle) {
                    sneakingByToggle = false;
                }
            }
        }
        boolean wantSneak = baselineSneak || sneakingByToggle;
        // In hold mode, also set the keybinding state so the movement packet reports crouching correctly.
        try {
            boolean toggleCrouchEnabled2 = mc.options != null && mc.options.toggleCrouch().get();
            if (!toggleCrouchEnabled2) {
                mc.options.keyShift.setDown(wantSneak);
            }
        } catch (Throwable ignored) {
        }
        if (player.isShiftKeyDown() != wantSneak) {
            try {
                player.setShiftKeyDown(wantSneak);
            } catch (Throwable ignored) {
            }
        }
        if (lastSneakSent != wantSneak) {
            sendSneakPacket(wantSneak);
            lastSneakSent = wantSneak;
        }

        // Handle Toggle Stance
        if (Config.enableToggleStance && TOGGLE_STANCE != null && TOGGLE_STANCE.consumeClick()) {
            boolean crawlingEnabled = Config.enableCrawlingMechanic;
            boolean isCrawling = lastCrawlSent; // what we asked server to do
            boolean isSneaking = wantSneak;

            if (crawlingEnabled) {
                if (isCrawling) {
                    // Crawl -> Stand
                    sendCrawlPacket(false);
                    lastCrawlSent = false;
                    crawlByToggle = false;
                    sneakingByToggle = false;
                } else if (isSneaking) {
                    // Sneak -> Crawl
                    sneakingByToggle = false;
                    sendCrawlPacket(true);
                    lastCrawlSent = true;
                    crawlByToggle = true;
                } else {
                    // Stand -> Sneak
                    crawlByToggle = false;
                    sneakingByToggle = true;
                }
            } else {
                // Only toggle sneak
                sneakingByToggle = !isSneaking;
            }
        }

        // Handle Door Knock keybind
        if (Config.enableDoorKnocking && DOOR_KNOCK_KEY != null && DOOR_KNOCK_KEY.consumeClick()) {
            handleDoorKnock(player);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void sendCrawlPacket(boolean crawl) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getConnection() != null) {
                var connection = minecraft.getConnection();
                if (connection != null) {
                    connection.send(new CrawlPayload(crawl));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void sendSneakPacket(boolean sneaking) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getConnection() != null) {
                var connection = minecraft.getConnection();
                if (connection != null) {
                    connection.send(new SneakPayload(sneaking));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void sendDoorKnockPacket(BlockPos doorPos) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getConnection() != null) {
                var connection = minecraft.getConnection();
                if (connection != null) {
                    connection.send(new DoorKnockPayload(doorPos));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void handleDoorKnock(LocalPlayer player) {
        try {
            // Get the block the player is looking at
            var hitResult = player.pick(5.0, 1.0f, false);
            if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var blockHitResult = (net.minecraft.world.phys.BlockHitResult) hitResult;
                var pos = blockHitResult.getBlockPos();
                var level = player.level();
                
                if (level != null) {
                    var state = level.getBlockState(pos);
                    // Check if it's a door block
                    if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) {
                        sendDoorKnockPacket(pos);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
    
    private static void handleCoyoteTimeJump(LocalPlayer player, boolean jumpKeyJustPressed) {
        try {
            // Never trigger coyote-time jumps from a held jump key; require a fresh key press.
            if (jumpKeyJustPressed && !player.onGround() && CoyoteTimeJump.canUseCoyoteTime(player)) {
                // Match Fabric behavior: trigger a normal vanilla jump and let our jump mixin
                // mark coyote time as used.
                player.jumpFromGround();
            }
        } catch (Throwable ignored) {
        }
    }
    
    private static void handleRapidFireJump(LocalPlayer player) {
        try {
            // Check if player is holding the jump key
            boolean jumpKeyPressed = Minecraft.getInstance().options.keyJump.isDown();
            
            // Update client state
            RapidFireJump.updateClientState(player, jumpKeyPressed);
            
            // Check if we can perform rapid fire jump
            if (jumpKeyPressed && RapidFireJump.canRapidFireJump(player, jumpKeyPressed)) {
                // Apply rapid fire jump by directly setting velocity
                if (Config.rapidFireJumpDebug) {
                    System.out.println("[RapidFireJump] Client: Player " + player.getName().getString() + " attempting rapid fire jump");
                }
                RapidFireJump.performRapidFireJump(player);
            }
            
            // Additional debug logging
            if (Config.rapidFireJumpDebug && jumpKeyPressed) {
                System.out.println("[RapidFireJump] Client debug: jumpKeyPressed=" + jumpKeyPressed + 
                    ", onGround=" + player.onGround() + ", playerY=" + String.format("%.2f", player.getY()));
            }
        } catch (Throwable ignored) {
        }
    }
}