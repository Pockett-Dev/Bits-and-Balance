package org.onenonly.bitsandbalance.mobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.onenonly.bitsandbalance.Config;

/**
 * Mobs: Disable Nitwits (NeoForge)
 *
 * Prevents Nitwit villagers from spawning naturally by converting them into
 * unemployed villagers during spawn finalization.
 */
public final class VillagerDisableNitwitHandler {

    private VillagerDisableNitwitHandler() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!Config.disableNitwits) {
            return;
        }

        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        // Match the intent of "naturally" while also catching village structure spawns.
        MobSpawnType spawnReason = event.getSpawnType();
        String reasonName = spawnReason != null ? spawnReason.name() : "";
        if (spawnReason != MobSpawnType.NATURAL
                && !"STRUCTURE".equals(reasonName)
                && !"CHUNK_GENERATION".equals(reasonName)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        VillagerData data = villager.getVillagerData();
        if (data.getProfession() != VillagerProfession.NITWIT) {
            return;
        }

        villager.setVillagerData(new VillagerData(data.getType(), VillagerProfession.NONE, data.getLevel()));
    }

    private static MobSpawnType tryGetSpawnReason(Entity entity) {
        try {
            java.lang.reflect.Method method = entity.getClass().getMethod("getEntitySpawnReason");
            Object value = method.invoke(entity);
            if (value instanceof MobSpawnType reason) {
                return reason;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Fallback for cases where FinalizeSpawnEvent fires before the villager's profession is finalized.
     * Runs when the entity is actually added to the world.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!Config.disableNitwits) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.loadedFromDisk()) {
            return;
        }
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        // Defer to next server tick so villager data is fully initialized.
        if (event.getLevel() instanceof ServerLevel level) {
            level.getServer().execute(() -> {
                try {
                    if (!Config.disableNitwits) return;
                    if (!villager.isAlive()) return;

                    MobSpawnType spawnReason = tryGetSpawnReason(villager);
                    String reasonName = spawnReason != null ? spawnReason.name() : "";
                    if (spawnReason != null
                            && spawnReason != MobSpawnType.NATURAL
                            && !"STRUCTURE".equals(reasonName)
                            && !"CHUNK_GENERATION".equals(reasonName)) {
                        return;
                    }

                    VillagerData data = villager.getVillagerData();
                    if (data.getProfession() != VillagerProfession.NITWIT) return;

                    villager.setVillagerData(new VillagerData(data.getType(), VillagerProfession.NONE, data.getLevel()));
                } catch (Throwable ignored) {
                }
            });
        }
    }
}
