package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Custom entity attributes for Bits and Balance.
 */
public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = 
            DeferredRegister.create(Registries.ATTRIBUTE, BitsAndBalance.MODID);

    /**
     * Aerodynamic drag attribute - reduces horizontal air resistance during elytra flight.
     * Base value is 0.0 (no reduction). Enchantment applies +0.5 (50% drag reduction).
     */
    public static final DeferredHolder<Attribute, Attribute> AERODYNAMIC_DRAG = ATTRIBUTES.register(
            "aerodynamic_drag",
            () -> new RangedAttribute(
                    "attribute.name.bitsandbalance.aerodynamic_drag",
                    0.0D,  // default value (no drag reduction)
                    0.0D,  // min value (no drag reduction)
                    1.0D   // max value (100% drag reduction, no drag)
            ).setSyncable(true)
    );

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }
}
