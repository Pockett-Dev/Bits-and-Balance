package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;

public final class GlowGooDispenserBehavior {
    private GlowGooDispenserBehavior() {
    }

    public static void register() {
        Item glowGoo = ModGlowGoo.glowGoo();
        DispenseItemBehavior defaultBehavior = new DefaultDispenseItemBehavior();
        DispenseItemBehavior projectileBehavior = new ProjectileDispenseBehavior(glowGoo);
        DispenserBlock.registerBehavior(glowGoo, (source, stack) -> GlowGooRuntime.enabled
                ? projectileBehavior.dispense(source, stack)
                : defaultBehavior.dispense(source, stack));
    }
}
