package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
//? if mc != 1.21.1
import net.minecraft.world.entity.player.Input;
//? endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
//? if mc == 1.21.1
import net.minecraft.world.entity.decoration.ArmorStand;
//? else
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.Mannequin;
import org.onenonly.bitsandbalance.mixin.MannequinProfileInvoker;
//? endif

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** The real player continues normal server-side life while their client controls a separate ghost avatar. */
public final class DreamProjection {
    public static final int DURATION_TICKS = 200;
    public static final int COOLDOWN_TICKS = 1200;
    public static final String BODY_NAME_PREFIX = "bitsandbalance:dream_body:";
    public static final String GHOST_NAME_PREFIX = "bitsandbalance:dream_ghost:";

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<Entity, Session> PROXIES = new IdentityHashMap<>();

    private DreamProjection() {
    }

    public static boolean start(ServerPlayer player) {
        if (!GoatHornPurposes.isEnabled("dream") || player.isSpectator() || player.isPassenger()
                || player.isSleeping() || !player.isAlive() || SESSIONS.containsKey(player.getUUID())
                || cooldownRemainingTicks(player) > 0) {
            return false;
        }
        //? if mc == 1.21.1
        ServerLevel level = player.serverLevel();
        //? else
        ServerLevel level = player.level();
        //? endif
        Entity bodyView = createAvatar(player, level, true);
        Entity ghost = createAvatar(player, level, false);
        if (bodyView == null || ghost == null) {
            return false;
        }
        float bodyYaw = player.yBodyRot;
        float headYaw = player.getYHeadRot();
        prepare(bodyView, player, BODY_NAME_PREFIX);
        prepare(ghost, player, GHOST_NAME_PREFIX);
        setFacing(bodyView, bodyYaw, player.getXRot(), bodyYaw, headYaw);
        setFacing(ghost, player.getYRot(), player.getXRot(), bodyYaw, headYaw);
        if (!level.addFreshEntity(bodyView)) {
            return false;
        }
        if (!level.addFreshEntity(ghost)) {
            bodyView.discard();
            return false;
        }
        Session session = new Session(player, level, bodyView, ghost, level.getGameTime() + DURATION_TICKS);
        SESSIONS.put(player.getUUID(), session);
        GoatHornCooldowns.begin(player, "dream", COOLDOWN_TICKS);
        PROXIES.put(bodyView, session);
        PROXIES.put(ghost, session);
        //? if mc != 1.21.1
        player.setLastClientInput(Input.EMPTY);
        //? endif
        player.xxa = 0.0F;
        player.yya = 0.0F;
        player.zza = 0.0F;
        player.stopUsingItem();
        return true;
    }

    /** The same per-horn timer that blocks another use of the Dream horn. */
    public static int cooldownRemainingTicks(ServerPlayer player) {
        return GoatHornCooldowns.remainingTicks(player, "dream");
    }

    private static void prepare(Entity avatar, ServerPlayer player, String prefix) {
        avatar.setPos(player.position());
        avatar.setYRot(player.getYRot());
        avatar.setXRot(player.getXRot());
        avatar.setNoGravity(true);
        avatar.setCustomName(net.minecraft.network.chat.Component.literal(prefix + player.getUUID()));
        avatar.setCustomNameVisible(false);
        avatar.setSilent(true);
    }

    private static void setFacing(Entity entity, float yaw, float pitch, float bodyYaw, float headYaw) {
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        entity.setXRot(pitch);
        entity.xRotO = pitch;
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = bodyYaw;
            living.yBodyRotO = bodyYaw;
            living.setYHeadRot(headYaw);
            living.yHeadRotO = headYaw;
        }
    }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.player != player || !player.isAlive() || player.level() != session.level) {
            finish(session, false);
            return;
        }
        // Server players normally receive their travel step from movement packets. While those
        // packets steer the ghost, advance the real body with no input so gravity and impulses work.
        player.travel(Vec3.ZERO);
        syncBodyView(session);
        session.ghost.setDeltaMovement(Vec3.ZERO);
        if (!session.ghost.isAlive() || !session.bodyView.isAlive()
                || session.level.getGameTime() >= session.endTick || !GoatHornPurposes.isEnabled("dream")) {
            finish(session, true);
            return;
        }
    }

    /** Follow the real body if the owner's visual copy ticks after the player. */
    public static void tickBodyView(LivingEntity entity) {
        Session session = PROXIES.get(entity);
        if (session != null && session.bodyView == entity) {
            syncBodyView(session);
        }
    }

    private static void syncBodyView(Session session) {
        ServerPlayer player = session.player;
        Entity bodyView = session.bodyView;
        bodyView.setPos(player.position());
        bodyView.setDeltaMovement(Vec3.ZERO);
        setFacing(bodyView, player.yBodyRot, player.getXRot(), player.yBodyRot, player.getYHeadRot());
    }

    /** Called on the server thread in place of vanilla player movement. */
    public static boolean moveGhost(ServerPlayer player, ServerboundMovePlayerPacket packet) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.player != player) {
            return false;
        }
        Entity ghost = session.ghost;
        double x = packet.getX(ghost.getX());
        double y = packet.getY(ghost.getY());
        double z = packet.getZ(ghost.getZ());
        if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && ghost.position().distanceToSqr(x, y, z) <= 100.0D
                && session.level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(x, y, z))) {
            ghost.setPos(x, y, z);
        }
        ghost.setYRot(packet.getYRot(ghost.getYRot()));
        ghost.setXRot(packet.getXRot(ghost.getXRot()));
        if (ghost instanceof LivingEntity living) {
            living.yBodyRot = ghost.getYRot();
            living.setYHeadRot(ghost.getYRot());
        }
        return true;
    }

    public static boolean isProjected(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.player == player;
    }

    /** The owner-only visual copy; other players see and attack the real player. */
    public static boolean isBody(Entity entity) {
        Session session = PROXIES.get(entity);
        return session != null && session.bodyView == entity;
    }

    public static boolean isGhost(Entity entity) {
        Session session = PROXIES.get(entity);
        return session != null && session.ghost == entity;
    }

    /** The server and client both recognize the decorative avatars by their private names. */
    public static boolean isVisualAvatar(Entity entity) {
        if (entity.getCustomName() == null) {
            return false;
        }
        String name = entity.getCustomName().getString();
        if (!name.startsWith(BODY_NAME_PREFIX) && !name.startsWith(GHOST_NAME_PREFIX)) {
            return false;
        }
        //? if mc == 1.21.1
        return entity instanceof ArmorStand;
        //? else
        return entity instanceof Mannequin;
        //? endif
    }

    public static boolean hurtBody(Entity body, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    public static void onLeave(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && session.player == player) {
            finish(session, false);
        }
    }

    public static boolean isOrphanedBody(Entity entity) {
        if (entity.level().isClientSide() || entity.getCustomName() == null || PROXIES.containsKey(entity)) {
            return false;
        }
        String name = entity.getCustomName().getString();
        if (!name.startsWith(BODY_NAME_PREFIX) && !name.startsWith(GHOST_NAME_PREFIX)) {
            return false;
        }
        //? if mc == 1.21.1
        return entity instanceof ArmorStand;
        //? else
        return entity instanceof Mannequin;
        //? endif
    }

    private static void finish(Session session, boolean returnToBody) {
        if (SESSIONS.remove(session.player.getUUID()) != session) {
            return;
        }
        PROXIES.remove(session.bodyView);
        PROXIES.remove(session.ghost);
        if (session.ghost.level() == session.level) {
            Vec3 at = session.ghost.position();
            session.level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.9D, at.z,
                    32, 0.35D, 0.7D, 0.35D, 0.06D);
        }
        session.bodyView.discard();
        session.ghost.discard();
        ServerPlayer player = session.player;
        if (returnToBody && player.isAlive() && player.level() == session.level) {
            Vec3 bodyPosition = player.position();
            player.connection.teleport(bodyPosition.x, bodyPosition.y, bodyPosition.z,
                    player.getYRot(), player.getXRot());
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            player.onUpdateAbilities();
        }
    }

    private static Entity createAvatar(ServerPlayer player, ServerLevel level, boolean equipment) {
        //? if mc == 1.21.1
        ArmorStand avatar = new ArmorStand(level, player.getX(), player.getY(), player.getZ());
        avatar.setShowArms(true);
        avatar.setNoBasePlate(true);
        ItemStack head = Items.PLAYER_HEAD.getDefaultInstance();
        head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
        avatar.setItemSlot(EquipmentSlot.HEAD, head);
        //? else
        //? if mc == 1.21.11 || mc == 26.1
        EntityType<?> mannequinType = EntityType.MANNEQUIN;
        //? else
        EntityType<?> mannequinType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "mannequin"));
        //? endif
        if (mannequinType == null) {
            return null;
        }
        Entity created = mannequinType.create(level, EntitySpawnReason.EVENT);
        if (!(created instanceof Mannequin avatar)) {
            return null;
        }
        ((MannequinProfileInvoker) avatar).bitsandbalance$setProfile(ResolvableProfile.createResolved(player.getGameProfile()));
        if (equipment) {
            avatar.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD).copy());
        }
        //? endif
        if (equipment) {
            avatar.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
            avatar.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
            avatar.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());
            avatar.setItemSlot(EquipmentSlot.MAINHAND, player.getItemBySlot(EquipmentSlot.MAINHAND).copy());
            avatar.setItemSlot(EquipmentSlot.OFFHAND, player.getItemBySlot(EquipmentSlot.OFFHAND).copy());
        }
        return avatar;
    }

    private record Session(ServerPlayer player, ServerLevel level, Entity bodyView, Entity ghost, long endTick) {
    }
}
