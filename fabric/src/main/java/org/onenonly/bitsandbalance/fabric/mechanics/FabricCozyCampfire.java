package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;

/**
 * Fabric port: Cozy Campfire
 * Provides regeneration to players and bees near lit campfires.
 */
public final class FabricCozyCampfire {
    private FabricCozyCampfire() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricMechanicsConfig.enableCozyCampfire) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % FabricMechanicsConfig.cozyCampfireIntervalTicks != 0) continue;

                if (isNearLitCampfire(player.blockPosition(), player.level()::getBlockState, player::distanceToSqr)) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION,
                        FabricMechanicsConfig.cozyCampfireDurationTicks,
                        FabricMechanicsConfig.cozyCampfireAmplifier
                    ));
                }
            }
        });

        if (FabricMechanicsConfig.cozyCampfireAffectBees) {
            ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
                // Bees tick independently; we'll handle them via a tick event
            });

            ServerTickEvents.END_SERVER_TICK.register(server -> {
                if (!FabricMechanicsConfig.enableCozyCampfire || !FabricMechanicsConfig.cozyCampfireAffectBees) return;

                for (var level : server.getAllLevels()) {
                    for (var entity : level.getAllEntities()) {
                        if (entity instanceof Bee bee) {
                            if (bee.tickCount % FabricMechanicsConfig.cozyCampfireIntervalTicks != 0) continue;

                            if (isNearLitCampfire(bee.blockPosition(), bee.level()::getBlockState, bee::distanceToSqr)) {
                                bee.addEffect(new MobEffectInstance(
                                    MobEffects.REGENERATION,
                                    FabricMechanicsConfig.cozyCampfireDurationTicks,
                                    FabricMechanicsConfig.cozyCampfireAmplifier
                                ));
                            }
                        }
                    }
                }
            });
        }
    }

    private static boolean isNearLitCampfire(BlockPos center, BlockStateProvider blockStateProvider, DistanceCalculator distanceCalculator) {
        try {
            int range = (int) Math.ceil(FabricMechanicsConfig.cozyCampfireRange);
            double rangeSq = FabricMechanicsConfig.cozyCampfireRange * FabricMechanicsConfig.cozyCampfireRange;

            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        BlockPos pos = center.offset(x, y, z);

                        if (distanceCalculator.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSq) {
                            continue;
                        }

                        try {
                            BlockState state = blockStateProvider.getBlockState(pos);
                            if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                                return true;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    @FunctionalInterface
    private interface BlockStateProvider {
        BlockState getBlockState(BlockPos pos);
    }

    @FunctionalInterface
    private interface DistanceCalculator {
        double distanceToSqr(double x, double y, double z);
    }
}
