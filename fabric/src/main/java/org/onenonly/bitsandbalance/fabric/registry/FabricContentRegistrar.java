package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

public final class FabricContentRegistrar implements ContentRegistrar {
    @Override
    public void registerBlock(Identifier id, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    @Override
    public void registerItem(Identifier id, Item item) {
        Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    @Override
    public void registerEntityType(Identifier id, EntityType<? extends Entity> entityType) {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, entityType);
    }
}
