package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.ModBlocks;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;
import org.onenonly.bitsandbalance.common.candle.CandleBundleAccess;
import org.onenonly.bitsandbalance.common.candle.CandleBundleColorUtil;
import net.minecraft.world.item.BlockItem;
import org.onenonly.bitsandbalance.network.CandleBundleColorsUpdatePayload;
import org.onenonly.bitsandbalance.network.CandleBundleContentsUpdatePayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(CandleBlock.class)
@SuppressWarnings("null")
public abstract class CandleBlockBundleMixin {

    private static boolean bitsandbalance$stackMatches(ItemStack a, ItemStack b) {
        if (a == null || a.isEmpty() || b == null || b.isEmpty()) return false;
        return a.getItem() == b.getItem();
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$allowMixedCandlesInOneBlock(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (held == null || held.isEmpty()) return;
        if (state == null || level == null || pos == null) return;

        // Only handle candle-on-candle interactions.
        // Accept items in the #candles tag OR any BlockItem wrapping a CandleBlock (covers our 2032 ColoredCandleBlocks).
        boolean isCandle = held.is(ItemTags.CANDLES)
                || (held.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CandleBlock);
        if (!isCandle) return;
        if (!state.hasProperty(CandleBlock.CANDLES)) return;

        int count = state.getValue(CandleBlock.CANDLES);
        if (count >= 4) return;

        boolean heldMatchesBlockItem = held.is(state.getBlock().asItem());

        // For BE-system colored candles (same block ID but different TINT_RGB),
        // treat different tints as different types for bundle/mixing logic.
        boolean effectiveMatch = heldMatchesBlockItem;

		// If disabled, do not allow mixing different candle items (vanilla behavior).
		if (!Config.allowMixedCandlePlacement && !effectiveMatch) {
            // If this is already a bundle, enforce same-item(+tint) stacking.
            var be = level.getBlockEntity(pos);
            if (!(be instanceof CandleBundleBlockEntity bundleBe)) return;

            ItemStack sample = bundleBe.getCandle(0);
            if (sample == null || sample.isEmpty()) return;
            if (!bitsandbalance$stackMatches(sample, held)) return;
        }

        // Client: vanilla only allows matching candle types. If we're mixing types, pretend success
        // and let the server drive the block update.
        if (level.isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) return;

        long posLong = pos.asLong();
        var bundleData = CandleBundleAccess.get(serverLevel);
        // Always route candle stacking through bundle logic so multi-candle stacks can be oriented.

        // Initialize bundle contents from saved bundle data / block entity / current block state.
        List<ItemStack> contents = bundleData.get(posLong);
        if (contents == null || contents.isEmpty()) {
            contents = new ArrayList<>(count + 1);

            var existingBe = level.getBlockEntity(pos);
            if (existingBe instanceof CandleBundleBlockEntity bundleBe) {
                for (int i = 0; i < count; i++) {
                    ItemStack s = bundleBe.getCandle(i);
                    if (s == null || s.isEmpty()) break;
                    contents.add(s.copyWithCount(1));
                }
            }

            if (contents.isEmpty()) {
                ItemStack base = new ItemStack(state.getBlock().asItem(), 1);

                for (int i = 0; i < count; i++) {
                    contents.add(base.copyWithCount(1));
                }
            }
        }

        contents.add(held.copyWithCount(1));

        net.minecraft.core.Direction facing = net.minecraft.core.Direction.SOUTH;
        var preBe = level.getBlockEntity(pos);
        if (preBe instanceof CandleBundleBlockEntity bundleBe) {
            facing = bundleBe.getFacing();
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (player != null) {
            facing = player.getDirection().getOpposite();
        }

        BlockState next = ModBlocks.BUNDLE_CANDLE.get().defaultBlockState().setValue(CandleBlock.CANDLES, count + 1);
        if (state.hasProperty(BlockStateProperties.LIT) && next.hasProperty(BlockStateProperties.LIT)) {
            next = next.setValue(BlockStateProperties.LIT, state.getValue(BlockStateProperties.LIT));
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && next.hasProperty(BlockStateProperties.WATERLOGGED)) {
            next = next.setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
        }

        // Compute per-candle colors for tintIndex 0..3.
        int[] colors = CandleBundleColorUtil.computeBundleColors(contents);

        for (ServerPlayer p : serverLevel.players()) {
            PacketDistributor.sendToPlayer(p, new CandleBundleColorsUpdatePayload(posLong, colors[0], colors[1], colors[2], colors[3]));
            ItemStack s0 = contents.size() > 0 ? contents.get(0).copyWithCount(1) : ItemStack.EMPTY;
            ItemStack s1 = contents.size() > 1 ? contents.get(1).copyWithCount(1) : ItemStack.EMPTY;
            ItemStack s2 = contents.size() > 2 ? contents.get(2).copyWithCount(1) : ItemStack.EMPTY;
            ItemStack s3 = contents.size() > 3 ? contents.get(3).copyWithCount(1) : ItemStack.EMPTY;
            PacketDistributor.sendToPlayer(p, new CandleBundleContentsUpdatePayload(posLong, s0, s1, s2, s3));
        }

        // Apply state update + persist bundle contents.
        level.setBlock(pos, next, 3);
        bundleData.set(posLong, contents);

        var be = level.getBlockEntity(pos);
        if (be instanceof CandleBundleBlockEntity bundleBe) {
            bundleBe.setFacing(facing);
            bundleBe.setCandles(contents);
        }

        if (player == null || !player.getAbilities().instabuild) {
            held.shrink(1);
        }

        // Vanilla candle stacking plays a placement sound; we bypass vanilla logic, so play it here.
        try {
            level.playSound(null, pos, SoundEvents.CANDLE_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        } catch (Throwable ignored) {
        }

        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
