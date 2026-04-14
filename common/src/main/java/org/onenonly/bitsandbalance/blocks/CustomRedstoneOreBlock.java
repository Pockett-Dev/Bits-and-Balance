package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Custom redstone ore block that glows when interacted with and handles Silk Touch properly.
 */
public class CustomRedstoneOreBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    private final ItemStack dropItem;
    private final int minXp;
    private final int maxXp;
    private final int minDropCount;
    private final int maxDropCount;

    public CustomRedstoneOreBlock(Properties properties, ItemStack dropItem, int minXp, int maxXp) {
        this(properties, dropItem, minXp, maxXp, 1, 1);
    }

    public CustomRedstoneOreBlock(Properties properties, ItemStack dropItem, int minXp, int maxXp, int minDropCount, int maxDropCount) {
        super(properties);
        this.dropItem = dropItem;
        this.minXp = minXp;
        this.maxXp = maxXp;
        this.minDropCount = minDropCount;
        this.maxDropCount = maxDropCount;
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        interact(state, level, pos);
        super.attack(state, level, pos, player);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        interact(state, level, pos);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            spawnParticles(level, pos);
        } else {
            interact(state, level, pos);
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    private static void interact(BlockState state, Level level, BlockPos pos) {
        spawnParticles(level, pos);
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, Boolean.TRUE), 3);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, Boolean.FALSE), 3);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            spawnParticles(level, pos);
        }
    }

    private static void spawnParticles(Level level, BlockPos pos) {
        double d0 = 0.5625;
        RandomSource randomsource = level.random;

        for (Direction direction : Direction.values()) {
            BlockPos blockpos = pos.relative(direction);
            if (!level.getBlockState(blockpos).isSolidRender()) {
                Direction.Axis axis = direction.getAxis();
                double d1 = axis == Direction.Axis.X ? 0.5 + d0 * (double) direction.getStepX() : (double) randomsource.nextFloat();
                double d2 = axis == Direction.Axis.Y ? 0.5 + d0 * (double) direction.getStepY() : (double) randomsource.nextFloat();
                double d3 = axis == Direction.Axis.Z ? 0.5 + d0 * (double) direction.getStepZ() : (double) randomsource.nextFloat();
                level.addParticle(DustParticleOptions.REDSTONE, (double) pos.getX() + d1, (double) pos.getY() + d2, (double) pos.getZ() + d3, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getParameter(LootContextParams.TOOL);

        if (tool != null) {
            ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            boolean hasSilkTouch = false;
            for (var holder : enchantments.keySet()) {
                if (holder.is(Enchantments.SILK_TOUCH)) {
                    hasSilkTouch = true;
                    break;
                }
            }

            if (hasSilkTouch) {
                return List.of(new ItemStack(this));
            }

            ItemStack drop = dropItem.copy();
            int baseCount = builder.getLevel().getRandom().nextInt(maxDropCount - minDropCount + 1) + minDropCount;
            drop.setCount(baseCount);

            int fortuneLevel = 0;
            for (var holder : enchantments.keySet()) {
                if (holder.is(Enchantments.FORTUNE)) {
                    fortuneLevel = enchantments.getLevel(holder);
                    break;
                }
            }

            if (fortuneLevel > 0) {
                int bonusCount = builder.getLevel().getRandom().nextInt(fortuneLevel + 2) - 1;
                if (bonusCount < 0) bonusCount = 0;
                drop.setCount(drop.getCount() + bonusCount);
            }

            return List.of(drop);
        }

        ItemStack drop = dropItem.copy();
        int baseCount = builder.getLevel().getRandom().nextInt(maxDropCount - minDropCount + 1) + minDropCount;
        drop.setCount(baseCount);
        return List.of(drop);
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);

        if (dropExperience && !tool.isEmpty()) {
            ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            boolean hasSilkTouch = false;
            for (var holder : enchantments.keySet()) {
                if (holder.is(Enchantments.SILK_TOUCH)) {
                    hasSilkTouch = true;
                    break;
                }
            }

            if (!hasSilkTouch && minXp > 0) {
                int xpAmount = level.getRandom().nextIntBetweenInclusive(minXp, maxXp);
                if (xpAmount > 0) {
                    net.minecraft.world.entity.ExperienceOrb.award(level, Vec3.atCenterOf(pos), xpAmount);
                }
            }
        }
    }
}
