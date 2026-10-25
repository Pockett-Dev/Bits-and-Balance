package org.onenonly.bitsandbalance.common.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.KilnBlock;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

public final class ModKiln {
    //? if mc == 1.21.1
    public static final ResourceLocation KILN_ID = id("kiln");
    public static final ResourceLocation KILN_SMELTABLES_ID = id("kiln_smeltables");
    //? else
    public static final Identifier KILN_ID = id("kiln");
    public static final Identifier KILN_SMELTABLES_ID = id("kiln_smeltables");
    //? endif
    public static final TagKey<Item> KILN_SMELTABLES = TagKey.create(Registries.ITEM, KILN_SMELTABLES_ID);

    private ModKiln() {
    }

    public static void register(ContentRegistrar registrar) {
        registrar.registerBlock(KILN_ID, ModKiln::createKilnBlock);
        registrar.registerItem(KILN_ID, () -> new BlockItem(kiln(), itemProperties()));
    }

    public static Block kiln() {
        //? if mc == 1.21.1
        Block block = BuiltInRegistries.BLOCK.get(KILN_ID);
        return block != null ? block : Blocks.AIR;
        //? else
        return BuiltInRegistries.BLOCK.get(KILN_ID).map(holder -> holder.value()).orElse(Blocks.AIR);
        //? endif
    }

    public static boolean isKiln(BlockState state) {
        return state != null && state.getBlock() instanceof KilnBlock;
    }

    private static Block createKilnBlock() {
        return new KilnBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)
                        //? if mc != 1.21.1
                        .setId(ResourceKey.create(Registries.BLOCK, KILN_ID))
                        //? endif
                        .strength(3.5F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE)
        );
    }

    private static Item.Properties itemProperties() {
        return new Item.Properties()
                //? if mc != 1.21.1
                .setId(ResourceKey.create(Registries.ITEM, KILN_ID))
                //? endif
                ;
    }

    //? if mc == 1.21.1
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }
    //? else
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }
    //? endif
}
