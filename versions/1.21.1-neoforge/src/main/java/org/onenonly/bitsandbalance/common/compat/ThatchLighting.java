package org.onenonly.bitsandbalance.common.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;

/** Blueprint's airy Thatch lighting also applies to BaB shapes made entirely of Thatch. */
public final class ThatchLighting {
    private static final ResourceLocation THATCH_SLAB =
            ResourceLocation.fromNamespaceAndPath("upgrade_aquatic", "beachgrass_thatch_slab");
    private static final ClassValue<Boolean> RENDER_VIEWS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return isRenderView(type);
        }
    };

    private ThatchLighting() {
    }

    public static boolean containsThatch(BlockState[] slots) {
        if (slots != null) {
            for (BlockState slot : slots) {
                if (slot != null && isThatch(slot.getBlock())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean usesThatchLighting(BlockState state, BlockGetter level, BlockPos pos) {
        Block block = state.getBlock();
        if (!(block instanceof MixedSlabBlock || block instanceof VerticalSlabBlock
                || block instanceof StepBlock || block instanceof QuadStepBlock
                || block instanceof VerticalStepBlock || block instanceof QuadVerticalStepBlock)) {
            return false;
        }
        Block source = EnhancedSlabItemSupport.sourceSlabForBlock(block);
        if (source != null) {
            return isThatch(source);
        }
        if (level == null || level == EmptyBlockGetter.INSTANCE) {
            return false;
        }
        // Light engines query chunks, while terrain builders query cached render regions.
        // Resolve both without loading a server chunk or referring to client-only classes.
        if (level instanceof LevelChunk chunk) {
            level = chunk.getLevel();
        }
        BlockState[] slots;
        if (level instanceof Level actualLevel) {
            slots = EnhancedSlabMaterialSavedData.peek(actualLevel, pos.asLong());
        } else {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MixedSlabBlockEntity mixed) {
                slots = new BlockState[]{mixed.getBottomSlab(), mixed.getTopSlab()};
            } else if (blockEntity instanceof VerticalSlabBlockEntity vertical) {
                slots = new BlockState[]{vertical.getFacingSlab(), vertical.getOppositeSlab()};
            } else if (blockEntity instanceof QuadStepBlockEntity quad) {
                slots = quad.getAllSlabs();
            } else if (blockEntity instanceof QuadVerticalStepBlockEntity quad) {
                slots = quad.getAllSlabs();
            } else if (blockEntity instanceof StepBlockEntity step) {
                slots = new BlockState[]{step.getSlabState()};
            } else if (blockEntity instanceof VerticalStepBlockEntity step) {
                slots = new BlockState[]{step.getSlabState()};
            } else {
                slots = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
            }
            if (slots == null && RENDER_VIEWS.get(level.getClass())) {
                // Legacy Vertical Slabs have no portable block entity; terrain views
                // use the same active-dimension material snapshot as their renderer.
                slots = EnhancedSlabMaterialSavedData.getClient(pos.asLong());
            }
        }
        if (slots == null) {
            return false;
        }
        boolean any = false;
        for (BlockState slot : slots) {
            if (slot == null || slot.isAir()) {
                continue;
            }
            if (!isThatch(slot.getBlock())) {
                return false;
            }
            any = true;
        }
        return any;
    }

    private static boolean isThatch(Block block) {
        return THATCH_SLAB.equals(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static boolean isRenderView(Class<?> type) {
        if (type == null) return false;
        if (type.getName().equals("net.minecraft.world.level.BlockAndTintGetter")) return true;
        for (Class<?> implemented : type.getInterfaces()) {
            if (isRenderView(implemented)) return true;
        }
        return isRenderView(type.getSuperclass());
    }
}
