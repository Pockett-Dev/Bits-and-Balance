package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.player.RemotePlayer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Deprecated placeholder.
 *
 * RemotePlayer does not declare isCrouching() in 1.21.10, so the actual crouch suppression
 * for sitting players is implemented in {@link EntityCrouchingSittingMixin}.
 */
@Deprecated
@Mixin(RemotePlayer.class)
public abstract class RemotePlayerMixin {
}
