package org.onenonly.bitsandbalance.common.content;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
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
        //? if mc == 1.21.1
        Block block = BuiltInRegistries.BLOCK.get(GOO_SPLATTER_ID);
        return block != null ? block : Blocks.AIR;
        //? else
        return BuiltInRegistries.BLOCK.get(GOO_SPLATTER_ID).map(holder -> holder.value()).orElse(Blocks.AIR);
        //? endif
    }

    public static Item glowGoo() {
        //? if mc == 1.21.1
        Item item = BuiltInRegistries.ITEM.get(GLOW_GOO_ID);
        return item != null ? item : Items.SLIME_BALL;
        //? else
        return BuiltInRegistries.ITEM.get(GLOW_GOO_ID).map(holder -> holder.value()).orElse(Items.SLIME_BALL);
        //? endif
    }

    public static Holder<MobEffect> bioluminescence() {
        //? if mc == 1.21.1
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(BIOLUMINESCENCE_ID);
        if (effect == null) {
            throw new IllegalStateException("Bioluminescence effect is not registered");
        }
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        //? else
        return BuiltInRegistries.MOB_EFFECT.get(BIOLUMINESCENCE_ID)
                .orElseThrow(() -> new IllegalStateException("Bioluminescence effect is not registered"));
        //? endif
    }

    @SuppressWarnings("unchecked")
    public static EntityType<GlowGooProjectile> glowGooProjectileType() {
        //? if mc == 1.21.1
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(GLOW_GOO_PROJECTILE_ID);
        if (entityType == null) {
            throw new IllegalStateException("Glow Goo projectile type is not registered");
        }
        return (EntityType<GlowGooProjectile>) entityType;
        //? else
        return (EntityType<GlowGooProjectile>) BuiltInRegistries.ENTITY_TYPE.get(GLOW_GOO_PROJECTILE_ID)
                .map(holder -> holder.value())
                .orElseThrow(() -> new IllegalStateException("Glow Goo projectile type is not registered"));
        //? endif
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }

    private static Block createGooSplatterBlock() {
        return new GooSplatterBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_WIRE)
                        //? if mc != 1.21.1
                        .setId(ResourceKey.create(Registries.BLOCK, GOO_SPLATTER_ID))
                        //? endif
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
                //? if mc == 1.21.1
                .build(GLOW_GOO_PROJECTILE_ID.toString());
                //? else
                .build(ResourceKey.create(Registries.ENTITY_TYPE, GLOW_GOO_PROJECTILE_ID));
                //? endif
    }

    private static Item.Properties itemProps(Identifier id) {
        return new Item.Properties()
                //? if mc != 1.21.1
                .setId(ResourceKey.create(Registries.ITEM, id))
                //? endif
                ;
    }
}
