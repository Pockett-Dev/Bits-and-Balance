package org.onenonly.bitsandbalance.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

/**
 * Event handler for the Glowing Glowberries tweak.
 * Gives the Glowing effect to entities that consume glowberries.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class GlowingGlowberriesEventHandler {

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        // Check if the tweak is enabled
        if (!Config.enableGlowingGlowberries) {
            return;
        }

        // Get the entity and item stack
        LivingEntity entity = event.getEntity();
        ItemStack itemStack = event.getItem();

        // Check if the item is a glowberry
        if (itemStack.getItem() == Items.GLOW_BERRIES) {
            // Apply the Glowing effect
            int durationTicks = Config.glowingGlowberriesDuration * 20; // Convert seconds to ticks
            MobEffectInstance glowingEffect = new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0);
            entity.addEffect(glowingEffect);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        // Check if the tweak is enabled
        if (!Config.enableGlowingGlowberries) {
            return;
        }

        // Only check foxes
        if (!(event.getEntity() instanceof Fox fox)) {
            return;
        }

        // Check if the fox is holding a glowberry and doesn't already have the effect
        ItemStack heldItem = fox.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (heldItem.getItem() == Items.GLOW_BERRIES && heldItem.getCount() > 0) {
            // Check if the fox doesn't already have the glowing effect
            if (!fox.hasEffect(MobEffects.GLOWING)) {
                // Apply the Glowing effect with fox-specific duration
                int durationTicks = Config.foxGlowingDuration * 20; // Convert seconds to ticks
                MobEffectInstance glowingEffect = new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0);
                fox.addEffect(glowingEffect);
            }
        }
    }
}
