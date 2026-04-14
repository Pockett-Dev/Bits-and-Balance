package org.onenonly.bitsandbalance.setup;

import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.ModBlocks;
import org.onenonly.bitsandbalance.ModItems;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.entity.AzaleaBoat;
import org.onenonly.bitsandbalance.entity.AzaleaChestBoat;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

/**
 * Common setup for both client and server.
 * Handles dispenser behavior, flammability, and fuel registration.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class CommonSetup {
    
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Only register azalea features if woodset is enabled
            if (org.onenonly.bitsandbalance.Config.enableAzaleaWoodset) {
                // Register dispenser behavior for azalea boats
                registerBoatDispenserBehavior();
                
                // Register flammability for azalea wood blocks
                registerFlammability();

                // Add azalea hanging sign blocks to the vanilla block entity type (prevents crash).
                registerHangingSignBlocks();
                registerShelfBlocks();
                
                BitsAndBalance.LOGGER.info("Common setup complete for Azalea wood");
            } else {
                BitsAndBalance.LOGGER.info("Azalea woodset disabled, skipping setup");
            }

            // Enhanced slabs: ensure the vertical slab block entity type accepts all dynamically
            // registered per-slab enhanced slab blocks.
            registerDynamicVerticalSlabBlocks();
            registerDynamicStepBlocks();
            registerDynamicVerticalStepBlocks();
            registerDynamicVariantFlammability();
        });
        
        // Register fuel values (must be done on the event bus)
        NeoForge.EVENT_BUS.addListener(CommonSetup::onFurnaceFuelBurnTime);
    }
    
    /**
     * Registers dispenser behavior for azalea boats.
     * When a boat item is dispensed, it spawns a boat in water or drops the item otherwise.
     */
    private static void registerBoatDispenserBehavior() {
        DispenseItemBehavior boatBehavior = new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultBehavior = new DefaultDispenseItemBehavior();
            
            @Override
            public ItemStack execute(net.minecraft.core.dispenser.BlockSource source, ItemStack stack) {
                Direction direction = source.state().getValue(DispenserBlock.FACING);
                Level level = source.level();
                double x = source.pos().getX() + direction.getStepX() * 1.125;
                double y = source.pos().getY() + direction.getStepY() * 1.125;
                double z = source.pos().getZ() + direction.getStepZ() * 1.125;
                BlockPos blockPos = source.pos().relative(direction);
                double yOffset;
                
                if (level.getFluidState(blockPos).is(FluidTags.WATER)) {
                    yOffset = 1.0;
                } else {
                    if (!level.getBlockState(blockPos).isAir() || !level.getFluidState(blockPos.below()).is(FluidTags.WATER)) {
                        return this.defaultBehavior.dispense(source, stack);
                    }
                    yOffset = 0.0;
                }
                
                net.minecraft.world.entity.Entity boat = stack.getItem() == ModItems.AZALEA_CHEST_BOAT.get()
                    ? new AzaleaChestBoat(level, x, y + yOffset, z)
                    : new AzaleaBoat(level, x, y + yOffset, z);
                
                // Don't set variant - custom renderer handles texture
                boat.setYRot(direction.toYRot());
                level.addFreshEntity(boat);
                stack.shrink(1);
                return stack;
            }
        };
        
        DispenserBlock.registerBehavior(ModItems.AZALEA_BOAT.get(), boatBehavior);
        DispenserBlock.registerBehavior(ModItems.AZALEA_CHEST_BOAT.get(), boatBehavior);
        
        BitsAndBalance.LOGGER.info("Registered dispenser behavior for Azalea boats");
    }
    
    /**
     * Handles axe stripping for azalea logs and wood.
     * This event is fired when a player uses an axe on a block.
     */
    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        // Skip if azalea woodset is disabled
        if (!org.onenonly.bitsandbalance.Config.enableAzaleaWoodset) {
            return;
        }
        
        // Check if the tool action is stripping (axe right-click)
        if (!event.getItemAbility().equals(ItemAbilities.AXE_STRIP)) {
            return;
        }
        
        // Check if the block is an azalea log or wood
        if (event.getState().is(ModBlocks.AZALEA_LOG.get())) {
            event.setFinalState(ModBlocks.STRIPPED_AZALEA_LOG.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, 
                            event.getState().getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)));
        } else if (event.getState().is(ModBlocks.AZALEA_WOOD.get())) {
            event.setFinalState(ModBlocks.STRIPPED_AZALEA_WOOD.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, 
                            event.getState().getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)));
        }
    }
    
    /**
     * Registers flammability for azalea wood blocks.
     * Values match vanilla oak wood (encouragement=5, flammability=20).
     */
    private static void registerFlammability() {
        FireBlock fireBlock = (FireBlock) Blocks.FIRE;
        
        // Azalea logs and wood
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.AZALEA_LOG.get(), 5, 5);
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.AZALEA_WOOD.get(), 5, 5);
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.STRIPPED_AZALEA_LOG.get(), 5, 5);
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.STRIPPED_AZALEA_WOOD.get(), 5, 5);
        
        // Azalea planks and derivatives
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.AZALEA_PLANKS.get(), 5, 20);
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.AZALEA_SLAB.get(), 5, 20);
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.AZALEA_STAIRS.get(), 5, 20);
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.AZALEA_FENCE.get(), 5, 20);
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.AZALEA_FENCE_GATE.get(), 5, 20);
        bitsandbalance$setFlammableReflective(fireBlock, ModBlocks.AZALEA_SHELF.get(), 5, 20);
        
        BitsAndBalance.LOGGER.info("Registered flammability for Azalea wood blocks");
    }

    private static void registerDynamicVariantFlammability() {
        FireBlock fireBlock = (FireBlock) Blocks.FIRE;

        for (Block verticalBlock : VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot()) {
            if (verticalBlock instanceof FixedVerticalSlabBlock fixedVerticalBlock) {
                bitsandbalance$registerDerivedFlammability(fireBlock, verticalBlock, fixedVerticalBlock.getSourceSlab());
            }
        }

        for (Block stepBlock : StepDynamicRegistry.getAllStepBlocksSnapshot()) {
            if (stepBlock instanceof FixedStepBlock fixedStepBlock) {
                bitsandbalance$registerDerivedFlammability(fireBlock, stepBlock, fixedStepBlock.getSourceSlab());
            }
        }

        for (Block verticalStepBlock : VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot()) {
            if (verticalStepBlock instanceof FixedVerticalStepBlock fixedVerticalStepBlock) {
                Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(fixedVerticalStepBlock.getSourceVerticalSlab());
                if (sourceSlab != null) {
                    bitsandbalance$registerDerivedFlammability(fireBlock, verticalStepBlock, sourceSlab);
                }
            }
        }
    }

    private static void bitsandbalance$registerDerivedFlammability(FireBlock fireBlock, Block targetBlock, Block sourceSlab) {
        int igniteOdds = DynamicVariantMaterialHelper.getDerivedIgniteOdds(sourceSlab);
        int burnOdds = DynamicVariantMaterialHelper.getDerivedBurnOdds(sourceSlab);
        if (igniteOdds <= 0 && burnOdds <= 0) {
            return;
        }

        bitsandbalance$setFlammableReflective(fireBlock, targetBlock, igniteOdds, burnOdds);
    }

    private static void bitsandbalance$setFlammableReflective(FireBlock fireBlock, Block block, int encouragement, int flammability) {
        try {
            java.lang.reflect.Method method = FireBlock.class.getDeclaredMethod("setFlammable", Block.class, int.class, int.class);
            method.setAccessible(true);
            method.invoke(fireBlock, block, encouragement, flammability);
        } catch (ReflectiveOperationException e) {
            BitsAndBalance.LOGGER.error("Failed to register flammability for {}", block, e);
        }
    }
    
    /**
     * Registers azalea hanging sign blocks with the vanilla HANGING_SIGN block entity type.
     * This allows vanilla HangingSignBlockEntity to work with our custom blocks.
     */
    private static void registerHangingSignBlocks() {
        boolean success = bitsandbalance$addValidBlocksReflective(
                BlockEntityType.HANGING_SIGN,
                java.util.List.of(
                        ModBlocks.AZALEA_HANGING_SIGN.get(),
                        ModBlocks.AZALEA_WALL_HANGING_SIGN.get()
                ),
                Blocks.OAK_HANGING_SIGN
        );
        if (!success) {
            BitsAndBalance.LOGGER.error("Failed to register Azalea hanging sign blocks with vanilla block entity type");
        }
    }

    private static void registerShelfBlocks() {
        boolean success = bitsandbalance$addValidBlocksReflective(
                BlockEntityType.SHELF,
                java.util.List.of(ModBlocks.AZALEA_SHELF.get()),
                Blocks.PALE_OAK_SHELF
        );
        if (!success) {
            BitsAndBalance.LOGGER.error("Failed to register Azalea shelf block with vanilla shelf block entity type");
        }
    }

    private static void registerDynamicVerticalSlabBlocks() {
        try {
            if (CommonBlockEntities.VERTICAL_SLAB == null) return;

            java.util.Collection<net.minecraft.world.level.block.Block> verticalBlocks =
                    VerticalSlabDynamicRegistry.getAllVerticalBlocksSnapshot();
            if (verticalBlocks.isEmpty()) return;

            boolean ok = bitsandbalance$addValidBlocksReflective(
                    CommonBlockEntities.VERTICAL_SLAB,
                    verticalBlocks,
                    ModBlocks.VERTICAL_SLAB.get()
            );
            if (!ok) {
                BitsAndBalance.LOGGER.error("Failed to expand vertical slab block entity validBlocks (reflection field not found)");
            }
        } catch (Throwable ignored) {
        }
    }

    private static void registerDynamicStepBlocks() {
        try {
            if (CommonBlockEntities.STEP == null) return;

            java.util.Collection<net.minecraft.world.level.block.Block> stepBlocks =
                    StepDynamicRegistry.getAllStepBlocksSnapshot();
            if (stepBlocks.isEmpty()) return;

            boolean ok = bitsandbalance$addValidBlocksReflective(
                    CommonBlockEntities.STEP,
                    stepBlocks,
                    ModBlocks.STEP.get()
            );
            if (!ok) {
                BitsAndBalance.LOGGER.error("Failed to expand step block entity validBlocks (reflection field not found)");
            }
        } catch (Throwable ignored) {
        }
    }

    private static void registerDynamicVerticalStepBlocks() {
        try {
            if (CommonBlockEntities.VERTICAL_STEP == null) return;

            java.util.Collection<net.minecraft.world.level.block.Block> verticalStepBlocks =
                    VerticalStepDynamicRegistry.getAllVerticalStepBlocksSnapshot();
            if (verticalStepBlocks.isEmpty()) return;

            boolean ok = bitsandbalance$addValidBlocksReflective(
                    CommonBlockEntities.VERTICAL_STEP,
                    verticalStepBlocks,
                    ModBlocks.VERTICAL_STEP.get()
            );
            if (!ok) {
                BitsAndBalance.LOGGER.error("Failed to expand vertical step block entity validBlocks (reflection field not found)");
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$addValidBlocksReflective(BlockEntityType<?> type,
                                                                   java.util.Collection<net.minecraft.world.level.block.Block> blocksToAdd,
                                                                   net.minecraft.world.level.block.Block seed) {
        if (type == null || blocksToAdd == null || blocksToAdd.isEmpty()) return false;

        // Prefer known names (dev + a couple historical obf names), then fall back to a
        // robust scan: find the Set field that already contains a known-valid block for this type.
        String[] possibleFieldNames = {"validBlocks", "f_155650_", "f_58917_"};

        java.util.List<java.lang.reflect.Field> candidates = new java.util.ArrayList<>();
        for (String fieldName : possibleFieldNames) {
            try {
                candidates.add(BlockEntityType.class.getDeclaredField(fieldName));
            } catch (Throwable ignored) {
            }
        }
        try {
            for (java.lang.reflect.Field f : BlockEntityType.class.getDeclaredFields()) {
                if (java.util.Set.class.isAssignableFrom(f.getType())) {
                    candidates.add(f);
                }
            }
        } catch (Throwable ignored) {
        }

        java.util.Set<java.lang.reflect.Field> tried = new java.util.HashSet<>();
        for (java.lang.reflect.Field validBlocksField : candidates) {
            if (validBlocksField == null) continue;
            if (!tried.add(validBlocksField)) continue;
            try {
                validBlocksField.setAccessible(true);
                Object fieldValue = validBlocksField.get(type);
                if (!(fieldValue instanceof java.util.Set<?>)) continue;

                @SuppressWarnings("unchecked")
                java.util.Set<net.minecraft.world.level.block.Block> validBlocks =
                        (java.util.Set<net.minecraft.world.level.block.Block>) fieldValue;

                // Heuristic: the "valid blocks" set should already contain a known-valid block.
                if (seed != null && !validBlocks.contains(seed)) {
                    continue;
                }

                int before = validBlocks.size();
                try {
                    validBlocks.addAll(blocksToAdd);
                } catch (UnsupportedOperationException e) {
                    java.util.Set<net.minecraft.world.level.block.Block> copy = new java.util.HashSet<>(validBlocks);
                    copy.addAll(blocksToAdd);
                    try {
                        validBlocksField.set(type, copy);
                    } catch (IllegalAccessException ignored) {
                        // If we can't replace the field, we can't expand an immutable set.
                        continue;
                    }
                }
                int after;
                try {
                    Object newValue = validBlocksField.get(type);
                    after = (newValue instanceof java.util.Set<?> s) ? s.size() : before;
                } catch (Throwable t) {
                    after = before;
                }

                BitsAndBalance.LOGGER.info(
                        "Expanded vertical slab block entity validBlocks (field: {}, +{} blocks)",
                        validBlocksField.getName(),
                        Math.max(0, after - before)
                );
                return true;
            } catch (Throwable ignored) {
                // try next candidate
            }
        }

        return false;
    }

    /**
     * Registers fuel burn times for azalea wood items.
     * Values match vanilla oak wood.
     */
    public static void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        ItemStack stack = event.getItemStack();
        Block itemBlock = Block.byItem(stack.getItem());

        int dynamicBurnTime = DynamicVariantMaterialHelper.getFuelBurnTime(itemBlock);
        if (dynamicBurnTime > 0) {
            event.setBurnTime(dynamicBurnTime);
            return;
        }

        // Skip azalea-specific fuel values if the woodset is disabled
        if (!org.onenonly.bitsandbalance.Config.enableAzaleaWoodset) {
            return;
        }
        
        // Logs and wood (300 ticks = 15 seconds)
        if (stack.is(ModItems.AZALEA_LOG.get()) || stack.is(ModItems.AZALEA_WOOD.get()) ||
            stack.is(ModItems.STRIPPED_AZALEA_LOG.get()) || stack.is(ModItems.STRIPPED_AZALEA_WOOD.get())) {
            event.setBurnTime(300);
        }
        // Planks (300 ticks)
        else if (stack.is(ModItems.AZALEA_PLANKS.get())) {
            event.setBurnTime(300);
        }
        // Slabs (150 ticks)
        else if (stack.is(ModItems.AZALEA_SLAB.get())) {
            event.setBurnTime(150);
        }
        // Stairs (300 ticks)
        else if (stack.is(ModItems.AZALEA_STAIRS.get())) {
            event.setBurnTime(300);
        }
        else if (stack.is(ModItems.AZALEA_SHELF.get())) {
            event.setBurnTime(300);
        }
        // Fence and fence gate (300 ticks)
        else if (stack.is(ModItems.AZALEA_FENCE.get()) || stack.is(ModItems.AZALEA_FENCE_GATE.get())) {
            event.setBurnTime(300);
        }
        // Door and trapdoor (200 ticks)
        else if (stack.is(ModItems.AZALEA_DOOR.get()) || stack.is(ModItems.AZALEA_TRAPDOOR.get())) {
            event.setBurnTime(200);
        }
        // Signs (200 ticks)
        else if (stack.is(ModItems.AZALEA_SIGN.get()) || stack.is(ModItems.AZALEA_HANGING_SIGN.get())) {
            event.setBurnTime(200);
        }
        // Button and pressure plate (100 ticks)
        else if (stack.is(ModItems.AZALEA_BUTTON.get()) || stack.is(ModItems.AZALEA_PRESSURE_PLATE.get())) {
            event.setBurnTime(100);
        }
        // Boats (1200 ticks = 60 seconds, matches vanilla)
        else if (stack.is(ModItems.AZALEA_BOAT.get()) || stack.is(ModItems.AZALEA_CHEST_BOAT.get())) {
            event.setBurnTime(1200);
        }
    }
}

