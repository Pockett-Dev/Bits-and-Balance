package org.onenonly.bitsandbalance.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Small bridge so we can register loader-agnostic common content on NeoForge using DeferredRegister.
 */
public final class NeoForgeContentRegistrar implements ContentRegistrar {

    private final DeferredRegister.Blocks blocks;
    private final DeferredRegister.Items items;
    private final DeferredRegister<EntityType<?>> entityTypes;

    public NeoForgeContentRegistrar(DeferredRegister.Blocks blocks, DeferredRegister.Items items, DeferredRegister<EntityType<?>> entityTypes) {
        this.blocks = Objects.requireNonNull(blocks, "blocks");
        this.items = Objects.requireNonNull(items, "items");
        this.entityTypes = Objects.requireNonNull(entityTypes, "entityTypes");
    }

    @Override
    public void registerBlock(ResourceLocation id, Block block) {
        blocks.register(id.getPath(), () -> block);
    }

    @Override
    public void registerBlock(ResourceLocation id, Supplier<? extends Block> factory) {
        blocks.register(id.getPath(), factory);
    }

    @Override
    public void registerItem(ResourceLocation id, Item item) {
        items.register(id.getPath(), () -> item);
    }

    @Override
    public void registerItem(ResourceLocation id, Supplier<? extends Item> factory) {
        items.register(id.getPath(), factory);
    }

    @Override
    public void registerEntityType(ResourceLocation id, EntityType<? extends Entity> entityType) {
        entityTypes.register(id.getPath(), () -> entityType);
    }

    @Override
    public void registerEntityType(ResourceLocation id, Supplier<? extends EntityType<? extends Entity>> factory) {
        entityTypes.register(id.getPath(), factory);
    }
}
