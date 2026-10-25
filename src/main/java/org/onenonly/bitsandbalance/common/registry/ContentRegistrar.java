package org.onenonly.bitsandbalance.common.registry;

//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
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
    //? if mc == 1.21.1
    void registerBlock(ResourceLocation id, Block block);

    default void registerBlock(ResourceLocation id, Supplier<? extends Block> factory) {
    //? else
    void registerBlock(Identifier id, Block block);

    default void registerBlock(Identifier id, Supplier<? extends Block> factory) {
    //? endif
        registerBlock(id, factory.get());
    }

    //? if mc == 1.21.1
    void registerItem(ResourceLocation id, Item item);

    default void registerItem(ResourceLocation id, Supplier<? extends Item> factory) {
    //? else
    void registerItem(Identifier id, Item item);

    default void registerItem(Identifier id, Supplier<? extends Item> factory) {
    //? endif
        registerItem(id, factory.get());
    }

    //? if mc == 1.21.1
    void registerEntityType(ResourceLocation id, EntityType<? extends Entity> entityType);

    default void registerEntityType(ResourceLocation id, Supplier<? extends EntityType<? extends Entity>> factory) {
    //? else
    void registerEntityType(Identifier id, EntityType<? extends Entity> entityType);

    default void registerEntityType(Identifier id, Supplier<? extends EntityType<? extends Entity>> factory) {
    //? endif
        registerEntityType(id, factory.get());
    }
}
