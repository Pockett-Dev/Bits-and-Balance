package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? else
import net.minecraft.client.gui.GuiGraphics;
//? endif
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Reads the two server-spawned avatar markers without a separate network protocol. */
public final class DreamProjectionClient {
    private static ClientLevel cachedLevel;
    private static long cachedTick = Long.MIN_VALUE;
    private static final Set<UUID> PROJECTED = new HashSet<>();
    private static final ThreadLocal<Boolean> RENDERING_GHOST_AVATAR = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> RENDERED_GHOST_AGE = ThreadLocal.withInitial(() -> 0);
    private static int localBodyAge;
    private static boolean wasProjected;
    private static long enteredAt;
    private static long returnedAt;
    private static LocalPlayer movementPlayer;
    private static boolean returning;
    private static int lastWarningStage;
    private static int warningsPlayed;
    private static boolean previousFlying;
    private static boolean previousMayfly;
    private static double entryHorizontalSpeed;
    private static int glideAge;

    private DreamProjectionClient() {
    }

    public static UUID owner(Entity entity, String prefix) {
        if (entity == null || entity.getCustomName() == null) {
            return null;
        }
        String name = entity.getCustomName().getString();
        if (!name.startsWith(prefix)) {
            return null;
        }
        try {
            return UUID.fromString(name.substring(prefix.length()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean isBody(Entity entity) {
        return owner(entity, DreamProjection.BODY_NAME_PREFIX) != null;
    }

    public static boolean isGhost(Entity entity) {
        return owner(entity, DreamProjection.GHOST_NAME_PREFIX) != null;
    }

    public static boolean isProjected(Player player) {
        if (RENDERING_GHOST_AVATAR.get()) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || player != minecraft.player || player.level() != level) {
            return false;
        }
        if (cachedLevel != level || cachedTick != level.getGameTime()) {
            cachedLevel = level;
            cachedTick = level.getGameTime();
            PROJECTED.clear();
            localBodyAge = 0;
            for (Entity entity : level.entitiesForRendering()) {
                UUID owner = owner(entity, DreamProjection.BODY_NAME_PREFIX);
                if (owner != null) {
                    PROJECTED.add(owner);
                    if (owner.equals(player.getUUID())) {
                        localBodyAge = entity.tickCount;
                    }
                }
            }
        }
        return PROJECTED.contains(player.getUUID());
    }

    public static void renderingGhostAvatar(boolean rendering, int age) {
        RENDERING_GHOST_AVATAR.set(rendering);
        RENDERED_GHOST_AGE.set(age);
    }

    /** The local player is the ghost; body motion packets must not steer it. */
    public static boolean ignoreBodyMotion(LocalPlayer player) {
        return !returning && isProjected(player);
    }

    public static void beginReturn(LocalPlayer player) {
        if (isProjected(player)) {
            returning = true;
        }
    }

    public static int warningCount() {
        return warningsPlayed;
    }

    /** Keep creative or spectator flight on the AFK body, not on its local ghost controller. */
    public static void prepareGlide(LocalPlayer player) {
        if (!isProjected(player)) {
            if (movementPlayer == player) {
                player.getAbilities().flying = previousFlying;
                player.getAbilities().mayfly = previousMayfly;
            }
            movementPlayer = null;
            returning = false;
            lastWarningStage = 0;
            warningsPlayed = 0;
            entryHorizontalSpeed = 0.0D;
            glideAge = 0;
            return;
        }
        if (movementPlayer != player) {
            movementPlayer = player;
            previousFlying = player.getAbilities().flying;
            previousMayfly = player.getAbilities().mayfly;
            Vec3 entryMotion = player.getDeltaMovement();
            entryHorizontalSpeed = Math.hypot(entryMotion.x, entryMotion.z);
            glideAge = 0;
            lastWarningStage = 0;
            warningsPlayed = 0;
        }
        player.getAbilities().flying = returning && previousFlying;
        player.getAbilities().mayfly = returning && previousMayfly;
    }

    /** Glide vertically while keeping airborne steering as responsive as ordinary walking. */
    public static void tickGlide(LocalPlayer player) {
        if (!isProjected(player) || returning) {
            return;
        }
        int age = glideAge++;
        int warningStage = localBodyAge >= DreamProjection.DURATION_TICKS - 20 ? 3
                : localBodyAge >= DreamProjection.DURATION_TICKS - 40 ? 2
                : localBodyAge >= DreamProjection.DURATION_TICKS - 60 ? 1 : 0;
        if (warningStage > lastWarningStage) {
            for (int stage = lastWarningStage + 1; stage <= warningStage; stage++) {
                float pitch = stage == 1 ? 0.75F : stage == 2 ? 1.0F : 1.3F;
                player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.85F, pitch);
                warningsPlayed++;
            }
            lastWarningStage = warningStage;
        }
        if (player.onGround() || player.isInWater() || player.isInLava()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        Minecraft minecraft = Minecraft.getInstance();
        boolean ascending = minecraft.screen == null && minecraft.options.keyJump.isDown();
        // Keep the normal jump impulse when space is held, then settle into a gentle climb.
        double vertical = ascending ? Math.max(motion.y, 0.11D)
                : motion.y < 0.0D ? Math.max(-0.39D, motion.y + 0.04D) : motion.y;
        double horizontalX = motion.x;
        double horizontalZ = motion.z;
        // The body and ghost share the local player's motion on entry. Ease the extra
        // steering drag in so a run, jump, or knockback carries through the switch.
        double transition = Math.min(1.0D, age / 20.0D);
        double eased = transition * transition * (3.0D - 2.0D * transition);
        double steeringDrag = 0.98D - 0.58D * eased;
        double forward = minecraft.screen == null ? player.zza : 0.0D;
        double strafe = minecraft.screen == null ? player.xxa : 0.0D;
        double inputLength = Math.hypot(forward, strafe);
        if (inputLength < 0.01D) {
            // Keep the entry momentum, then return to the usual ghost steering drag.
            horizontalX *= steeringDrag;
            horizontalZ *= steeringDrag;
        } else {
            double yaw = Math.toRadians(player.getYRot());
            double wishX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) / inputLength;
            double wishZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) / inputLength;
            double along = horizontalX * wishX + horizontalZ * wishZ;
            double sidewaysX = horizontalX - along * wishX;
            double sidewaysZ = horizontalZ - along * wishZ;
            horizontalX = wishX * (along < 0.0D ? along * steeringDrag : along) + sidewaysX * steeringDrag;
            horizontalZ = wishZ * (along < 0.0D ? along * steeringDrag : along) + sidewaysZ * steeringDrag;
            // Vanilla travel adds its own air acceleration on the next tick. Keep the
            // stored velocity below the walk or sprint speed after that acceleration.
            double normalMaxSpeed = Math.max(0.0D, player.getAttributeValue(Attributes.MOVEMENT_SPEED)) * 1.95D;
            double carriedSpeed = entryHorizontalSpeed * Math.pow(0.91D, age);
            double maxSpeed = Math.max(normalMaxSpeed, carriedSpeed);
            double speed = Math.hypot(horizontalX, horizontalZ);
            if (speed > maxSpeed && speed > 0.0D) {
                horizontalX *= maxSpeed / speed;
                horizontalZ *= maxSpeed / speed;
            }
        }
        if (horizontalX != motion.x || vertical != motion.y || horizontalZ != motion.z) {
            player.setDeltaMovement(horizontalX, vertical, horizontalZ);
        }
        player.fallDistance = 0.0F;
    }

    public static int ghostColor(Entity entity) {
        int age = RENDERING_GHOST_AVATAR.get() ? RENDERED_GHOST_AGE.get()
                : isGhost(entity) ? entity.tickCount : localBodyAge;
        double entrance = Math.min(1.0D, age / 10.0D);
        double exit = Math.max(0.0D, Math.min(1.0D, (DreamProjection.DURATION_TICKS - age) / 10.0D));
        int alpha = isGhost(entity) || RENDERING_GHOST_AVATAR.get()
                ? (int) Math.round(64.0D * entrance * exit)
                : (int) Math.round((255.0D - 191.0D * entrance) * exit);
        return Math.max(0, Math.min(255, alpha)) << 24 | 0xFFFFFF;
    }

    public static int ghostColor(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0x40FFFFFF : ghostColor(minecraft.level.getEntity(entityId));
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public static void renderTransition(GuiGraphicsExtractor graphics) {
    //? else
    public static void renderTransition(GuiGraphics graphics) {
    //? endif
        Minecraft minecraft = Minecraft.getInstance();
        boolean projected = minecraft.player != null && isProjected(minecraft.player);
        long now = System.nanoTime();
        if (projected != wasProjected) {
            wasProjected = projected;
            if (projected) {
                enteredAt = now;
            } else {
                returnedAt = now;
            }
        }
        double opacity = 0.0D;
        if (projected) {
            double enter = Math.min(1.0D, (now - enteredAt) / 550_000_000.0D);
            opacity = Math.sin(Math.PI * enter) * 0.55D;
            if (localBodyAge >= DreamProjection.DURATION_TICKS - 12) {
                opacity = Math.max(opacity,
                        Math.min(0.68D, (localBodyAge - (DreamProjection.DURATION_TICKS - 12)) / 12.0D * 0.68D));
            }
        } else if (returnedAt != 0L) {
            opacity = Math.max(0.0D, 0.68D * (1.0D - (now - returnedAt) / 500_000_000.0D));
        }
        if (opacity > 0.0D) {
            int alpha = (int) (opacity * 255.0D);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0x211936);
        }
    }

    public static boolean isProjected(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        return isGhost(entity) || entity instanceof Player player && isProjected(player);
    }
}
