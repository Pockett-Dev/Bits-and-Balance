package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

/**
 * Tweaks: Enhanced Slab Behavior — Quad Step Block Entity
 *
 * Stores up to 4 slab states, one per quadrant in a {@link org.onenonly.bitsandbalance.common.blocks.QuadStepBlock}.
 */
public class QuadStepBlockEntity extends BlockEntity {

    private static final String[] SLOT_TAGS = { "Slot0", "Slot1", "Slot2", "Slot3" };
    private static final Block DEFAULT_SLAB = Blocks.OAK_SLAB;

    private final @Nullable BlockState[] slabs = new BlockState[4];

    public QuadStepBlockEntity(BlockPos pos, BlockState state) {
        super(CommonBlockEntities.QUAD_STEP, pos, state);
    }

    public @Nullable BlockState getSlabAt(int index) {
        if (index < 0 || index >= 4) return null;
        return slabs[index];
    }

    public void setSlabAt(int index, @Nullable BlockState state) {
        if (index < 0 || index >= 4) return;
        BlockState normalized = bitsandbalance$normalizeStoredSlab(state);
        slabs[index] = normalized;
        setChanged();
        bitsandbalance$refreshFluidGeometry();
        sync();
    }

    public void bitsandbalance$setSlabAtSilent(int index, @Nullable BlockState state) {
        if (index < 0 || index >= 4) return;
        BlockState normalized = bitsandbalance$normalizeStoredSlab(state);
        slabs[index] = normalized;
        setChanged();
        bitsandbalance$refreshFluidGeometry();
    }

    public BlockState[] getAllSlabs() {
        return slabs.clone();
    }

    public void setAllSlabs(BlockState[] incoming) {
        for (int i = 0; i < 4; i++) {
            BlockState original = (incoming != null && i < incoming.length) ? incoming[i] : null;
            BlockState normalized = original != null ? bitsandbalance$normalizeStoredSlab(original) : null;
            slabs[i] = normalized;
        }
        setChanged();
        bitsandbalance$refreshFluidGeometry();
        sync();
    }

    public void bitsandbalance$setAllSlabsSilent(BlockState[] incoming) {
        for (int i = 0; i < 4; i++) {
            BlockState original = (incoming != null && i < incoming.length) ? incoming[i] : null;
            BlockState normalized = original != null ? bitsandbalance$normalizeStoredSlab(original) : null;
            slabs[i] = normalized;
        }
        setChanged();
        bitsandbalance$refreshFluidGeometry();
    }

    /** Copies the runtime index into this passive record without publishing or updating shapes. */
    public void bitsandbalance$setSlabsFromStore(BlockState[] incoming) {
        for (int i = 0; i < slabs.length; i++) {
            slabs[i] = incoming != null && i < incoming.length ? incoming[i] : null;
        }
        // Client attachment must not notify neighbors: their shape queries can lazily
        // attach another container and recurse through an entire partial-quad field.
        if (level != null && !level.isClientSide()) setChanged();
    }

    public int getCount() {
        int n = 0;
        for (BlockState s : slabs) if (s != null) n++;
        return n;
    }

    public int bitsandbalance$countMatchingSlabs(@Nullable BlockState match) {
        if (match == null) return 0;
        int count = 0;
        for (BlockState slab : slabs) {
            if (match.equals(slab)) {
                count++;
            }
        }
        return count;
    }

    public boolean bitsandbalance$requiresKneeslabBreaking() {
        BlockState first = null;
        for (BlockState slab : slabs) {
            if (slab == null) continue;
            if (first == null) {
                first = slab;
                continue;
            }
            if (!first.equals(slab)) {
                return true;
            }
        }
        return false;
    }

    private void bitsandbalance$refreshFluidGeometry() {
        org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabFluidGeometryRefresh
                .afterStoredShapeChange(level, worldPosition, getBlockState());
    }

    private void sync() {
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

    public void bitsandbalance$syncToClients() {
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < 4; i++) {
            if (slabs[i] != null) {
                tag.putString(SLOT_TAGS[i], blockId(slabs[i]));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 4; i++) {
            String raw = tag.getString(SLOT_TAGS[i]);
            slabs[i] = raw.isEmpty() ? null : resolveSlabState(raw);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static String blockId(@Nullable BlockState state) {
        if (state == null) return BuiltInRegistries.BLOCK.getKey(DEFAULT_SLAB).toString();
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static @Nullable BlockState resolveSlabState(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return normalizeSlabState(DEFAULT_SLAB.defaultBlockState());
        }
        try {
            ResourceLocation id = ResourceLocation.parse(serialized);
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block == null || block == Blocks.AIR) return null;
            return normalizeSlabState(block.defaultBlockState());
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable BlockState bitsandbalance$normalizeStoredSlab(@Nullable BlockState state) {
        if (state == null) return null;

        Block block = state.getBlock();
        Block slabBlock = null;

        if (block instanceof SlabBlock) {
            slabBlock = block;
        } else if (block instanceof FixedStepBlock fixed) {
            slabBlock = fixed.getSourceSlab();
        } else {
            slabBlock = StepDynamicRegistry.getSlabForStep(block);
            if (slabBlock == null) {
                Block verticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block);
                if (verticalSlab != null) {
                    slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(verticalSlab);
                }
            }
            if (slabBlock == null) {
                slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(block);
            }
        }

        if (slabBlock == null) {
            return null;
        }

        return normalizeSlabState(slabBlock.defaultBlockState());
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
