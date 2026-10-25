package org.onenonly.bitsandbalance.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabParticleStateCache;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

/**
 * Tweaks: Enhanced Slab Behavior — Vertical Step Block Entity
 *
 * Stores the identity of the single vertical slab this vertical step represents.
 */
public class VerticalStepBlockEntity extends BlockEntity {

    private static final String TAG_SLAB = "Slab";
    private static final Block DEFAULT_SLAB = Blocks.OAK_SLAB;

    private @Nullable BlockState slabState;

    public VerticalStepBlockEntity(BlockPos pos, BlockState blockState) {
        super(CommonBlockEntities.VERTICAL_STEP, pos, blockState);
        Block owningBlock = blockState.getBlock();
        Block sourceVerticalSlab = null;
        try {
            if (owningBlock instanceof FixedVerticalStepBlock fixed) {
                sourceVerticalSlab = fixed.getSourceVerticalSlab();
            } else {
                sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(owningBlock);
            }
        } catch (Throwable ignored) {
        }

        Block slabBlock = DEFAULT_SLAB;
        if (sourceVerticalSlab != null) {
            Block mapped = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
            slabBlock = mapped != null ? mapped : sourceVerticalSlab;
        }

        BlockState init = slabBlock.defaultBlockState();
        if (init.hasProperty(SlabBlock.TYPE)) {
            init = init.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (init.hasProperty(SlabBlock.WATERLOGGED)) {
            init = init.setValue(SlabBlock.WATERLOGGED, false);
        }
        this.slabState = init;
    }

    public @Nullable BlockState getSlabState() {
        return slabState;
    }

    public void setSlabState(BlockState state) {
        this.slabState = normalizeSlabState(state);
        EnhancedSlabParticleStateCache.remember(level, worldPosition, this.slabState);
        setChanged();
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

    // ── Serialization ─────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString(TAG_SLAB, blockId(slabState));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        slabState = resolveSlabState(input.getStringOr(TAG_SLAB, ""));
        EnhancedSlabParticleStateCache.remember(level, worldPosition, slabState);
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

    private static String blockId(BlockState state) {
        if (state == null) return BuiltInRegistries.BLOCK.getKey(DEFAULT_SLAB).toString();
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static BlockState resolveSlabState(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return DEFAULT_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        try {
            Identifier id = Identifier.parse(serialized);
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            return normalizeSlabState(block != null ? block.defaultBlockState() : null);
        } catch (Exception e) {
            return DEFAULT_SLAB.defaultBlockState();
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
