package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.onenonly.bitsandbalance.common.mechanics.FriendlyFireFriendlies;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;

/**
 * Fabric implementation of "Friendly Fire Friendlies".
 */
public final class FabricFriendlyFireFriendlies {
    private FabricFriendlyFireFriendlies() {
    }

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!FabricMechanicsConfig.enableFriendlyFireFriendlies) return InteractionResult.PASS;
            if (world.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            return FriendlyFireFriendlies.isOwnedTamedPet(player, entity)
                    ? InteractionResult.FAIL
                    : InteractionResult.PASS;
        });
    }
}
