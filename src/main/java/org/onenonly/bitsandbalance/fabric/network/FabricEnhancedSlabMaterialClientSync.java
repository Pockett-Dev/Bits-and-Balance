package org.onenonly.bitsandbalance.fabric.network;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.client.BioluminescenceLevelRendererAccess;


public final class FabricEnhancedSlabMaterialClientSync {
    private FabricEnhancedSlabMaterialClientSync() {
    }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(EnhancedSlabMaterialUpdatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    Level level = context.client().level;
                    if (level != null && !payload.dimensionId().equals(EnhancedSlabMaterialSavedData.dimensionId(level))) {
                        return;
                    }
                    EnhancedSlabMaterialSavedData.setActiveClientDimension(payload.dimensionId());
                    BlockState[] slabs = payload.toSlabs();
                    boolean changed = EnhancedSlabMaterialSavedData.updateClient(payload.dimensionId(), payload.pos(), slabs);
                    if (level != null && changed) {
                        LongOpenHashSet changedPositions = new LongOpenHashSet(1);
                        changedPositions.add(payload.pos());
                        refreshClientBlocks(level, changedPositions);
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(EnhancedSlabMaterialBulkPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    Level level = context.client().level;
                    if (level != null && !payload.dimensionId().equals(EnhancedSlabMaterialSavedData.dimensionId(level))) {
                        return;
                    }
                    EnhancedSlabMaterialSavedData.setActiveClientDimension(payload.dimensionId());
                    long[] positions = payload.positions();
                    int count = Math.min(positions == null ? 0 : positions.length, payload.slabTags() == null ? 0 : payload.slabTags().length);
                    BlockState[][] slabsByPosition = new BlockState[count][];
                    for (int i = 0; i < count; i++) {
                        slabsByPosition[i] = payload.toSlabs(i);
                    }
                    LongOpenHashSet changedPositions = EnhancedSlabMaterialSavedData.replaceClientChunkRange(
                            payload.dimensionId(),
                            payload.centerChunkX(),
                            payload.centerChunkZ(),
                            payload.chunkRadius(),
                            positions,
                            slabsByPosition);
                    if (level != null && !changedPositions.isEmpty()) {
                        refreshClientBlocks(level, changedPositions);
                    }
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Level level = client.level;
            if (level == null) {
                if (EnhancedSlabMaterialSavedData.activeClientDimension() != null) {
                    EnhancedSlabMaterialSavedData.clearClient();
                }
                return;
            }
            EnhancedSlabMaterialSavedData.setActiveClientDimension(EnhancedSlabMaterialSavedData.dimensionId(level));
        });
    }

    private static void refreshClientBlocks(Level level, LongOpenHashSet changedPositions) {
        LongOpenHashSet dirtySections = new LongOpenHashSet(Math.max(8, changedPositions.size()));
        LongIterator changedIterator = changedPositions.iterator();
        while (changedIterator.hasNext()) {
            BlockPos pos = BlockPos.of(changedIterator.nextLong());
            EnhancedSlabMaterialSavedData.hydrateClientBlockEntityAt(level, pos);
            addSection(dirtySections, pos);
            for (Direction direction : Direction.values()) {
                addSection(dirtySections, pos.relative(direction));
            }
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == level && client.levelRenderer instanceof BioluminescenceLevelRendererAccess access) {
            LongIterator sectionIterator = dirtySections.iterator();
            while (sectionIterator.hasNext()) {
                long section = sectionIterator.nextLong();
                access.bitsandbalance$scheduleChunkRebuild(
                        SectionPos.x(section),
                        SectionPos.y(section),
                        SectionPos.z(section),
                        true
                );
            }
        }
    }

    private static void addSection(LongOpenHashSet sections, BlockPos pos) {
        sections.add(SectionPos.asLong(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
        ));
    }

}
