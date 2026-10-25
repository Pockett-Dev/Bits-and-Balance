package org.onenonly.bitsandbalance.mixin;

import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
//? if mc == 1.21.1
import net.minecraft.world.entity.RelativeMovement;
//? else
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
//? endif
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerDreamMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void bitsandbalance$endDreamOnLeave(DisconnectionDetails details, CallbackInfo ci) {
        ServerLevel level = bitsandbalance$serverLevel();
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> DreamProjection.onLeave(player));
        } else {
            DreamProjection.onLeave(player);
        }
    }

    @Inject(method = "teleport(DDDFF)V", at = @At("HEAD"))
    private void bitsandbalance$endDreamBeforeBodyTeleport(double x, double y, double z,
                                                             float yaw, float pitch, CallbackInfo ci) {
        DreamProjection.onLeave(player);
    }

    //? if mc == 1.21.1
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void bitsandbalance$endDreamBeforeRelativeTeleport(double x, double y, double z,
                                                                 float yaw, float pitch,
                                                                 Set<RelativeMovement> relatives, CallbackInfo ci) {
    //? else
    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
    private void bitsandbalance$endDreamBeforeRelativeTeleport(PositionMoveRotation destination,
                                                                 Set<Relative> relatives, CallbackInfo ci) {
    //? endif
        DreamProjection.onLeave(player);
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockGhostActions(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (bitsandbalance$serverLevel().getServer().isSameThread() && DreamProjection.isProjected(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAbilities", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$keepBodyFlightUnchanged(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        if (bitsandbalance$serverLevel().getServer().isSameThread() && DreamProjection.isProjected(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$keepGhostInputOffBody(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        if (bitsandbalance$serverLevel().getServer().isSameThread() && DreamProjection.isProjected(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$moveDreamGhost(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (bitsandbalance$serverLevel().getServer().isSameThread()
                && DreamProjection.moveGhost(player, packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockGhostInventory(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (bitsandbalance$serverLevel().getServer().isSameThread() && DreamProjection.isProjected(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$blockGhostEntityInteraction(ServerboundInteractPacket packet, CallbackInfo ci) {
        ServerLevel level = bitsandbalance$serverLevel();
        if (!level.getServer().isSameThread()) {
            return;
        }
        //? if mc == 1.21.1 || mc == 1.21.11
        Entity target = packet.getTarget(level);
        //? else
        Entity target = level.getEntity(packet.entityId());
        //? endif
        if (DreamProjection.isProjected(player)
                || (DreamProjection.isBody(target) && !bitsandbalance$isAttack(packet))) {
            ci.cancel();
        }
    }

    private static boolean bitsandbalance$isAttack(ServerboundInteractPacket packet) {
        //? if mc == 1.21.1 || mc == 1.21.11
        boolean[] attack = {false};
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(net.minecraft.world.InteractionHand hand) {
            }

            @Override
            public void onInteraction(net.minecraft.world.InteractionHand hand,
                                      net.minecraft.world.phys.Vec3 location) {
            }

            @Override
            public void onAttack() {
                attack[0] = true;
            }
        });
        return attack[0];
        //? else
        return false;
        //? endif
    }

    private ServerLevel bitsandbalance$serverLevel() {
        //? if mc == 1.21.1
        return player.serverLevel();
        //? else
        return player.level();
        //? endif
    }
}
