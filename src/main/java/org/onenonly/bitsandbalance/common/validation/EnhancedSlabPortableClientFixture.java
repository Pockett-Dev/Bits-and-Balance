package org.onenonly.bitsandbalance.common.validation;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

//? if mc == 26.3
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.onenonly.bitsandbalance.common.blockentity.*;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
//? endif

/** Read-only material/hover regression in a marked COPY of the affected old world. */
public final class EnhancedSlabPortableClientFixture {
    public record Result(boolean passed, int checked, List<String> details) { }
    private static int ticks;
    private static Result result;
    private EnhancedSlabPortableClientFixture() { }

    public static Result tick(Minecraft client) {
        //? if mc == 26.3
        try {
            var server = client.getSingleplayerServer();
            if (server == null || !server.getWorldPath(LevelResource.ROOT).toString().toLowerCase().contains("scratch")
                    || !Files.isRegularFile(server.getWorldPath(LevelResource.ROOT).resolve(".bnb-mutable-fixture")))
                return new Result(false, 0, List.of("requires-marked-scratch-copy"));
            ServerLevel level = server.getLevel(client.level.dimension());
            if (ticks++ == 0) {
                server.submit(() -> {
                    var player = server.getPlayerList().getPlayer(client.player.getUUID());
                    player.teleportTo(-664.5, 69, -304.5);
                    return true;
                }).get(10, TimeUnit.SECONDS);
                return null;
            }
            if (ticks < 140) return null;
            if (result != null) return ticks > 200 ? result : null;
            var records = server.submit(() -> EnhancedSlabMaterialSavedData.get(level)
                    .snapshot(EnhancedSlabMaterialSavedData.dimensionId(level)).stream()
                    .filter(entry -> {
                        BlockState state = level.getBlockState(BlockPos.of(entry.pos()));
                        return state.is(CommonBlocks.MIXED_SLAB) || state.is(CommonBlocks.QUAD_STEP)
                                || state.is(CommonBlocks.QUAD_VERTICAL_STEP);
                    }).toList()).get(20, TimeUnit.SECONDS);
            List<String> details = new ArrayList<>();
            boolean passed = records.size() == 8;
            int checked = 0;
            for (var entry : records) {
                BlockPos pos = BlockPos.of(entry.pos());
                var before = EnhancedSlabMaterialSavedData.getClientView(client.level, entry.pos());
                boolean initial = before != null && before.matches(entry.slabs());
                // Reproduce the hover path: a chunk has material packets but no attached BE.
                client.level.removeBlockEntity(pos);
                BlockEntity entity = client.level.getBlockEntity(pos);
                BlockState owner = client.level.getBlockState(pos);
                boolean shape = !owner.getShape(client.level, pos).isEmpty()
                        && !owner.getCollisionShape(client.level, pos).isEmpty();
                var after = EnhancedSlabMaterialSavedData.getClientView(client.level, entry.pos());
                boolean intact = after != null && after.matches(entry.slabs());
                BlockState[] mirrored = slots(entity);
                boolean mirror = after != null && after.matches(mirrored);
                // Exercise a subsequent packet changing occupied slots, then restore its data.
                BlockState[] replacement = entry.slabs().clone();
                replacement[0] = net.minecraft.world.level.block.Blocks.BIRCH_SLAB.defaultBlockState();
                EnhancedSlabMaterialSavedData.rememberClient(client.level, entry.pos(), replacement);
                var updated = EnhancedSlabMaterialSavedData.getClientView(client.level, entry.pos());
                boolean refresh = updated != null && updated.matches(slots(entity));
                EnhancedSlabMaterialSavedData.rememberClient(client.level, entry.pos(), entry.slabs());
                passed &= initial && shape && intact && mirror && refresh;
                checked += 5;
                details.add(pos.toShortString() + ":initial=" + initial + ",shape=" + shape
                        + ",hoverIntact=" + intact + ",mirror=" + mirror + ",packetRefresh=" + refresh
                        + ",slots=" + Arrays.toString(mirrored));
            }
            client.player.setPos(-664.5, 69, -304.5);
            client.player.setYRot(0); client.player.setXRot(25);
            client.player.yRotO = 0; client.player.xRotO = 25;
            String screenshot = "bnb-portable-client-upgrade.png";
            Screenshot.grab(client.gameDirectory, screenshot, client.gameRenderer.mainRenderTarget(), 1, message -> { });
            details.add("screenshot=" + screenshot);
            result = new Result(passed, checked, details);
            return null;
        } catch (Throwable error) {
            return new Result(false, 0, List.of(error.toString()));
        }
        //? else
        return new Result(false, 0, List.of("fixture-requires-26.3"));
        //? endif
    }

    //? if mc == 26.3
    private static BlockState[] slots(BlockEntity entity) {
        if (entity instanceof MixedSlabBlockEntity mixed)
            return new BlockState[]{mixed.getBottomSlab(), mixed.getTopSlab(), null, null};
        if (entity instanceof QuadStepBlockEntity quad) return quad.getAllSlabs();
        if (entity instanceof QuadVerticalStepBlockEntity quad) return quad.getAllSlabs();
        return null;
    }
    //? endif
}
