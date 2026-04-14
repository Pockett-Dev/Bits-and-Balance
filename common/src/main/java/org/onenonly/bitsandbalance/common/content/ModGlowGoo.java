package org.onenonly.bitsandbalance.common.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.GooSplatterBlock;
import org.onenonly.bitsandbalance.common.entity.GlowGooProjectile;
import org.onenonly.bitsandbalance.common.items.GlowGooItem;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

public final class ModGlowGoo {
    public static final Identifier GOO_SPLATTER_ID = id("goo_splatter");
    public static final Identifier GLOW_GOO_ID = id("glow_goo");
    public static final Identifier GLOW_GOO_PROJECTILE_ID = id("glow_goo_projectile");
    public static final Identifier BIOLUMINESCENCE_ID = id("bioluminescence");

    private ModGlowGoo() {
    }

    public static void register(ContentRegistrar registrar) {
        registrar.registerBlock(GOO_SPLATTER_ID, ModGlowGoo::createGooSplatterBlock);
        registrar.registerItem(GLOW_GOO_ID, () -> new GlowGooItem(itemProps(GLOW_GOO_ID).stacksTo(16)));
        registrar.registerEntityType(GLOW_GOO_PROJECTILE_ID, ModGlowGoo::createGlowGooProjectileType);
    }

    public static Block gooSplatter() {
        return BuiltInRegistries.BLOCK.get(GOO_SPLATTER_ID).map(holder -> holder.value()).orElse(Blocks.AIR);
    }

    public static Item glowGoo() {
        return BuiltInRegistries.ITEM.get(GLOW_GOO_ID).map(holder -> holder.value()).orElse(Items.SLIME_BALL);
    }

    public static Holder<MobEffect> bioluminescence() {
        return BuiltInRegistries.MOB_EFFECT.get(BIOLUMINESCENCE_ID)
                .orElseThrow(() -> new IllegalStateException("Bioluminescence effect is not registered"));
    }

    @SuppressWarnings("unchecked")
    public static EntityType<GlowGooProjectile> glowGooProjectileType() {
        return (EntityType<GlowGooProjectile>) BuiltInRegistries.ENTITY_TYPE.get(GLOW_GOO_PROJECTILE_ID)
                .map(holder -> holder.value())
                .orElseThrow(() -> new IllegalStateException("Glow Goo projectile type is not registered"));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }

    private static Block createGooSplatterBlock() {
        return new GooSplatterBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_WIRE)
                        .setId(ResourceKey.create(Registries.BLOCK, GOO_SPLATTER_ID))
                        .strength(0.1F)
                        .lightLevel(state -> 15)
                        .noOcclusion()
                        .sound(SoundType.SLIME_BLOCK)
        );
    }

    private static EntityType<GlowGooProjectile> createGlowGooProjectileType() {
        return EntityType.Builder.<GlowGooProjectile>of(GlowGooProjectile::new, MobCategory.MISC)
                .sized(0.28F, 0.28F)
                .clientTrackingRange(4)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, GLOW_GOO_PROJECTILE_ID));
    }

    private static Item.Properties itemProps(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
}