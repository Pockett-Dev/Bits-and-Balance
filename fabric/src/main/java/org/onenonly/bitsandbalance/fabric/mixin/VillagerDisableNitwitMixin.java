package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.registries.Registries;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mobs: Disable Nitwits
 *
 * Prevents Nitwit villagers from spawning (configurable).
 */
@Mixin(Villager.class)
public abstract class VillagerDisableNitwitMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void bitsandbalance$disableNitwits(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!FabricMobsConfig.disableNitwits) return;

        // Match the intent of "naturally" while also catching village structure spawns.
        String reasonName = spawnReason != null ? spawnReason.name() : "";
        if (spawnReason != EntitySpawnReason.NATURAL && !"STRUCTURE".equals(reasonName) && !"CHUNK_GENERATION".equals(reasonName)) {
            return;
        }

        Villager villager = (Villager) (Object) this;
        VillagerData data = villager.getVillagerData();
        if (!data.profession().is(VillagerProfession.NITWIT)) return;

        // Convert nitwits into unemployed villagers.
        var professionRegistry = level.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION);
        var none = professionRegistry.getOrThrow(VillagerProfession.NONE);
        villager.setVillagerData(new VillagerData(data.type(), none, data.level()));
    }
}
