package org.onenonly.bitsandbalance.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudItemTransforms;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudPlacedGlass;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudRuntime;

public final class BottleOfCloudItem extends Item {
    public BottleOfCloudItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!BottleOfCloudRuntime.enabled) {
            return InteractionResult.PASS;
        }

        // Place in mid-air at the configured range (minimum 2 blocks from player eye position).
        double placeDistance = Math.max(2.0D, BottleOfCloudRuntime.placementDistance);
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 target = eyePos.add(look.scale(placeDistance));
        BlockPos blockPos = BlockPos.containing(target);

        // If we're inside a block, nudge upward to a safe-ish spot.
        if (!level.getBlockState(blockPos).getCollisionShape(level, blockPos, CollisionContext.empty()).isEmpty()) {
            blockPos = blockPos.above();
        }

        int durationTicks = Math.max(1, BottleOfCloudRuntime.durationTicks);
        int fadeLastTicks = Math.max(0, BottleOfCloudRuntime.fadeStartTicks);
        if (fadeLastTicks > durationTicks) fadeLastTicks = durationTicks;

        int minY = level.dimensionType().minY();
        int maxY = minY + level.dimensionType().height();
        if (blockPos.getY() < minY || blockPos.getY() >= maxY) {
            return InteractionResult.PASS;
        }

        BlockState existing = level.getBlockState(blockPos);
        if (!existing.canBeReplaced()) {
            return InteractionResult.PASS;
        }

        if (!player.mayUseItemAt(blockPos, Direction.UP, stack)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            ItemStack transformedStack = BottleOfCloudItemTransforms.emptyCloudBottle(stack, player);
            return InteractionResult.SUCCESS.heldItemTransformedTo(transformedStack);
        }

        BlockState cloudState = ModBottleOfCloud.cloudBlock().defaultBlockState().setValue(CloudBlock.TEMPORARY, Boolean.TRUE);
        boolean placed = level.setBlock(blockPos, cloudState, 3);
        if (!placed) {
            return InteractionResult.PASS;
        }

        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            if (sl.getBlockEntity(blockPos) instanceof TemporaryCloudBlockEntity temporaryCloud) {
                temporaryCloud.configureFade(sl.getGameTime(), durationTicks, fadeLastTicks);
            }
            BottleOfCloudPlacedGlass.track(sl, blockPos, durationTicks, fadeLastTicks);
        }

        level.levelEvent(2001, blockPos, net.minecraft.world.level.block.Block.getId(ModBottleOfCloud.cloudBlock().defaultBlockState()));
        SoundType soundType = ModBottleOfCloud.cloudBlock().defaultBlockState().getSoundType();
        level.playSound(null, blockPos, soundType.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);

        ItemStack transformedStack = BottleOfCloudItemTransforms.emptyCloudBottle(stack, player);
        player.setItemInHand(hand, transformedStack);
        return InteractionResult.SUCCESS.heldItemTransformedTo(transformedStack);
    }

}
