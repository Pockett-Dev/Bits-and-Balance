package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public final class VerticalSlabRecipeInputs {
    private VerticalSlabRecipeInputs() {
    }

    public static @Nullable Item displayInputForSlab(Block slabBlock) {
        if (slabBlock == null) {
            return null;
        }

        Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
        if (slabId == null) {
            return null;
        }

        String namespace = slabId.getNamespace();
        String path = slabId.getPath();
        if (!path.endsWith("_slab")) {
            return null;
        }

        String basePath = path.substring(0, path.length() - "_slab".length());
        Set<Identifier> candidates = new LinkedHashSet<>();
        addCandidate(candidates, namespace, basePath + "_planks");
        if (basePath.endsWith("_brick")) {
            addCandidate(candidates, namespace, basePath.substring(0, basePath.length() - "_brick".length()) + "_bricks");
        }
        if (basePath.endsWith("_tile")) {
            addCandidate(candidates, namespace, basePath.substring(0, basePath.length() - "_tile".length()) + "_tiles");
        }
        addCandidate(candidates, namespace, basePath + "_block");
        addCandidate(candidates, namespace, basePath);

        if (!"minecraft".equals(namespace)) {
            addCandidate(candidates, "minecraft", basePath + "_planks");
            if (basePath.endsWith("_brick")) {
                addCandidate(candidates, "minecraft", basePath.substring(0, basePath.length() - "_brick".length()) + "_bricks");
            }
            if (basePath.endsWith("_tile")) {
                addCandidate(candidates, "minecraft", basePath.substring(0, basePath.length() - "_tile".length()) + "_tiles");
            }
            addCandidate(candidates, "minecraft", basePath + "_block");
            addCandidate(candidates, "minecraft", basePath);
        }

        for (Identifier candidate : candidates) {
            Item item = BuiltInRegistries.ITEM.getOptional(candidate).orElse(null);
            if (item != null && item != Items.AIR) {
                return item;
            }
        }

        return null;
    }

    private static void addCandidate(Set<Identifier> candidates, String namespace, String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        candidates.add(Identifier.fromNamespaceAndPath(namespace, path));
    }
}
