package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

/**
 * Tweaks: Enhanced Slab Behavior — Quad Vertical Step Block Entity
 *
 * Stores up to 4 vertical-slab states, one per quadrant in a
 * {@link org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock}.
 * Quadrant indices match {@link org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock}:
 * <ul>
 *   <li>0 — north-west  (X 0-8,  Z 0-8)</li>
 *   <li>1 — north-east  (X 8-16, Z 0-8)</li>
 *   <li>2 — south-west  (X 0-8,  Z 8-16)</li>
 *   <li>3 — south-east  (X 8-16, Z 8-16)</li>
 * </ul>
 */
public class QuadVerticalStepBlockEntity extends BlockEntity {

    private static final String[] SLOT_TAGS = { "Slot0", "Slot1", "Slot2", "Slot3" };
    private static final Block DEFAULT_SLAB = Blocks.OAK_SLAB;

    /** 4-element array; {@code null} means the quadrant is empty. */
    private final @Nullable BlockState[] slabs = new BlockState[4];

    public QuadVerticalStepBlockEntity(BlockPos pos, BlockState state) {
        super(CommonBlockEntities.QUAD_VERTICAL_STEP, pos, state);
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public @Nullable BlockState getSlabAt(int index) {
        if (index < 0 || index >= 4) return null;
        return slabs[index];
    }

    public void setSlabAt(int index, @Nullable BlockState state) {
        if (index < 0 || index >= 4) return;
        BlockState normalized = bitsandbalance$normalizeStoredSlab(state);
        slabs[index] = normalized;
        setChanged();
        sync();
    }

    /** Sets a slot without emitting any network sync. */
    public void bitsandbalance$setSlabAtSilent(int index, @Nullable BlockState state) {
        if (index < 0 || index >= 4) return;
        BlockState normalized = bitsandbalance$normalizeStoredSlab(state);
        slabs[index] = normalized;
        setChanged();
    }

    /** Returns a defensive copy of the full slots array. */
    public BlockState[] getAllSlabs() {
        return slabs.clone();
    }

    /** Bulk-sets all 4 slots. Fires a single sync after. */
    public void setAllSlabs(BlockState[] incoming) {
        for (int i = 0; i < 4; i++) {
            BlockState original = (incoming != null && i < incoming.length) ? incoming[i] : null;
            BlockState normalized = original != null ? bitsandbalance$normalizeStoredSlab(original) : null;
            slabs[i] = normalized;
        }
        setChanged();
        sync();
    }

    /** Bulk-set without emitting any network sync. */
    public void bitsandbalance$setAllSlabsSilent(BlockState[] incoming) {
        for (int i = 0; i < 4; i++) {
            BlockState original = (incoming != null && i < incoming.length) ? incoming[i] : null;
            BlockState normalized = original != null ? bitsandbalance$normalizeStoredSlab(original) : null;
            slabs[i] = normalized;
        }
        setChanged();
    }

    /** Returns the number of non-null slots. */
    public int getCount() {
        int n = 0;
        for (BlockState s : slabs) if (s != null) n++;
        return n;
    }

    /** Returns the number of occupied slots equal to the supplied stored slab state. */
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

    /**
     * Returns {@code true} when this quad contains more than one distinct stored slab state.
     * Mixed quads should always use kneeslab-style targeted breaking, even when the player is
     * not crouching.
     */
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

    /**
     * Forces an immediate client sync for this block entity.
     *
     * Use sparingly; most callers should prefer {@link #setSlabAt(int, BlockState)} which
     * syncs automatically.
     */
    public void bitsandbalance$syncToClients() {
        sync();
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 4; i++) {
            if (slabs[i] != null) {
                output.putString(SLOT_TAGS[i], blockId(slabs[i]));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 4; i++) {
            String raw = input.getStringOr(SLOT_TAGS[i], "");
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String blockId(@Nullable BlockState state) {
        if (state == null) return BuiltInRegistries.BLOCK.getKey(DEFAULT_SLAB).toString();
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static @Nullable BlockState resolveSlabState(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return normalizeSlabState(DEFAULT_SLAB.defaultBlockState());
        }
        try {
            Identifier id = Identifier.parse(serialized);
            Block block = BuiltInRegistries.BLOCK.getValue(id);
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
        } else if (block instanceof FixedVerticalStepBlock fixed) {
            Block sourceVertical = fixed.getSourceVerticalSlab();
            slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVertical);
            if (slabBlock == null) slabBlock = sourceVertical;
        } else {
            Block sourceVertical = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block);
            if (sourceVertical == null) {
                sourceVertical = VerticalSlabDynamicRegistry.getVerticalForSlab(block);
            }
            if (sourceVertical != null) {
                slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVertical);
                if (slabBlock == null) slabBlock = sourceVertical;
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
