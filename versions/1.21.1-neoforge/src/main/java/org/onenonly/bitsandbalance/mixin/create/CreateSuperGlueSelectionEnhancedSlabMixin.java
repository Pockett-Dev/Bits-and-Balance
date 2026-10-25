package org.onenonly.bitsandbalance.mixin.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHelper", remap = false)
public abstract class CreateSuperGlueSelectionEnhancedSlabMixin {
    @WrapOperation(
            method = "searchGlueGroup",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/api/contraption/BlockMovementChecks;isMovementNecessary(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"
            ),
            remap = false,
            require = 0
    )
    private static boolean bitsandbalance$treatEnhancedSlabsAsGlueBlocks(BlockState state,
                                                                         Level level,
                                                                         BlockPos pos,
                                                                         Operation<Boolean> original) {
        if (EnhancedSlabCreateCompat.supportsMovement(state)) {
            return true;
        }
        return original.call(state, level, pos);
    }

    @Inject(method = "searchGlueGroup", at = @At("RETURN"), cancellable = true, remap = false)
    private static void bitsandbalance$searchEnhancedSlabGlueGroup(Level level,
                                                                   BlockPos start,
                                                                   BlockPos target,
                                                                   boolean stickToSurfaces,
                                                                   CallbackInfoReturnable<Set<BlockPos>> cir) {
        Set<BlockPos> original = cir.getReturnValue();
        if (original != null && original.contains(target)) {
            return;
        }

        Set<BlockPos> enhancedGroup = bitsandbalance$findEnhancedSlabGlueGroup(level, start, target, stickToSurfaces);
        if (enhancedGroup != null && enhancedGroup.contains(target)) {
            cir.setReturnValue(enhancedGroup);
        }
    }

    @Unique
    private static @Nullable Set<BlockPos> bitsandbalance$findEnhancedSlabGlueGroup(Level level,
                                                                                    BlockPos start,
                                                                                    BlockPos target,
                                                                                    boolean stickToSurfaces) {
        if (level == null || start == null || target == null) {
            return null;
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> frontier = new ArrayDeque<>();
        AABB selected = new AABB(Vec3.atLowerCornerOf(start), Vec3.atLowerCornerOf(target))
                .expandTowards(1.0D, 1.0D, 1.0D);
        boolean foundEnhancedSlab = false;

        visited.add(start);
        frontier.add(start);

        while (!frontier.isEmpty()) {
            BlockPos current = frontier.remove();
            BlockState currentState = level.getBlockState(current);
            if (EnhancedSlabCreateCompat.supportsMovement(currentState)) {
                foundEnhancedSlab = true;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next)) {
                    continue;
                }

                BlockState nextState = level.getBlockState(next);
                boolean nextIsEnhancedSlab = EnhancedSlabCreateCompat.supportsMovement(nextState);

                boolean connectedBySurface = stickToSurfaces
                        && (bitsandbalance$isGlued(level, current, direction)
                        || bitsandbalance$isSideSticky(level, current, direction)
                        || bitsandbalance$isSideSticky(level, next, direction.getOpposite()));
                if (!selected.contains(Vec3.atCenterOf(next)) && !connectedBySurface) {
                    continue;
                }

                if (!bitsandbalance$isGlueSearchBlock(level, next, nextState)
                        || !bitsandbalance$isGlueFaceValid(level, current, currentState, direction)
                        || !bitsandbalance$isGlueFaceValid(level, next, nextState, direction.getOpposite())) {
                    continue;
                }

                visited.add(next);
                frontier.add(next);
                if (nextIsEnhancedSlab) {
                    foundEnhancedSlab = true;
                }
            }
        }

        if (!foundEnhancedSlab || !visited.contains(target) || visited.size() < 2) {
            return null;
        }
        return visited;
    }

    @Unique
    private static boolean bitsandbalance$isGlueSearchBlock(Level level, BlockPos pos, BlockState state) {
        return EnhancedSlabCreateCompat.supportsMovement(state)
                || bitsandbalance$isMovementNecessary(state, level, pos);
    }

    @Unique
    private static boolean bitsandbalance$isGlueFaceValid(Level level,
                                                         BlockPos pos,
                                                         BlockState state,
                                                         Direction direction) {
        return EnhancedSlabCreateCompat.supportsMovement(state)
                || bitsandbalance$isValidFace(level, pos, direction);
    }

    @Unique
    private static boolean bitsandbalance$isMovementNecessary(BlockState state, Level level, BlockPos pos) {
        return bitsandbalance$invokeCreateBoolean(
                "com.simibubi.create.api.contraption.BlockMovementChecks",
                "isMovementNecessary",
                new Class<?>[] { BlockState.class, Level.class, BlockPos.class },
                state,
                level,
                pos
        );
    }

    @Unique
    private static boolean bitsandbalance$isValidFace(Level level, BlockPos pos, Direction direction) {
        return bitsandbalance$invokeCreateBoolean(
                "com.simibubi.create.content.contraptions.glue.SuperGlueEntity",
                "isValidFace",
                new Class<?>[] { Level.class, BlockPos.class, Direction.class },
                level,
                pos,
                direction
        );
    }

    @Unique
    private static boolean bitsandbalance$isSideSticky(Level level, BlockPos pos, Direction direction) {
        return bitsandbalance$invokeCreateBoolean(
                "com.simibubi.create.content.contraptions.glue.SuperGlueEntity",
                "isSideSticky",
                new Class<?>[] { Level.class, BlockPos.class, Direction.class },
                level,
                pos,
                direction
        );
    }

    @Unique
    private static boolean bitsandbalance$isGlued(LevelAccessor level, BlockPos pos, Direction direction) {
        return bitsandbalance$invokeCreateBoolean(
                "com.simibubi.create.content.contraptions.glue.SuperGlueEntity",
                "isGlued",
                new Class<?>[] { LevelAccessor.class, BlockPos.class, Direction.class, Set.class },
                level,
                pos,
                direction,
                null
        );
    }

    @Unique
    private static boolean bitsandbalance$invokeCreateBoolean(String className,
                                                             String methodName,
                                                             Class<?>[] parameterTypes,
                                                             Object... args) {
        try {
            Class<?> type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            Method method = type.getMethod(methodName, parameterTypes);
            return Boolean.TRUE.equals(method.invoke(null, args));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
