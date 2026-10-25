//? if mc == 1.21.1
package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** Render the 1.21.1 stand avatar with the actual player's skin and model. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererDreamAvatarMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$renderDreamAvatar(LivingEntity entity, float yaw, float partialTick,
                                                   PoseStack pose, MultiBufferSource buffers, int light,
                                                   CallbackInfo ci) {
        if (!(entity instanceof ArmorStand stand)) {
            return;
        }
        boolean ghost = DreamProjectionClient.isGhost(stand);
        UUID ownerId = DreamProjectionClient.owner(stand,
                ghost ? DreamProjection.GHOST_NAME_PREFIX : DreamProjection.BODY_NAME_PREFIX);
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (ownerId == null || level == null) {
            return;
        }
        Player owner = level.getPlayerByUUID(ownerId);
        if (owner == null) {
            return;
        }
        RemotePlayer avatar = new RemotePlayer(level, owner.getGameProfile());
        avatar.setPos(stand.position());
        avatar.setYRot(stand.getYRot());
        avatar.setXRot(stand.getXRot());
        avatar.yRotO = stand.yRotO;
        avatar.xRotO = stand.xRotO;
        avatar.yBodyRot = stand.yBodyRot;
        avatar.yBodyRotO = stand.yBodyRotO;
        avatar.yHeadRot = stand.getYHeadRot();
        avatar.yHeadRotO = stand.yHeadRotO;
        avatar.setInvisible(ghost);
        if (!ghost) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                avatar.setItemSlot(slot, owner.getItemBySlot(slot).copy());
            }
        }
        DreamProjectionClient.renderingGhostAvatar(ghost, stand.tickCount);
        try {
            minecraft.getEntityRenderDispatcher().render(avatar, 0.0D, 0.0D, 0.0D,
                    yaw, partialTick, pose, buffers, light);
        } finally {
            DreamProjectionClient.renderingGhostAvatar(false, 0);
        }
        ci.cancel();
    }
}
//? endif
