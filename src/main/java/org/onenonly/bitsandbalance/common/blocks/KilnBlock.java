package org.onenonly.bitsandbalance.common.blocks;

//? if mc != 26.3
import com.mojang.serialization.MapCodec;
//? endif
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
//? if mc == 26.2 || mc == 26.3
import net.minecraft.world.level.block.entity.BlockEntityTypes;
//? endif
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.KilnFurnaceBlockEntity;

/**
 * A furnace-family block whose recipe selection and speed are specialized by
 * {@code AbstractFurnaceBlockEntityKilnMixin}.
 */
public final class KilnBlock extends AbstractFurnaceBlock {
//? if mc != 26.3
    public static final MapCodec<KilnBlock> CODEC = simpleCodec(KilnBlock::new);
//? endif

    public KilnBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

//? if mc != 26.3
    @Override
    public MapCodec<KilnBlock> codec() {
        return CODEC;
    }
//? endif

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KilnFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        //? if mc == 26.2 || mc == 26.3
        return createFurnaceTicker(level, type, BlockEntityTypes.FURNACE);
        //? else
        return createFurnaceTicker(level, type, BlockEntityType.FURNACE);
        //? endif
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FurnaceBlockEntity furnace) {
            Component title = furnace.getCustomName();
            if (title == null) {
                title = Component.translatable("container.bitsandbalance.kiln");
            }
            MenuProvider menuProvider = new SimpleMenuProvider(furnace::createMenu, title);
            player.openMenu(menuProvider);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }

        Direction direction = state.getValue(FACING);
        Direction.Axis axis = direction.getAxis();
        double sideways = random.nextDouble() * 0.6 - 0.3;
        double dx = axis == Direction.Axis.X ? direction.getStepX() * 0.52 : sideways;
        double dy = random.nextDouble() * 6.0 / 16.0;
        double dz = axis == Direction.Axis.Z ? direction.getStepZ() * 0.52 : sideways;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);

        double topJitterX = random.nextDouble() * 0.2 - 0.1;
        double topJitterZ = random.nextDouble() * 0.2 - 0.1;
        level.addParticle(ParticleTypes.SMOKE, x + topJitterX, y + 1.02, z + topJitterZ,
                0.0, 0.0, 0.0);
    }
}
