package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
//? if mc == 1.21.1
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
//? endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if mc != 1.21.1
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//? endif
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;

import java.util.List;

public class CandleBundleBlockEntity extends BlockEntity {
    private static final String TAG_CANDLES = "Candles";
    private static final String TAG_FACING = "Facing";

    private final ItemStack[] candles = new ItemStack[] {
        ItemStack.EMPTY,
        ItemStack.EMPTY,
        ItemStack.EMPTY,
        ItemStack.EMPTY
    };

    /**
     * The direction the smallest candle should face.
     * Vanilla candle models have their smallest candle towards SOUTH by default.
     */
    private Direction facing = Direction.SOUTH;

    public CandleBundleBlockEntity(BlockPos pos, BlockState state) {
        super(CommonBlockEntities.CANDLE_BUNDLE, pos, state);
    }

    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        if (facing == null || !facing.getAxis().isHorizontal()) {
            facing = Direction.SOUTH;
        }
        this.facing = facing;

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getCandle(int index) {
        if (index < 0 || index >= candles.length) {
            return ItemStack.EMPTY;
        }
        return candles[index];
    }

    public void setCandles(List<ItemStack> stacks) {
        for (int i = 0; i < candles.length; i++) {
            ItemStack stack = (stacks != null && i < stacks.size()) ? stacks.get(i) : ItemStack.EMPTY;
            candles[i] = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
        }

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

//? if mc == 1.21.1
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putString(TAG_FACING, facing.getName());

        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag candleList = new ListTag();
        for (ItemStack stack : candles) {
            if (stack == null || stack.isEmpty()) continue;
            Tag encoded = ItemStack.CODEC.encodeStart(ops, stack.copyWithCount(1)).result().orElse(null);
            if (encoded != null) {
                candleList.add(encoded);
            }
        }
        tag.put(TAG_CANDLES, candleList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        Direction loadedFacing = Direction.byName(tag.getString(TAG_FACING));
        if (loadedFacing == null || !loadedFacing.getAxis().isHorizontal()) {
            loadedFacing = Direction.SOUTH;
        }
        facing = loadedFacing;

        for (int i = 0; i < candles.length; i++) {
            candles[i] = ItemStack.EMPTY;
        }

        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        int index = 0;
        for (Tag encoded : tag.getList(TAG_CANDLES, Tag.TAG_COMPOUND)) {
            if (index >= candles.length) break;
            ItemStack stack = ItemStack.CODEC.parse(ops, encoded).result().orElse(ItemStack.EMPTY);
            candles[index] = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
            index++;
        }
    }
//? else
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString(TAG_FACING, facing.getName());

        var list = output.list(TAG_CANDLES, ItemStack.CODEC);
        for (ItemStack stack : candles) {
            if (stack == null || stack.isEmpty()) continue;
            list.add(stack.copyWithCount(1));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        Direction loadedFacing = Direction.byName(input.getStringOr(TAG_FACING, Direction.SOUTH.getName()));
        if (loadedFacing == null || !loadedFacing.getAxis().isHorizontal()) {
            loadedFacing = Direction.SOUTH;
        }
        facing = loadedFacing;

        for (int i = 0; i < candles.length; i++) {
            candles[i] = ItemStack.EMPTY;
        }

        int index = 0;
        for (ItemStack stack : input.listOrEmpty(TAG_CANDLES, ItemStack.CODEC)) {
            if (index >= candles.length) break;
            candles[index] = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
            index++;
        }
    }
//? endif

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
