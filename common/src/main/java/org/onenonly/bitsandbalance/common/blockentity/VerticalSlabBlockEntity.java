package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;

/**
 * Tweaks: Enhanced Slab Behavior — Vertical Slab Block Entity
 *
 * Stores the identity of one or two slab halves for {@link org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock}.
 *
 * Each half is serialized as a slab block id.
 */
public class VerticalSlabBlockEntity extends BlockEntity {

    private static final String TAG_FACING_SLAB = "FacingSlab";
    private static final String TAG_OPPOSITE_SLAB = "OppositeSlab";

    private static final Block DEFAULT_SLAB = Blocks.OAK_SLAB;

    private BlockState facingSlab;
    private BlockState oppositeSlab;

    public VerticalSlabBlockEntity(BlockPos pos, BlockState state) {
        super(CommonBlockEntities.VERTICAL_SLAB, pos, state);
        BlockState initial = bitsandbalance$defaultSlabStateForOwner(state);
        this.facingSlab = initial;
        this.oppositeSlab = initial;
    }

    public static BlockState bitsandbalance$defaultSlabStateForOwner(BlockState ownerState) {
        Block slabBlock = DEFAULT_SLAB;
        try {
            Block owningBlock = ownerState.getBlock();
            if (owningBlock instanceof FixedVerticalSlabBlock fixed) {
                slabBlock = fixed.getSourceSlab();
            } else {
                Block mapped = VerticalSlabDynamicRegistry.getSlabForVertical(owningBlock);
                if (mapped != null) slabBlock = mapped;
            }
        } catch (Throwable ignored) {
        }

        BlockState initial = slabBlock.defaultBlockState();
        if (initial.hasProperty(SlabBlock.TYPE)) {
            initial = initial.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (initial.hasProperty(SlabBlock.WATERLOGGED)) {
            initial = initial.setValue(SlabBlock.WATERLOGGED, false);
        }
        return initial;
    }

    public BlockState getFacingSlab() {
        return facingSlab;
    }

    public BlockState getOppositeSlab() {
        return oppositeSlab;
    }

    public void setSlabs(BlockState facing, BlockState opposite) {
        this.facingSlab = normalizeSlabState(facing);
        this.oppositeSlab = normalizeSlabState(opposite);
        setChanged();
        bitsandbalance$sync();
    }

    public void setSingleFacingSlab(BlockState facing) {
        this.facingSlab = normalizeSlabState(facing);
        setChanged();
        bitsandbalance$sync();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString(TAG_FACING_SLAB, blockId(facingSlab));
        output.putString(TAG_OPPOSITE_SLAB, blockId(oppositeSlab));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        facingSlab = resolveSlabState(input.getStringOr(TAG_FACING_SLAB, ""));
        oppositeSlab = resolveSlabState(input.getStringOr(TAG_OPPOSITE_SLAB, ""));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void bitsandbalance$sync() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        serverLevel.getChunkSource().blockChanged(worldPosition);

        Packet<ClientGamePacketListener> pkt = getUpdatePacket();
        if (pkt != null) {
            for (ServerPlayer p : serverLevel.players()) {
                try { p.connection.send(pkt); } catch (Throwable ignored) { }
            }
        }
    }

    private static String blockId(BlockState state) {
        if (state == null) return BuiltInRegistries.BLOCK.getKey(DEFAULT_SLAB).toString();
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static BlockState resolveSlabState(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return DEFAULT_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        try {
            Identifier identifier = Identifier.parse(serialized);
            Block block = BuiltInRegistries.BLOCK.getValue(identifier);
            return normalizeSlabState(block != null ? block.defaultBlockState() : null);
        } catch (Exception e) {
            return DEFAULT_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
    }

    private static BlockState normalizeSlabState(@Nullable BlockState state) {
        BlockState normalized = (state != null && state.getBlock() instanceof SlabBlock)
                ? state.getBlock().defaultBlockState()
                : DEFAULT_SLAB.defaultBlockState();
        if (normalized.hasProperty(SlabBlock.TYPE)) {
            normalized = normalized.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (normalized.hasProperty(SlabBlock.WATERLOGGED)) {
            normalized = normalized.setValue(SlabBlock.WATERLOGGED, false);
        }
        return normalized;
    }
}
