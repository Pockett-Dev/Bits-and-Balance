package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.entity.ItemDropHomingHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class QuickHarvestingLogic {
    private static final long QUICK_HARVEST_DROP_HOME_DURATION_TICKS = 20L;

    private QuickHarvestingLogic() {
    }

    public static boolean shouldHandleInteraction(ItemStack stack, BlockState state, boolean hoesEnabled, boolean axesEnabled) {
        HarvestMode mode = resolveMode(stack, hoesEnabled, axesEnabled);
        return describeTarget(mode, state) != null;
    }

    public static boolean tryHandleInteraction(ServerPlayer player, Level level, InteractionHand hand, BlockPos originPos, BlockState originState, boolean hoesEnabled, boolean axesEnabled, boolean homeDropsToUser) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        ItemStack stack = player.getItemInHand(hand);
        HarvestMode mode = resolveMode(stack, hoesEnabled, axesEnabled);
        if (describeTarget(mode, originState) == null) {
            return false;
        }

        EquipmentSlot usedSlot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        boolean handled = false;

        for (BlockPos targetPos : collectTargets(mode, originPos)) {
            if (!serverLevel.isLoaded(targetPos)) {
                continue;
            }
            if (mode != HarvestMode.HAND && mode != HarvestMode.NONE && stack.isEmpty()) {
                break;
            }

            BlockState targetState = serverLevel.getBlockState(targetPos);
            HarvestAction action = describeTarget(mode, targetState);
            if (action == null) {
                continue;
            }

            if (harvestTarget(player, serverLevel, targetPos, targetState, stack, usedSlot, action, homeDropsToUser)) {
                handled = true;
            }
        }

        return handled;
    }

    private static List<BlockPos> collectTargets(HarvestMode mode, BlockPos originPos) {
        List<BlockPos> targets = new ArrayList<>();
        targets.add(originPos);
        if (mode != HarvestMode.HOE) {
            return targets;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                targets.add(originPos.offset(dx, 0, dz));
            }
        }
        return targets;
    }

    private static HarvestMode resolveMode(ItemStack stack, boolean hoesEnabled, boolean axesEnabled) {
        if (stack.isEmpty()) {
            return HarvestMode.HAND;
        }
        if (hoesEnabled && stack.getItem() instanceof HoeItem) {
            return HarvestMode.HOE;
        }
        if (axesEnabled && stack.getItem() instanceof AxeItem) {
            return HarvestMode.AXE;
        }
        return HarvestMode.NONE;
    }

    private static HarvestAction describeTarget(HarvestMode mode, BlockState state) {
        return switch (mode) {
            case HAND, HOE -> describeHandOrHoeTarget(state);
            case AXE -> describeAxeTarget(state);
            case NONE -> null;
        };
    }

    private static HarvestAction describeHandOrHoeTarget(BlockState state) {
        Block block = state.getBlock();
        if (isStemTarget(block) || isAxeSpecificTarget(state)) {
            return null;
        }

        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state) ? HarvestAction.REPLANT_FROM_CROP_ITEM : null;
        }

        return describeGenericAgeTarget(state, true);
    }

    private static HarvestAction describeAxeTarget(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.PUMPKIN || block == Blocks.MELON) {
            return HarvestAction.NO_REPLANT;
        }
        if (block instanceof CocoaBlock) {
            return isMature(state, CocoaBlock.AGE) ? HarvestAction.REPLANT_COCOA : null;
        }
        if (!isGenericAxeTarget(state)) {
            return null;
        }
        return HarvestAction.REPLANT_FROM_BLOCK_ITEM;
    }

    private static boolean isAxeSpecificTarget(BlockState state) {
        return describeAxeTarget(state) != null;
    }

    private static boolean isGenericAxeTarget(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock || block instanceof CocoaBlock) {
            return false;
        }
        if (isStemTarget(block) || !state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return false;
        }
        return describeGenericAgeTarget(state, false) == HarvestAction.REPLANT_FROM_BLOCK_ITEM;
    }

    private static HarvestAction describeGenericAgeTarget(BlockState state, boolean requireNonAxeTag) {
        Optional<IntegerProperty> age = findAgeProperty(state);
        if (age.isEmpty() || !isMature(state, age.get())) {
            return null;
        }

        Block block = state.getBlock();
        if (requireNonAxeTag && state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return null;
        }
        if (block.asItem() == Items.AIR) {
            return null;
        }
        return HarvestAction.REPLANT_FROM_BLOCK_ITEM;
    }

    private static boolean harvestTarget(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, EquipmentSlot usedSlot, HarvestAction action, boolean homeDropsToUser) {
        Block block = state.getBlock();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = collectDrops(block, state, level, pos, blockEntity, player, tool);

        if (!level.destroyBlock(pos, false, player)) {
            return false;
        }

        applyReplant(level, pos, state, drops, action);

        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                if (homeDropsToUser) {
                    ItemDropHomingHelper.popExactResource(level, pos, drop, player, QUICK_HARVEST_DROP_HOME_DURATION_TICKS);
                } else {
                    Block.popResource(level, pos, drop);
                }
            }
        }

        state.spawnAfterBreak(level, pos, tool, true);
        player.awardStat(Stats.BLOCK_MINED.get(block));
        player.causeFoodExhaustion(0.005F);

        if (!tool.isEmpty()) {
            tool.hurtAndBreak(1, player, usedSlot);
        }

        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static List<ItemStack> collectDrops(Block block, BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, ServerPlayer player, ItemStack tool) {
        var lootTableKey = block.getLootTable().orElse(null);
        if (lootTableKey == null) {
            return List.of();
        }

        LootParams.Builder builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);

        return level.getServer()
                .reloadableRegistries()
                .getLootTable(lootTableKey)
                .getRandomItems(builder.create(LootContextParamSets.BLOCK));
    }

    private static void applyReplant(ServerLevel level, BlockPos pos, BlockState state, List<ItemStack> drops, HarvestAction action) {
        switch (action) {
            case NO_REPLANT -> {
            }
            case REPLANT_FROM_CROP_ITEM -> {
                if (!(state.getBlock() instanceof CropBlock crop)) {
                    return;
                }
                if (consumeCropSeed(drops, crop)) {
                    level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
                }
            }
            case REPLANT_FROM_BLOCK_ITEM -> {
                Optional<IntegerProperty> age = findAgeProperty(state);
                if (age.isEmpty()) {
                    return;
                }
                int minAge = age.get().getPossibleValues().stream().min(Integer::compareTo).orElse(0);
                if (consumeBlockItem(drops, state.getBlock().asItem())) {
                    level.setBlock(pos, state.setValue(age.get(), minAge), Block.UPDATE_ALL);
                }
            }
            case REPLANT_COCOA -> {
                if (consumeBlockItem(drops, Items.COCOA_BEANS)) {
                    level.setBlock(pos, state.setValue(CocoaBlock.AGE, 0), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static boolean consumeCropSeed(List<ItemStack> drops, Block cropBlock) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty() || drop.getCount() <= 0) {
                continue;
            }
            if (!(drop.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            if (blockItem.getBlock() != cropBlock) {
                continue;
            }

            drop.shrink(1);
            return true;
        }
        return false;
    }

    private static boolean consumeBlockItem(List<ItemStack> drops, Item item) {
        if (item == Items.AIR) {
            return false;
        }
        for (ItemStack drop : drops) {
            if (drop.getItem() != item || drop.getCount() <= 0) {
                continue;
            }

            drop.shrink(1);
            return true;
        }
        return false;
    }

    private static boolean isStemTarget(Block block) {
        return block == Blocks.MELON_STEM
                || block == Blocks.PUMPKIN_STEM
                || block == Blocks.ATTACHED_MELON_STEM
                || block == Blocks.ATTACHED_PUMPKIN_STEM;
    }

    private static Optional<IntegerProperty> findAgeProperty(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty intProp && "age".equals(intProp.getName())) {
                return Optional.of(intProp);
            }
        }
        return Optional.empty();
    }

    private static boolean isMature(BlockState state, IntegerProperty ageProperty) {
        int current = state.getValue(ageProperty);
        int max = ageProperty.getPossibleValues().stream().max(Integer::compareTo).orElse(current);
        return current >= max;
    }

    private enum HarvestMode {
        NONE,
        HAND,
        HOE,
        AXE
    }

    private enum HarvestAction {
        NO_REPLANT,
        REPLANT_FROM_CROP_ITEM,
        REPLANT_FROM_BLOCK_ITEM,
        REPLANT_COCOA
    }
}