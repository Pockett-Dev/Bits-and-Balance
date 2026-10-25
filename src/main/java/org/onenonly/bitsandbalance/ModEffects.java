package org.onenonly.bitsandbalance;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.common.effects.BioluminescenceEffect;
import org.onenonly.bitsandbalance.effects.ResurfacingEffect;
import org.onenonly.bitsandbalance.effects.DisplacementEffect;
import org.onenonly.bitsandbalance.effects.ReturningEffect;

/**
 * Registers custom mob effects for the Bits and Balance mod.
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, BitsAndBalance.MODID);

    // Resurfacing effect - teleports player to surface like Instant Health
    public static final DeferredHolder<MobEffect, MobEffect> RESURFACING = EFFECTS.register("resurfacing",
            () -> new ResurfacingEffect());

    // Displacement effect - randomly teleports player within 100-block radius
    public static final DeferredHolder<MobEffect, MobEffect> DISPLACEMENT = EFFECTS.register("displacement",
            () -> new DisplacementEffect());

    // Returning effect - teleports player to their latest death location
    public static final DeferredHolder<MobEffect, MobEffect> RETURNING = EFFECTS.register("returning",
            () -> new ReturningEffect());

        public static final DeferredHolder<MobEffect, MobEffect> BIOLUMINESCENCE = EFFECTS.register("bioluminescence",
                        BioluminescenceEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
