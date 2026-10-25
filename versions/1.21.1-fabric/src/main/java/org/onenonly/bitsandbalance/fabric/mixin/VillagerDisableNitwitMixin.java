package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.ServerLevelAccessor;
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
            MobSpawnType spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!FabricMobsConfig.disableNitwits) return;

        String reasonName = spawnReason != null ? spawnReason.name() : "";
        if (spawnReason != MobSpawnType.NATURAL && !"STRUCTURE".equals(reasonName) && !"CHUNK_GENERATION".equals(reasonName)) {
            return;
        }

        Villager villager = (Villager) (Object) this;
        VillagerData data = villager.getVillagerData();
        if (data.getProfession() != VillagerProfession.NITWIT) return;

        villager.setVillagerData(new VillagerData(data.getType(), VillagerProfession.NONE, data.getLevel()));
    }
}