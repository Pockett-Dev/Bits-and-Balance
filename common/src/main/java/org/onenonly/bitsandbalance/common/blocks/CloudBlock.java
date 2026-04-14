package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cloud block used by Bottle of Cloud.
 *
 * <p>When {@link #TEMPORARY} is true, the block exists physically (so it can be
 * broken) but does not render. Rendering is provided by the fading display entity.
 * When false, it behaves and renders like a normal block (player-placeable).
 *
 * <p>Entities standing on a cloud block gradually sink 50% in over ~1 second
 * (the collision surface descends from block top to 50% height). Jump height
 * scales with the current sink depth via a {@link Attributes#JUMP_STRENGTH}
 * attribute modifier, reaching 1.75× normal height at full depth.
 */
public final class CloudBlock extends Block {
    public static final BooleanProperty TEMPORARY = BooleanProperty.create("temporary");

    // -------------------------------------------------------------------------
    // Sinking configuration
    // -------------------------------------------------------------------------

    /** Number of discrete steps from surface to max depth. 1 step/tick → ~1 second to full depth. */
    private static final int SINK_STEPS = 20;

    /** Maximum sink fraction of block height (50%). */
    private static final float MAX_SINK_DEPTH = 0.50f;

    /**
     * Pre-computed collision shapes for each sink step.
     * Step 0 ≈ flush with block top (15.999/16), Step SINK_STEPS = 50% sunk (top at 8/16).
     */
    private static final VoxelShape[] SINK_SHAPES = new VoxelShape[SINK_STEPS + 1];

    static {
        for (int i = 0; i <= SINK_STEPS; i++) {
            double topHeight = (1.0 - (i * (double) MAX_SINK_DEPTH / SINK_STEPS)) * 16.0;
            // Cap just below the block ceiling so the entity's feet always sit
            // *inside* the block's voxel space (strictly < Y+1), which is
            // required for entityInside to fire and advance the sink step.
            topHeight = Math.min(topHeight, 15.999);
            SINK_SHAPES[i] = Block.box(0, 0, 0, 16, topHeight, 16);
        }
    }

    /** Fully-sunk shape: top at 50% height (8/16). Used as fallback for non-entity queries. */
    private static final VoxelShape CLOUD_FULL_SINK_SHAPE = SINK_SHAPES[SINK_STEPS];

    /**
     * Per-entity sink progress (step index 0–SINK_STEPS), maintained on both
     * client and server so each side's physics see the same collision shape.
     */
    private static final ConcurrentHashMap<UUID, Integer> SINK_STEP = new ConcurrentHashMap<>();

    /**
     * Last game tick when sink progress was advanced for each entity.
     * Prevents multiple {@code entityInside} calls in the same tick (from
     * overlapping adjacent cloud blocks) from causing jittery/non-uniform sink speed.
     */
    private static final ConcurrentHashMap<UUID, Long> LAST_SINK_UPDATE_TICK = new ConcurrentHashMap<>();

    /**
     * Last game tick when an entity touched any cloud block.
     * Used to hard-clear jump/sink state the moment cloud contact stops.
     */
    private static final ConcurrentHashMap<UUID, Long> LAST_CLOUD_TOUCH_TICK = new ConcurrentHashMap<>();

    /**
     * ID for the transient {@link Attributes#JUMP_STRENGTH} modifier applied
     * while an entity is sinking in a cloud block.
     *
     * <p>Using a transient modifier means it is removed automatically when the
     * entity is discarded, and never persisted to NBT.
     */
    private static final Identifier CLOUD_JUMP_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("bitsandbalance", "cloud_sink_jump");

    /**
     * Maximum ADD_MULTIPLIED_TOTAL value added to JUMP_STRENGTH at full sink depth.
     * 0.75 → total jump strength = base × (1 + 0.75) = 1.75×.
     */
    private static final double MAX_JUMP_MULTIPLIER = 0.75;

    public CloudBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TEMPORARY, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TEMPORARY);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(TEMPORARY) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        // Clouds fully cushion landings: landing here should never deal fall damage.
        // Intentionally do not call super.fallOn(...), which is where vanilla blocks
        // apply normal fall damage handling.
    }

    // -------------------------------------------------------------------------
    // Face culling: adjacent cloud blocks suppress shared faces so a group of
    // touching clouds renders as one seamless blob (same technique as glass).
    // -------------------------------------------------------------------------

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        // Only suppress inner faces when BOTH touching blocks are rendered cloud blocks.
        // An invisible (TEMPORARY) cloud block must not poke holes in a visible neighbor.
        if (adjacentBlockState.is(this)
                && !state.getValue(TEMPORARY)
                && !adjacentBlockState.getValue(TEMPORARY)) {
            return true;
        }
        return super.skipRendering(state, adjacentBlockState, side);
    }

    // -------------------------------------------------------------------------
    // Sinking: the collision surface descends from block top to 50% depth over
    // ~1 second (SINK_STEPS ticks), one step per call to entityInside.
    // The shape is looked up per-entity so each entity sinks independently.
    // -------------------------------------------------------------------------

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ecc) {
            Entity entity = ecc.getEntity();
            if (entity != null) {
                UUID id = entity.getUUID();
                // Seed the initial sink step from fall velocity on first contact.
                // getDeltaMovement().y is called here (before physics resolves) so it
                // still holds the true downward speed, letting the entity skip the
                // surface and land immediately at the fall-appropriate depth.
                if (!SINK_STEP.containsKey(id)) {
                    // Falling 1.5+ blocks → land immediately at full depth.
                    // Less than that (step-off, gentle drop) → start at surface and sink gradually.
                    if (entity.fallDistance >= 1.5f) {
                        SINK_STEP.put(id, SINK_STEPS);
                    }
                }
                int step = SINK_STEP.getOrDefault(id, 0);
                return SINK_SHAPES[step];
            }
        }
        // Non-entity queries (pathfinding, pistons, etc.): return the fully-sunk
        // shape so walkability checks still see a solid floor.
        return CLOUD_FULL_SINK_SHAPE;
    }

    // -------------------------------------------------------------------------
    // Jump height scaling via JUMP_STRENGTH attribute modifier.
    // Also advances the per-entity sink step each tick.
    // NOTE: both client and server advance SINK_STEP — the local player's
    // physics runs client-side, so the client must see the shrinking shape too.
    // Attribute modification (server-only) syncs to the client automatically.
    // -------------------------------------------------------------------------

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean intersects) {
        UUID id = entity.getUUID();
        LAST_CLOUD_TOUCH_TICK.put(id, level.getGameTime());

        // If the entity's feet are at or above block top, they are on the surface —
        // clear any existing sink so the next landing starts fresh.
        double blockTopY = pos.getY() + 1.0;
        if (entity.getY() >= blockTopY) {
            SINK_STEP.remove(id);
            LAST_SINK_UPDATE_TICK.remove(id);
            if (!level.isClientSide() && entity instanceof LivingEntity living) {
                removeJumpModifier(living);
            }
            return;
        }

        // Advance at most once per game tick so crossing/overlapping adjacent cloud
        // blocks does not accelerate or jitter sink progression.
        long gameTime = level.getGameTime();
        long lastUpdate = LAST_SINK_UPDATE_TICK.getOrDefault(id, Long.MIN_VALUE);
        int step = SINK_STEP.getOrDefault(id, 0);
        if (lastUpdate != gameTime) {
            step = Math.min(SINK_STEPS, step + 1);
            SINK_STEP.put(id, step);
            LAST_SINK_UPDATE_TICK.put(id, gameTime);
        }

        // Apply a scaling JUMP_STRENGTH modifier on the server (synced to client).
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            AttributeInstance attr = living.getAttribute(Attributes.JUMP_STRENGTH);
            if (attr != null) {
                // Multiplier scales linearly: 0 at step 0 → MAX_JUMP_MULTIPLIER at full depth.
                double multiplier = (step * MAX_JUMP_MULTIPLIER) / SINK_STEPS;
                attr.addOrUpdateTransientModifier(new AttributeModifier(
                        CLOUD_JUMP_MODIFIER_ID,
                        multiplier,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    /** Removes the cloud jump modifier from a living entity if present. */
    private static void removeJumpModifier(LivingEntity living) {
        AttributeInstance attr = living.getAttribute(Attributes.JUMP_STRENGTH);
        if (attr != null) {
            attr.removeModifier(CLOUD_JUMP_MODIFIER_ID);
        }
    }

    /**
     * Removes all cloud-related state if the entity did not touch any cloud block this tick.
     * Call once per-tick for living entities to prevent stale jump modifiers from persisting.
     */
    public static void cleanupEntityStateIfNotTouchingCloud(LivingEntity living) {
        Level level = living.level();
        UUID id = living.getUUID();
        long gameTime = level.getGameTime();
        long lastTouch = LAST_CLOUD_TOUCH_TICK.getOrDefault(id, Long.MIN_VALUE);
        if (lastTouch == gameTime) {
            return;
        }

        SINK_STEP.remove(id);
        LAST_SINK_UPDATE_TICK.remove(id);
        LAST_CLOUD_TOUCH_TICK.remove(id);

        if (!level.isClientSide()) {
            removeJumpModifier(living);
        }
    }
}
