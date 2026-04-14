package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
        List<Identifier> candidates = new ArrayList<>();
        candidates.add(Identifier.fromNamespaceAndPath(namespace, basePath + "_planks"));
        if (basePath.endsWith("_brick")) {
            candidates.add(Identifier.fromNamespaceAndPath(namespace, basePath.substring(0, basePath.length() - "_brick".length()) + "_bricks"));
        }
        candidates.add(Identifier.fromNamespaceAndPath(namespace, basePath + "_block"));
        candidates.add(Identifier.fromNamespaceAndPath(namespace, basePath));

        for (Identifier candidate : candidates) {
            Item item = BuiltInRegistries.ITEM.getOptional(candidate).orElse(null);
            if (item != null && item != Items.AIR) {
                return item;
            }
        }

        return null;
    }
}