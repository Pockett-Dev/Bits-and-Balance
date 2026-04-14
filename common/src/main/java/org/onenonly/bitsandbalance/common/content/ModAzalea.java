package org.onenonly.bitsandbalance.common.content;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Optional;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.blocks.AzaleaButtonBlock;
import org.onenonly.bitsandbalance.common.blocks.AzaleaDoorBlock;
import org.onenonly.bitsandbalance.common.blocks.AzaleaPressurePlateBlock;
import org.onenonly.bitsandbalance.common.blocks.AzaleaStairBlock;
import org.onenonly.bitsandbalance.common.blocks.AzaleaTrapDoorBlock;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;
import org.onenonly.bitsandbalance.common.registry.ModWoodTypes;
import org.onenonly.bitsandbalance.common.entity.AzaleaBoat;
import org.onenonly.bitsandbalance.common.entity.AzaleaChestBoat;
import org.onenonly.bitsandbalance.common.item.AzaleaBoatItem;

/**
 * First "real" content chunk ported into :common.
 *
 * Note: Some items were placeholders during bring-up, but azalea signs and boats
 * are now fully wired on Fabric.
 */
public final class ModAzalea {
    public static final Identifier AZALEA_LOG_ID = id("azalea_log");
    public static final Identifier AZALEA_WOOD_ID = id("azalea_wood");
    public static final Identifier AZALEA_PLANKS_ID = id("azalea_planks");
    public static final Identifier STRIPPED_AZALEA_LOG_ID = id("stripped_azalea_log");
    public static final Identifier STRIPPED_AZALEA_WOOD_ID = id("stripped_azalea_wood");

    public static final Identifier AZALEA_SLAB_ID = id("azalea_slab");
    public static final Identifier AZALEA_STAIRS_ID = id("azalea_stairs");
    public static final Identifier AZALEA_BUTTON_ID = id("azalea_button");
    public static final Identifier AZALEA_PRESSURE_PLATE_ID = id("azalea_pressure_plate");
    public static final Identifier AZALEA_FENCE_ID = id("azalea_fence");
    public static final Identifier AZALEA_FENCE_GATE_ID = id("azalea_fence_gate");
    public static final Identifier AZALEA_SHELF_ID = id("azalea_shelf");

    public static final Identifier AZALEA_WALL_SIGN_ID = id("azalea_wall_sign");
    public static final Identifier AZALEA_WALL_HANGING_SIGN_ID = id("azalea_wall_hanging_sign");

    public static final Identifier AZALEA_DOOR_ID = id("azalea_door");
    public static final Identifier AZALEA_TRAPDOOR_ID = id("azalea_trapdoor");

    public static final Identifier AZALEA_SIGN_ID = id("azalea_sign");
    public static final Identifier AZALEA_HANGING_SIGN_ID = id("azalea_hanging_sign");
    public static final Identifier AZALEA_BOAT_ID = id("azalea_boat");
    public static final Identifier AZALEA_CHEST_BOAT_ID = id("azalea_chest_boat");

    public static final EntityType<AzaleaBoat> AZALEA_BOAT_ENTITY_TYPE = EntityType.Builder
        .<AzaleaBoat>of(AzaleaBoat::new, MobCategory.MISC)
        .sized(1.375F, 0.5625F)
        .eyeHeight(0.5625F)
        .clientTrackingRange(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, AZALEA_BOAT_ID));

    public static final EntityType<AzaleaChestBoat> AZALEA_CHEST_BOAT_ENTITY_TYPE = EntityType.Builder
        .<AzaleaChestBoat>of(AzaleaChestBoat::new, MobCategory.MISC)
        .sized(1.375F, 0.5625F)
        .eyeHeight(0.5625F)
        .clientTrackingRange(10)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, AZALEA_CHEST_BOAT_ID));

    public static final Block AZALEA_LOG = new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_LOG_ID))
        .mapColor(MapColor.COLOR_PINK)
    );

    public static final Block AZALEA_WOOD = new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_WOOD_ID))
        .mapColor(MapColor.COLOR_PINK)
    );

    public static final Block STRIPPED_AZALEA_LOG = new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)
        .setId(ResourceKey.create(Registries.BLOCK, STRIPPED_AZALEA_LOG_ID))
        .mapColor(MapColor.COLOR_PINK)
    );

    public static final Block STRIPPED_AZALEA_WOOD = new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)
        .setId(ResourceKey.create(Registries.BLOCK, STRIPPED_AZALEA_WOOD_ID))
        .mapColor(MapColor.COLOR_PINK)
    );

    public static final Block AZALEA_PLANKS = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_PLANKS_ID))
        .mapColor(MapColor.COLOR_PINK)
    );

    public static final Block AZALEA_SLAB = new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_SLAB_ID))
    );

    public static final Block AZALEA_STAIRS = new AzaleaStairBlock(
        AZALEA_PLANKS.defaultBlockState(),
        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)
            .setId(ResourceKey.create(Registries.BLOCK, AZALEA_STAIRS_ID))
    );

    public static final Block AZALEA_BUTTON = new AzaleaButtonBlock(
        ModWoodTypes.AZALEA_BLOCK_SET,
        30,
        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)
            .setId(ResourceKey.create(Registries.BLOCK, AZALEA_BUTTON_ID))
    );

    public static final Block AZALEA_PRESSURE_PLATE = new AzaleaPressurePlateBlock(
        ModWoodTypes.AZALEA_BLOCK_SET,
        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)
            .setId(ResourceKey.create(Registries.BLOCK, AZALEA_PRESSURE_PLATE_ID))
    );

    public static final Block AZALEA_FENCE = new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_FENCE_ID))
    );

    public static final Block AZALEA_FENCE_GATE = new FenceGateBlock(
        ModWoodTypes.AZALEA_WOOD_TYPE,
        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)
            .setId(ResourceKey.create(Registries.BLOCK, AZALEA_FENCE_GATE_ID))
    );

    public static final Block AZALEA_SHELF = new ShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_SHELF)
        .overrideLootTable(Optional.of(lootTableKey("blocks/azalea_shelf")))
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_SHELF_ID))
        .mapColor(MapColor.COLOR_PINK)
    );

    public static final Block AZALEA_DOOR = new AzaleaDoorBlock(ModWoodTypes.AZALEA_BLOCK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_DOOR_ID))
    );

    public static final Block AZALEA_TRAPDOOR = new AzaleaTrapDoorBlock(ModWoodTypes.AZALEA_BLOCK_SET, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_TRAPDOOR_ID))
    );

    public static final Block AZALEA_SIGN = new StandingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN)
        // ofFullCopy(oak sign) can carry a loot-table override pointing at oak.
        // Explicitly use our loot table so Survival drops work reliably.
        .overrideLootTable(Optional.of(lootTableKey("blocks/azalea_sign")))
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_SIGN_ID))
    );

    public static final Block AZALEA_WALL_SIGN = new WallSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN)
        .overrideLootTable(Optional.of(lootTableKey("blocks/azalea_wall_sign")))
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_WALL_SIGN_ID))
    );

    public static final Block AZALEA_HANGING_SIGN = new CeilingHangingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN)
        .overrideLootTable(Optional.of(lootTableKey("blocks/azalea_hanging_sign")))
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_HANGING_SIGN_ID))
    );

    public static final Block AZALEA_WALL_HANGING_SIGN = new WallHangingSignBlock(ModWoodTypes.AZALEA_WOOD_TYPE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN)
        .overrideLootTable(Optional.of(lootTableKey("blocks/azalea_wall_hanging_sign")))
        .setId(ResourceKey.create(Registries.BLOCK, AZALEA_WALL_HANGING_SIGN_ID))
    );

    private ModAzalea() {
    }

    public static void register(ContentRegistrar registrar) {
        registerBlockWithItem(registrar, AZALEA_LOG_ID, AZALEA_LOG);
        registerBlockWithItem(registrar, AZALEA_WOOD_ID, AZALEA_WOOD);
        registerBlockWithItem(registrar, AZALEA_PLANKS_ID, AZALEA_PLANKS);
        registerBlockWithItem(registrar, STRIPPED_AZALEA_LOG_ID, STRIPPED_AZALEA_LOG);
        registerBlockWithItem(registrar, STRIPPED_AZALEA_WOOD_ID, STRIPPED_AZALEA_WOOD);

        registerBlockWithItem(registrar, AZALEA_SLAB_ID, AZALEA_SLAB);
        registerBlockWithItem(registrar, AZALEA_STAIRS_ID, AZALEA_STAIRS);
        registerBlockWithItem(registrar, AZALEA_BUTTON_ID, AZALEA_BUTTON);
        registerBlockWithItem(registrar, AZALEA_PRESSURE_PLATE_ID, AZALEA_PRESSURE_PLATE);
        registerBlockWithItem(registrar, AZALEA_FENCE_ID, AZALEA_FENCE);
        registerBlockWithItem(registrar, AZALEA_FENCE_GATE_ID, AZALEA_FENCE_GATE);
        registerBlockWithItem(registrar, AZALEA_SHELF_ID, AZALEA_SHELF);

        // Signs (block + item)
        registrar.registerBlock(AZALEA_SIGN_ID, AZALEA_SIGN);
        registrar.registerBlock(AZALEA_WALL_SIGN_ID, AZALEA_WALL_SIGN);
        registrar.registerItem(AZALEA_SIGN_ID, new SignItem(AZALEA_SIGN, AZALEA_WALL_SIGN, itemProps(AZALEA_SIGN_ID).stacksTo(16)));

        // Hanging signs (block + item)
        registrar.registerBlock(AZALEA_HANGING_SIGN_ID, AZALEA_HANGING_SIGN);
        registrar.registerBlock(AZALEA_WALL_HANGING_SIGN_ID, AZALEA_WALL_HANGING_SIGN);
        registrar.registerItem(AZALEA_HANGING_SIGN_ID, new HangingSignItem(AZALEA_HANGING_SIGN, AZALEA_WALL_HANGING_SIGN, itemProps(AZALEA_HANGING_SIGN_ID).stacksTo(16)));

        registerBlockWithItem(registrar, AZALEA_DOOR_ID, AZALEA_DOOR);
        registerBlockWithItem(registrar, AZALEA_TRAPDOOR_ID, AZALEA_TRAPDOOR);

        // Boat entities + items
        registrar.registerEntityType(AZALEA_BOAT_ID, AZALEA_BOAT_ENTITY_TYPE);
        registrar.registerEntityType(AZALEA_CHEST_BOAT_ID, AZALEA_CHEST_BOAT_ENTITY_TYPE);

        registerItem(registrar, AZALEA_BOAT_ID, new AzaleaBoatItem(false, itemProps(AZALEA_BOAT_ID)));
        registerItem(registrar, AZALEA_CHEST_BOAT_ID, new AzaleaBoatItem(true, itemProps(AZALEA_CHEST_BOAT_ID)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }

    private static ResourceKey<LootTable> lootTableKey(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, id(path));
    }

    private static void registerBlockWithItem(ContentRegistrar registrar, Identifier id, Block block) {
        registrar.registerBlock(id, block);
        registrar.registerItem(id, new BlockItem(block, itemProps(id)));
    }

    private static void registerItem(ContentRegistrar registrar, Identifier id, Item item) {
        registrar.registerItem(id, item);
    }

    private static Item.Properties itemProps(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
}
