package org.onenonly.bitsandbalance.common.client;

//? if mc != 1.21.1
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
//? endif
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
//? else
//? if mc != 1.21.1
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
//? endif
import net.minecraft.world.level.BlockAndTintGetter;
//? endif
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabMaterialLookup;

import java.util.List;

public final class MixedSlabClientModelUtil {
    private static final long TOP_SEED_SALT = 0x9E3779B97F4A7C15L;

    private MixedSlabClientModelUtil() {
    }

    //? if mc == 1.21.1
    public static void appendParts(List<?> parts, BlockState slabState, long seed) {
    }
    //? else
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    public static void appendParts(List<BlockStateModelPart> parts, BlockState slabState, long seed) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(slabState);
        model.collectParts(RandomSource.create(seed), parts);
    }
    //? else
    public static void appendParts(List<BlockModelPart> parts, BlockState slabState, long seed) {
        BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(slabState);
        model.collectParts(RandomSource.create(seed), parts);
    }
    //? endif
    //? endif

    public static Object geometryKey(Halves halves) {
        //? if mc == 1.21.1
        return halves;
        //? else
        return new GeometryKey(halves.bottom(), halves.top());
        //? endif
    }

    public static long topSeed(long baseSeed) {
        return baseSeed ^ TOP_SEED_SALT;
    }

    public static Halves halvesFromSavedSlots(BlockAndTintGetter level, BlockPos pos) {
        BlockState[] slots = MixedSlabMaterialLookup.get(level, pos);
        if (slots == null) {
            return null;
        }
        BlockState bottom = slot(slots, 0);
        BlockState top = slot(slots, 1);
        return bottom != null && top != null ? new Halves(bottom, top) : null;
    }

    public record Halves(BlockState bottom, BlockState top) {
    }

    private static BlockState slot(BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    //? if mc != 1.21.1
    private record GeometryKey(BlockState bottom, BlockState top) {
    }
    //? endif
}
