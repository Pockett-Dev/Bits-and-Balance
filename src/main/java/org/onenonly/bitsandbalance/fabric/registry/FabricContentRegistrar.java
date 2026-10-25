package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

public final class FabricContentRegistrar implements ContentRegistrar {
    @Override
    //? if mc == 1.21.1
    public void registerBlock(ResourceLocation id, Block block) {
    //? else
    public void registerBlock(Identifier id, Block block) {
    //? endif
        Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    @Override
    //? if mc == 1.21.1
    public void registerItem(ResourceLocation id, Item item) {
    //? else
    public void registerItem(Identifier id, Item item) {
    //? endif
        Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    @Override
    //? if mc == 1.21.1
    public void registerEntityType(ResourceLocation id, EntityType<? extends Entity> entityType) {
    //? else
    public void registerEntityType(Identifier id, EntityType<? extends Entity> entityType) {
    //? endif
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, entityType);
    }
}
