package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonMoveCache;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;

/**
 * Tweaks: Enhanced Slab Behavior — Mixed Double Slab Block Entity
 *
 * Stores the identity of the two slab halves (bottom and top) that make up
 * a {@link org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock}.
 *
 * <p>Each half is persisted as a slab block id. The live half {@link BlockState}s are
 * reconstructed on demand with the appropriate {@link SlabType} applied.</p>
 */
public class MixedSlabBlockEntity extends BlockEntity {

    private static final String TAG_BOTTOM_SLAB = "BottomSlab";
    private static final String TAG_TOP_SLAB = "TopSlab";

    /** Fallback block used when stored slab id cannot be resolved. */
    private static final Block DEFAULT_SLAB = Blocks.OAK_SLAB;

    private Block bottomSlabBlock;
    private Block topSlabBlock;

    /**
     * If this BE was created as part of a piston move, we may need to restore slab halves
     * after NBT load depending on lifecycle ordering (some paths can call {@link #setLevel(Level)}
     * before {@link #loadAdditional(ValueInput)}).
     */
    private @Nullable MixedSlabPistonMoveCache.Entry pendingPistonRestore;

    public MixedSlabBlockEntity(BlockPos pos, BlockState state) {
        super(CommonBlockEntities.MIXED_SLAB, pos, state);
        this.bottomSlabBlock = DEFAULT_SLAB;
        this.topSlabBlock = DEFAULT_SLAB;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);

        // If this mixed slab was moved by a piston, consume the cached halves.
        if (level.isClientSide()) return;
        MixedSlabPistonMoveCache.Entry e = MixedSlabPistonMoveCache.take(worldPosition.asLong());
        if (e != null) {
            pendingPistonRestore = e;
            setSlabs(e.bottomSlab(), e.topSlab());
        }
    }

    // ── Getters / setters ───────────────────────────────────────────────────

    public BlockState getBottomSlab() {
        return makeHalfState(bottomSlabBlock, SlabType.BOTTOM);
    }

    public BlockState getTopSlab() {
        return makeHalfState(topSlabBlock, SlabType.TOP);
    }

    /**
     * Sets both slab halves and marks the block entity dirty + triggers a
     * client sync packet.
     */
    public void setSlabs(BlockState bottom, BlockState top) {
        this.bottomSlabBlock = slabBlock(bottom);
        this.topSlabBlock = slabBlock(top);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    // ── Persistence ─────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString(TAG_BOTTOM_SLAB, blockId(bottomSlabBlock));
        output.putString(TAG_TOP_SLAB, blockId(topSlabBlock));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        bottomSlabBlock = resolveSlabBlock(input.getStringOr(TAG_BOTTOM_SLAB, ""));
        topSlabBlock = resolveSlabBlock(input.getStringOr(TAG_TOP_SLAB, ""));

        if (pendingPistonRestore != null) {
            MixedSlabPistonMoveCache.Entry e = pendingPistonRestore;
            pendingPistonRestore = null;
            setSlabs(e.bottomSlab(), e.topSlab());
        }
    }

    // ── Client sync ─────────────────────────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String blockId(@Nullable Block block) {
        return BuiltInRegistries.BLOCK.getKey(block != null ? block : DEFAULT_SLAB).toString();
    }

    private static Block slabBlock(@Nullable BlockState state) {
        if (state == null || !(state.getBlock() instanceof SlabBlock)) {
            return DEFAULT_SLAB;
        }
        return state.getBlock();
    }

    private static BlockState makeHalfState(@Nullable Block block, SlabType type) {
        Block slabBlock = (block instanceof SlabBlock) ? block : DEFAULT_SLAB;
        BlockState state = slabBlock.defaultBlockState();
        if (state.hasProperty(SlabBlock.TYPE)) {
            state = state.setValue(SlabBlock.TYPE, type);
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            state = state.setValue(SlabBlock.WATERLOGGED, false);
        }
        return state;
    }

    private static Block resolveSlabBlock(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return DEFAULT_SLAB;
        }
        try {
            String blockIdStr = serialized;
            int bracketIdx = serialized.indexOf('[');
            if (bracketIdx >= 0) {
                blockIdStr = serialized.substring(0, bracketIdx);
            }
            Identifier identifier = Identifier.parse(blockIdStr);
            Block block = BuiltInRegistries.BLOCK.getValue(identifier);
            return block instanceof SlabBlock ? block : DEFAULT_SLAB;
        } catch (Exception e) {
            return DEFAULT_SLAB;
        }
    }
}
