package org.onenonly.bitsandbalance.mobs;

import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

/**
 * Iron Golems kill Creepers mechanic:
 * - Iron Golems will target Creepers (implemented via mixin)
 * - Creepers will not retaliate / swap target to Iron Golems (prevents swelling/explosions)
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class IronGolemCreeperHandler {
    private IronGolemCreeperHandler() {}

    @SubscribeEvent
    public static void onCreeperTargetChange(LivingChangeTargetEvent event) {
        if (!Config.enableIronGolemsKillCreepers) return;

        if (event.getEntity().level().isClientSide()) return;

        if (!(event.getEntity() instanceof Creeper creeper)) return;

        if (event.getNewAboutToBeSetTarget() instanceof IronGolem) {
            // Prevent creepers from ever targeting golems (including when hurt), which prevents swelling.
            creeper.setSwellDir(-1);
            event.setCanceled(true);
        }
    }
}
