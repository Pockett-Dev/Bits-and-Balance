package org.onenonly.bitsandbalance.fabric.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * NeoForge no-op bridge for Fabric's optional Axiom integration hooks.
 */
public final class FabricAxiomInfiniteReachCompat {
    private FabricAxiomInfiniteReachCompat() {
    }

    public static void maybeSendStepPlacementPacket(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
    }
}
