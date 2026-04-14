package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.LinkedHashSet;
import java.util.Set;

public final class SlabFamilyBlockLookup {
    private static final Set<String> SPECIAL_PLURAL_SUFFIXES = Set.of("brick", "tile");

    private SlabFamilyBlockLookup() {
    }

    public static @Nullable Block getBaseBlockForSlab(@Nullable Block slab) {
        try {
            if (slab == null) return null;
            Identifier id = BuiltInRegistries.BLOCK.getKey(slab);
            if (id == null) return null;

            String path = id.getPath();
            if (!path.endsWith("_slab")) return null;

            String namespace = id.getNamespace();
            String basePath = path.substring(0, path.length() - "_slab".length());

            LinkedHashSet<Identifier> candidates = new LinkedHashSet<>();

            if (slab.builtInRegistryHolder().is(BlockTags.WOODEN_SLABS)) {
                bitsandbalance$addCandidate(candidates, namespace, basePath + "_planks");
            }

            bitsandbalance$addCandidate(candidates, namespace, basePath);

            String specialBasePath = bitsandbalance$getSpecialBasePath(basePath);
            if (specialBasePath != null) {
                bitsandbalance$addCandidate(candidates, namespace, specialBasePath);
            }

            String pluralBasePath = bitsandbalance$getPluralBasePath(basePath);
            if (pluralBasePath != null) {
                bitsandbalance$addCandidate(candidates, namespace, pluralBasePath);
            }

            for (Identifier candidate : candidates) {
                if (!BuiltInRegistries.BLOCK.containsKey(candidate)) continue;
                return BuiltInRegistries.BLOCK.getValue(candidate);
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static @Nullable Block getBaseBlockForVerticalSlab(@Nullable Block verticalSlab) {
        Block slab = EnhancedSlabRecipeLookup.slabForVertical(verticalSlab);
        return getBaseBlockForSlab(slab);
    }

    public static @Nullable Block getBaseBlockForStep(@Nullable Block step) {
        Block slab = EnhancedSlabRecipeLookup.slabForStep(step);
        return getBaseBlockForSlab(slab);
    }

    public static @Nullable Block getBaseBlockForVerticalStep(@Nullable Block verticalStep) {
        Block verticalSlab = EnhancedSlabRecipeLookup.verticalSlabForVerticalStep(verticalStep);
        return getBaseBlockForVerticalSlab(verticalSlab);
    }

    private static void bitsandbalance$addCandidate(Set<Identifier> candidates, String namespace, String path) {
        if (path == null || path.isEmpty()) return;
        candidates.add(Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static @Nullable String bitsandbalance$getSpecialBasePath(String basePath) {
        return switch (basePath) {
            case "bamboo" -> "bamboo_planks";
            case "quartz" -> "quartz_block";
            case "purpur" -> "purpur_block";
            default -> null;
        };
    }

    private static @Nullable String bitsandbalance$getPluralBasePath(String basePath) {
        if (basePath == null || basePath.isEmpty()) return null;

        for (String suffix : SPECIAL_PLURAL_SUFFIXES) {
            if (basePath.equals(suffix) || basePath.endsWith("_" + suffix)) {
                return basePath + "s";
            }
        }

        return null;
    }
}