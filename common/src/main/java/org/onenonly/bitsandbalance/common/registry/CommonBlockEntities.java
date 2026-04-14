package org.onenonly.bitsandbalance.common.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;

/**
 * Cross-loader holder for block entity types that are registered by loader modules.
 *
 * NeoForge assigns these during DeferredRegister setup.
 * Fabric assigns these during direct registry registration.
 */
public final class CommonBlockEntities {
    private CommonBlockEntities() {
    }

    public static BlockEntityType<CandleBundleBlockEntity> CANDLE_BUNDLE;

    /** BE for mixed double slabs — stores two different slab identities. */
    public static BlockEntityType<MixedSlabBlockEntity> MIXED_SLAB;

    /** BE for vertical slabs — stores one or two slab identities based on blockstate. */
    public static BlockEntityType<VerticalSlabBlockEntity> VERTICAL_SLAB;

    /** BE for single step blocks — stores one slab identity. */
    public static BlockEntityType<StepBlockEntity> STEP;

    /** BE for quad step container blocks — stores up to 4 slab identities. */
    public static BlockEntityType<QuadStepBlockEntity> QUAD_STEP;

    /** BE for single vertical step blocks — stores one slab identity. */
    public static BlockEntityType<VerticalStepBlockEntity> VERTICAL_STEP;

    /** BE for quad vertical step container blocks — stores up to 4 slab identities. */
    public static BlockEntityType<QuadVerticalStepBlockEntity> QUAD_VERTICAL_STEP;
}
