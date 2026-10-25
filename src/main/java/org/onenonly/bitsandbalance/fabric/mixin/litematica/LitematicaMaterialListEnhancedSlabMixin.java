package org.onenonly.bitsandbalance.fabric.mixin.litematica;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.compat.litematica.LitematicaEnhancedSlabCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.materials.MaterialListUtils", remap = false)
public abstract class LitematicaMaterialListEnhancedSlabMixin {
    @Inject(
            method = "createMaterialListFor(Lfi/dy/masa/litematica/schematic/LitematicaSchematic;Ljava/util/Collection;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void bitsandbalance$countStoredSlabMaterials(@Coerce Object schematic,
                                                                Collection<String> subRegions,
                                                                CallbackInfoReturnable<List<Object>> cir) {
        if (schematic == null || subRegions == null) {
            return;
        }

        try {
            Object2IntOpenHashMap<BlockState> vanillaCounts = new Object2IntOpenHashMap<>();
            List<MaterialCount> enhancedCounts = new ArrayList<>();
            Method getContainer = schematic.getClass().getMethod("getSubRegionContainer", String.class);
            Method getBlockEntityMap = schematic.getClass().getMethod("getBlockEntityMapForRegion", String.class);

            boolean foundEnhanced = false;
            for (String regionName : subRegions) {
                Object container = getContainer.invoke(schematic, regionName);
                if (container == null) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<BlockPos, ?> blockEntityMap = (Map<BlockPos, ?>) getBlockEntityMap.invoke(schematic, regionName);
                Vec3i size = (Vec3i) container.getClass().getMethod("getSize").invoke(container);
                Method get = container.getClass().getMethod("get", int.class, int.class, int.class);

                for (int y = 0; y < size.getY(); y++) {
                    for (int z = 0; z < size.getZ(); z++) {
                        for (int x = 0; x < size.getX(); x++) {
                            BlockState state = (BlockState) get.invoke(container, x, y, z);
                            Object tag = blockEntityMap == null ? null : blockEntityMap.get(new BlockPos(x, y, z));
                            if (LitematicaEnhancedSlabCompat.isCompatTag(tag) && LitematicaEnhancedSlabCompat.handles(state)) {
                                foundEnhanced = true;
                                for (ItemStack stack : LitematicaEnhancedSlabCompat.materialItems(state, LitematicaEnhancedSlabCompat.slotsFromTag(tag))) {
                                    addEnhancedCount(enhancedCounts, stack);
                                }
                            } else {
                                vanillaCounts.addTo(state, 1);
                            }
                        }
                    }
                }
            }

            if (!foundEnhanced) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft == null ? null : minecraft.player;
            List<Object> entries = vanillaMaterialEntries(vanillaCounts, player);
            for (MaterialCount count : enhancedCounts) {
                entries.add(materialListEntry(count.stack.copy(), count.count, count.count, 0, availableCount(player, count.stack)));
            }
            cir.setReturnValue(entries);
        } catch (Throwable ignored) {
        }
    }

    private static List<Object> vanillaMaterialEntries(Object2IntOpenHashMap<BlockState> vanillaCounts, Player player) throws ReflectiveOperationException {
        Class<?> materialListUtils = Class.forName("fi.dy.masa.litematica.materials.MaterialListUtils");
        Method getMaterialList = materialListUtils.getMethod(
                "getMaterialList",
                Object2IntOpenHashMap.class,
                Object2IntOpenHashMap.class,
                Object2IntOpenHashMap.class,
                Player.class
        );
        @SuppressWarnings("unchecked")
        List<Object> entries = (List<Object>) getMaterialList.invoke(
                null,
                vanillaCounts,
                vanillaCounts.clone(),
                new Object2IntOpenHashMap<>(),
                player
        );
        return new ArrayList<>(entries);
    }

    private static Object materialListEntry(ItemStack stack,
                                            int countTotal,
                                            int countMissing,
                                            int countMismatched,
                                            int countAvailable) throws ReflectiveOperationException {
        Class<?> entryClass = Class.forName("fi.dy.masa.litematica.materials.MaterialListEntry");
        Constructor<?> constructor = entryClass.getConstructor(ItemStack.class, int.class, int.class, int.class, int.class);
        return constructor.newInstance(stack, countTotal, countMissing, countMismatched, countAvailable);
    }

    private static void addEnhancedCount(List<MaterialCount> counts, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        for (MaterialCount count : counts) {
            if (ItemStack.isSameItemSameComponents(count.stack, stack)) {
                count.count += stack.getCount();
                return;
            }
        }
        counts.add(new MaterialCount(stack.copy(), stack.getCount()));
    }

    private static int availableCount(Player player, ItemStack wanted) {
        if (player == null || wanted.isEmpty()) {
            return 0;
        }

        int total = 0;
        Container inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, wanted)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static final class MaterialCount {
        private final ItemStack stack;
        private int count;

        private MaterialCount(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }
}
