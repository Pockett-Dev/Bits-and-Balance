package org.onenonly.bitsandbalance.mobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
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
        EntitySpawnReason spawnReason = event.getSpawnType();
        String reasonName = spawnReason != null ? spawnReason.name() : "";
        if (spawnReason != EntitySpawnReason.NATURAL
                && !"STRUCTURE".equals(reasonName)
                && !"CHUNK_GENERATION".equals(reasonName)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        VillagerData data = villager.getVillagerData();
        if (!data.profession().is(VillagerProfession.NITWIT)) {
            return;
        }

        var professionRegistry = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION);
        var none = professionRegistry.getOrThrow(VillagerProfession.NONE);
        villager.setVillagerData(new VillagerData(data.type(), none, data.level()));
    }

    private static EntitySpawnReason tryGetSpawnReason(Entity entity) {
        try {
            // In newer MC, Entity stores a spawn reason that can be queried.
            java.lang.reflect.Method method = entity.getClass().getMethod("getEntitySpawnReason");
            Object value = method.invoke(entity);
            if (value instanceof EntitySpawnReason reason) {
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

                    EntitySpawnReason spawnReason = tryGetSpawnReason(villager);
                    String reasonName = spawnReason != null ? spawnReason.name() : "";
                    if (spawnReason != null
                            && spawnReason != EntitySpawnReason.NATURAL
                            && !"STRUCTURE".equals(reasonName)
                            && !"CHUNK_GENERATION".equals(reasonName)) {
                        return;
                    }

                    VillagerData data = villager.getVillagerData();
                    if (!data.profession().is(VillagerProfession.NITWIT)) return;

                    var professionRegistry = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION);
                    var none = professionRegistry.getOrThrow(VillagerProfession.NONE);
                    villager.setVillagerData(new VillagerData(data.type(), none, data.level()));
                } catch (Throwable ignored) {
                }
            });
        }
    }
}
