package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class EnhancedSlabCreativeTabFamilies {
    private EnhancedSlabCreativeTabFamilies() {
    }

    public static List<Family> snapshot(boolean includeVerticalSlabs, boolean includeSteps) {
        Map<Block, MutableFamily> families = new IdentityHashMap<>();

        if (includeVerticalSlabs) {
            for (Block verticalSlab : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
                Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(verticalSlab);
                if (sourceSlab == null) {
                    continue;
                }

                families.computeIfAbsent(sourceSlab, MutableFamily::new).verticalSlab = verticalSlab;
            }
        }

        if (includeSteps) {
            for (Block step : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
                Block sourceSlab = StepDynamicRegistry.getSlabForStep(step);
                if (sourceSlab == null) {
                    continue;
                }

                families.computeIfAbsent(sourceSlab, MutableFamily::new).step = step;
            }

            for (Block verticalStep : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
                Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(verticalStep);
                if (sourceVerticalSlab == null) {
                    continue;
                }

                Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
                if (sourceSlab == null) {
                    continue;
                }

                MutableFamily family = families.computeIfAbsent(sourceSlab, MutableFamily::new);
                family.verticalStep = verticalStep;
                if (family.verticalSlab == null) {
                    family.verticalSlab = sourceVerticalSlab;
                }
            }
        }

        if (families.isEmpty()) {
            return Collections.emptyList();
        }

        List<Family> snapshot = new ArrayList<>(families.size());
        for (MutableFamily family : families.values()) {
            if (family.verticalSlab == null && family.step == null && family.verticalStep == null) {
                continue;
            }

            snapshot.add(new Family(family.sourceSlab, family.verticalSlab, family.step, family.verticalStep));
        }

        snapshot.sort(Comparator.comparing(EnhancedSlabCreativeTabFamilies::bitsandbalance$keyForFamily));
        return Collections.unmodifiableList(snapshot);
    }

    private static String bitsandbalance$keyForFamily(Family family) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(family.sourceSlab());
        return id == null ? "" : id.toString();
    }

    public record Family(Block sourceSlab, Block verticalSlab, Block step, Block verticalStep) {
    }

    private static final class MutableFamily {
        private final Block sourceSlab;
        private Block verticalSlab;
        private Block step;
        private Block verticalStep;

        private MutableFamily(Block sourceSlab) {
            this.sourceSlab = sourceSlab;
        }
    }
}