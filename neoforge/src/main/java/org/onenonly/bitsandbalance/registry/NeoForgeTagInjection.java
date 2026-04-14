package org.onenonly.bitsandbalance.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantFamilyTagInjector;
import org.onenonly.bitsandbalance.common.registry.StepToolTagInjector;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabToolTagInjector;

/**
 * Injects tool-related block tags into dynamically-registered vertical-slab blocks after tags
 * are loaded or reloaded, so that systems like Jade display the correct tool in the HUD.
 *
 * <p>{@link TagsUpdatedEvent} fires on the GAME event bus every time the tag manager rebinds
 * holders (including on {@code /reload}), which is exactly when we need to re-inject because
 * {@link net.minecraft.core.Holder.Reference#bindTags} replaces the tag set from scratch.</p>
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class NeoForgeTagInjection {

    private NeoForgeTagInjection() {
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        DynamicVariantFamilyTagInjector.injectFamilyTags();
        VerticalSlabToolTagInjector.injectToolTags();
        StepToolTagInjector.injectToolTags();
    }
}
