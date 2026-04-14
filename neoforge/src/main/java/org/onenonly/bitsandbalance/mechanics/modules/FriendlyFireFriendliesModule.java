package org.onenonly.bitsandbalance.mechanics.modules;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.FriendlyFireFriendlies;

/**
 * Friendly Fire Friendlies
 *
 * Prevents players from accidentally melee-attacking their own tamed pets.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class FriendlyFireFriendliesModule {
    private FriendlyFireFriendliesModule() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!Config.enableFriendlyFireFriendlies) return;
        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        var target = event.getTarget();

        if (FriendlyFireFriendlies.isOwnedTamedPet(player, target)) {
            event.setCanceled(true);
        }
    }
}
