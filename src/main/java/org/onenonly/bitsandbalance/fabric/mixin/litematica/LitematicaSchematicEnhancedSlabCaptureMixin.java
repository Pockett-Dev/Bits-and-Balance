package org.onenonly.bitsandbalance.fabric.mixin.litematica;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.compat.litematica.LitematicaEnhancedSlabCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.schematic.LitematicaSchematic", remap = false)
public abstract class LitematicaSchematicEnhancedSlabCaptureMixin {
    @Shadow(remap = false) private Map<String, Object> blockContainers;
    @Shadow(remap = false) private Map<String, Map<BlockPos, Object>> tileEntities;

    @Inject(method = "takeBlocksFromWorld", at = @At("TAIL"), remap = false, require = 0)
    private void bitsandbalance$captureStoredSlabMaterials(Level world, List<?> boxes, @Coerce Object info, CallbackInfo ci) {
        bitsandbalance$captureStoredSlabMaterialsFromBoxes(world, boxes);
    }

    @Inject(method = "takeBlocksFromWorldWithinChunk", at = @At("TAIL"), remap = false, require = 0)
    private void bitsandbalance$captureStoredSlabMaterialsWithinChunk(Level world,
                                                                      ImmutableMap<String, ?> volumes,
                                                                      ImmutableMap<String, ?> boxes,
                                                                      @Coerce Object info,
                                                                      CallbackInfo ci) {
        bitsandbalance$captureStoredSlabMaterialsFromBoxes(world, boxes.values());
    }

    private void bitsandbalance$captureStoredSlabMaterialsFromBoxes(Level world, Iterable<?> boxes) {
        if (world == null || boxes == null) {
            return;
        }

        for (Object box : boxes) {
            try {
                String regionName = bitsandbalance$boxName(box);
                Object container = blockContainers.get(regionName);
                Map<BlockPos, Object> tileEntityMap = tileEntities.get(regionName);
                if (container == null || tileEntityMap == null) {
                    continue;
                }

                BlockPos pos1 = bitsandbalance$boxPos(box, "getPos1");
                BlockPos pos2 = bitsandbalance$boxPos(box, "getPos2");
                BlockPos min = new BlockPos(
                        Math.min(pos1.getX(), pos2.getX()),
                        Math.min(pos1.getY(), pos2.getY()),
                        Math.min(pos1.getZ(), pos2.getZ())
                );
                Vec3i size = bitsandbalance$containerSize(container);
                Method get = container.getClass().getMethod("get", int.class, int.class, int.class);

                for (int y = 0; y < size.getY(); y++) {
                    for (int z = 0; z < size.getZ(); z++) {
                        for (int x = 0; x < size.getX(); x++) {
                            BlockPos rel = new BlockPos(x, y, z);
                            BlockPos worldPos = min.offset(x, y, z);
                            BlockState state = (BlockState) get.invoke(container, x, y, z);
                            CompoundTag tag = LitematicaEnhancedSlabCompat.exportTag(world, worldPos, rel, state);
                            if (tag != null) {
                                Object storedTag = LitematicaEnhancedSlabCompat.toLitematicaTag(this, tileEntityMap, tag);
                                if (storedTag != null) {
                                    tileEntityMap.put(rel, storedTag);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static String bitsandbalance$boxName(Object box) throws ReflectiveOperationException {
        Object name = box.getClass().getMethod("getName").invoke(box);
        return name == null ? "" : name.toString();
    }

    private static BlockPos bitsandbalance$boxPos(Object box, String methodName) throws ReflectiveOperationException {
        return (BlockPos) box.getClass().getMethod(methodName).invoke(box);
    }

    private static Vec3i bitsandbalance$containerSize(Object container) throws ReflectiveOperationException {
        return (Vec3i) container.getClass().getMethod("getSize").invoke(container);
    }
}
