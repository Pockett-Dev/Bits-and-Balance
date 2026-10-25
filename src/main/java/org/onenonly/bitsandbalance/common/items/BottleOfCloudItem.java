package org.onenonly.bitsandbalance.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
//? if mc == 1.21.1
import net.minecraft.world.InteractionResultHolder;
//? else
import net.minecraft.world.InteractionResult;
//? endif
//? if mc == 1.21.1
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
//? endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
//? if mc == 1.21.1
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceBlockDisplayAccess;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceDisplayAccess;
//? else
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
//? endif
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
    //? if mc == 1.21.1
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    //? else
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //? endif
        ItemStack stack = player.getItemInHand(hand);

        if (!BottleOfCloudRuntime.enabled) {
            //? if mc == 1.21.1
            return InteractionResultHolder.pass(stack);
            //? else
            return InteractionResult.PASS;
            //? endif
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
            //? if mc == 1.21.1
            return InteractionResultHolder.pass(stack);
            //? else
            return InteractionResult.PASS;
            //? endif
        }

        BlockState existing = level.getBlockState(blockPos);
        if (!existing.canBeReplaced()) {
            //? if mc == 1.21.1
            return InteractionResultHolder.pass(stack);
            //? else
            return InteractionResult.PASS;
            //? endif
        }

        if (!player.mayUseItemAt(blockPos, Direction.UP, stack)) {
            //? if mc == 1.21.1
            return InteractionResultHolder.pass(stack);
            //? else
            return InteractionResult.PASS;
            //? endif
        }

        //? if mc == 1.21.1
        ItemStack transformedStack = BottleOfCloudItemTransforms.emptyCloudBottle(stack, player);
        //? endif
        if (level.isClientSide()) {
            //? if mc != 1.21.1
            ItemStack transformedStack = BottleOfCloudItemTransforms.emptyCloudBottle(stack, player);
            //? endif
            //? if mc == 1.21.1
            return InteractionResultHolder.sidedSuccess(transformedStack, true);
            //? else
            return InteractionResult.SUCCESS.heldItemTransformedTo(transformedStack);
            //? endif
        }

        BlockState cloudState = ModBottleOfCloud.cloudBlock().defaultBlockState().setValue(CloudBlock.TEMPORARY, Boolean.TRUE);
        boolean placed = level.setBlock(blockPos, cloudState, 3);
        if (!placed) {
            //? if mc == 1.21.1
            return InteractionResultHolder.pass(stack);
            //? else
            return InteractionResult.PASS;
            //? endif
        }

        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            //? if mc != 1.21.1
            if (sl.getBlockEntity(blockPos) instanceof TemporaryCloudBlockEntity temporaryCloud) {
                temporaryCloud.configureFade(sl.getGameTime(), durationTicks, fadeLastTicks);
            }
            BottleOfCloudPlacedGlass.track(sl, blockPos, durationTicks, fadeLastTicks);
            //? else
            Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, sl);
            display.setPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            ((BitsAndBalanceBlockDisplayAccess) (Object) display).bitsandbalance$setBlockState(ModBottleOfCloud.cloudBlock().defaultBlockState());
            // Start fully opaque so the Bottle o' Cloud version is visually
            // indistinguishable from a normal cloud block until fading begins.
            ((BitsAndBalanceDisplayAccess) (Object) display).bitsandbalance$setGlowColorOverride(
                (255 << 24) | BottleOfCloudPlacedGlass.FADE_SENTINEL_RGB);
            sl.addFreshEntity(display);

            BottleOfCloudPlacedGlass.track(sl, blockPos, durationTicks, fadeLastTicks, display.getUUID());
            //? endif
        }

        level.levelEvent(2001, blockPos, net.minecraft.world.level.block.Block.getId(ModBottleOfCloud.cloudBlock().defaultBlockState()));
        SoundType soundType = ModBottleOfCloud.cloudBlock().defaultBlockState().getSoundType();
        level.playSound(null, blockPos, soundType.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);

        //? if mc != 1.21.1
        ItemStack transformedStack = BottleOfCloudItemTransforms.emptyCloudBottle(stack, player);
        //? endif
        player.setItemInHand(hand, transformedStack);
        //? if mc == 1.21.1
        return InteractionResultHolder.sidedSuccess(transformedStack, false);
        //? else
        return InteractionResult.SUCCESS.heldItemTransformedTo(transformedStack);
        //? endif
    }

}
