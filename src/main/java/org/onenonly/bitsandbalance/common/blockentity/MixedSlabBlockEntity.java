package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if mc != 1.21.1
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//? endif
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;

/**
 * Passive persistence record for a mixed slab's two materials.
 *
 * <p>The optimized material store remains authoritative while the world is running. This block
 * entity has no ticker or renderer; it exists so normal chunk NBT, structures, and third-party
 * schematic tools have a portable copy of the materials.</p>
 */
public final class MixedSlabBlockEntity extends BlockEntity {
    private static final String TAG_BOTTOM_SLAB = "BottomSlab";
    private static final String TAG_TOP_SLAB = "TopSlab";
    private static final Block DEFAULT_SLAB = Blocks.OAK_SLAB;

    private BlockState bottomSlab = DEFAULT_SLAB.defaultBlockState();
    private BlockState topSlab = DEFAULT_SLAB.defaultBlockState();

    public MixedSlabBlockEntity(BlockPos pos, BlockState state) {
        super(CommonBlockEntities.MIXED_SLAB, pos, state);
        bottomSlab = normalize(bottomSlab);
        topSlab = normalize(topSlab);
    }

    public BlockState getBottomSlab() {
        return bottomSlab;
    }

    public BlockState getTopSlab() {
        return topSlab;
    }

    public void setSlabs(@Nullable BlockState bottom, @Nullable BlockState top) {
        bitsandbalance$setSlabsFromStore(bottom, top);
        EnhancedSlabMaterialSavedData.remember(level, worldPosition.asLong(), bottomSlab, topSlab);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            serverLevel.getChunkSource().blockChanged(worldPosition);
        }
    }

    /** Updates the portable mirror without feeding back into the runtime store. */
    public void bitsandbalance$setSlabsFromStore(@Nullable BlockState bottom, @Nullable BlockState top) {
        BlockState normalizedBottom = normalize(bottom);
        BlockState normalizedTop = normalize(top);
        if (normalizedBottom.equals(bottomSlab) && normalizedTop.equals(topSlab)) {
            return;
        }
        bottomSlab = normalizedBottom;
        topSlab = normalizedTop;
        // Passive client hydration must not start recursive neighbor shape queries.
        if (level != null && !level.isClientSide()) setChanged();
    }

    //? if mc == 1.21.1
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_BOTTOM_SLAB, SlabStateTagHelper.saveSlabState(bottomSlab, DEFAULT_SLAB));
        tag.put(TAG_TOP_SLAB, SlabStateTagHelper.saveSlabState(topSlab, DEFAULT_SLAB));
    }
    //? endif

    //? if mc == 1.21.1
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tag.contains(TAG_BOTTOM_SLAB) && !tag.contains(TAG_TOP_SLAB)) {
            EnhancedSlabMaterialSavedData.hydrateClientBlockEntity(level, this);
            return;
        }
        bottomSlab = SlabStateTagHelper.loadStoredSlabState(tag.getCompound(TAG_BOTTOM_SLAB), DEFAULT_SLAB);
        topSlab = SlabStateTagHelper.loadStoredSlabState(tag.getCompound(TAG_TOP_SLAB), DEFAULT_SLAB);
        if (level != null) {
            EnhancedSlabMaterialSavedData.remember(level, worldPosition.asLong(), bottomSlab, topSlab);
        }
    }
    //? endif

    //? if mc != 1.21.1
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(TAG_BOTTOM_SLAB, CompoundTag.CODEC, SlabStateTagHelper.saveSlabState(bottomSlab, DEFAULT_SLAB));
        output.store(TAG_TOP_SLAB, CompoundTag.CODEC, SlabStateTagHelper.saveSlabState(topSlab, DEFAULT_SLAB));
    }
    //? endif

    //? if mc != 1.21.1
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (input.child(TAG_BOTTOM_SLAB).isEmpty() && input.getString(TAG_BOTTOM_SLAB).isEmpty()
                && input.child(TAG_TOP_SLAB).isEmpty() && input.getString(TAG_TOP_SLAB).isEmpty()) {
            EnhancedSlabMaterialSavedData.hydrateClientBlockEntity(level, this);
            return;
        }
        bottomSlab = SlabStateTagHelper.readStoredSlabState(input, TAG_BOTTOM_SLAB, DEFAULT_SLAB);
        topSlab = SlabStateTagHelper.readStoredSlabState(input, TAG_TOP_SLAB, DEFAULT_SLAB);
        if (level != null) {
            EnhancedSlabMaterialSavedData.remember(level, worldPosition.asLong(), bottomSlab, topSlab);
        }
    }
    //? endif

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static BlockState normalize(@Nullable BlockState state) {
        return SlabStateTagHelper.normalizeStoredSlabState(state, DEFAULT_SLAB);
    }
}
