package org.onenonly.bitsandbalance.fabric.registry;

import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantFamilyTagInjector;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.StepToolTagInjector;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabToolTagInjector;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

/**
 * Registers the Fabric-side tag-loaded callback that reapplies tag-dependent behavior for
 * dynamically-registered slab-family blocks after every tag reload.
 *
 * <p>{@link CommonLifecycleEvents#TAGS_LOADED} fires on both client and server after every tag
 * reload, matching the NeoForge {@code TagsUpdatedEvent} semantics.</p>
 */
public final class FabricTagInjection {

    private FabricTagInjection() {
    }

    public static void init() {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            DynamicVariantFamilyTagInjector.injectFamilyTags();
            VerticalSlabToolTagInjector.injectToolTags();
            StepToolTagInjector.injectToolTags();
            registerDynamicVariantFlammability();
        });
    }

    private static void registerDynamicVariantFlammability() {
        FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();

        for (Block verticalBlock : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
            if (verticalBlock instanceof FixedVerticalSlabBlock fixedVerticalBlock) {
                registerDerivedFlammability(flammable, verticalBlock, fixedVerticalBlock.getSourceSlab());
            }
        }

        for (Block stepBlock : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
            if (stepBlock instanceof FixedStepBlock fixedStepBlock) {
                registerDerivedFlammability(flammable, stepBlock, fixedStepBlock.getSourceSlab());
            }
        }

        for (Block verticalStepBlock : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
            if (verticalStepBlock instanceof FixedVerticalStepBlock fixedVerticalStepBlock) {
                Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(fixedVerticalStepBlock.getSourceVerticalSlab());
                if (sourceSlab != null) {
                    registerDerivedFlammability(flammable, verticalStepBlock, sourceSlab);
                }
            }
        }
    }

    private static void registerDerivedFlammability(FlammableBlockRegistry flammable, Block targetBlock, Block sourceSlab) {
        int igniteOdds = DynamicVariantMaterialHelper.getDerivedIgniteOdds(sourceSlab);
        int burnOdds = DynamicVariantMaterialHelper.getDerivedBurnOdds(sourceSlab);
        if (igniteOdds <= 0 && burnOdds <= 0) {
            return;
        }

        flammable.add(targetBlock, igniteOdds, burnOdds);
    }
}
