package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Minimal cross-loader registration surface.
 *
 * Loader modules implement this using their registry systems.
 */
public interface ContentRegistrar {
    void registerBlock(Identifier id, Block block);

    default void registerBlock(Identifier id, Supplier<? extends Block> factory) {
        registerBlock(id, factory.get());
    }

    void registerItem(Identifier id, Item item);

    default void registerItem(Identifier id, Supplier<? extends Item> factory) {
        registerItem(id, factory.get());
    }

    void registerEntityType(Identifier id, EntityType<? extends Entity> entityType);

    default void registerEntityType(Identifier id, Supplier<? extends EntityType<? extends Entity>> factory) {
        registerEntityType(id, factory.get());
    }
}
