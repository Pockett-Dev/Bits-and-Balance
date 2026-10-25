package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.onenonly.bitsandbalance.common.entity.SeatEntity;

public final class SittingSeatHelper {
    private SittingSeatHelper() {
    }

    public static boolean isMountedOnSeat(Entity entity) {
        try {
            return entity != null && entity.isPassenger() && isSeat(entity.getVehicle());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isSeat(Entity entity) {
        return entity instanceof SeatEntity;
    }

    public static boolean mount(ServerPlayer player, EntityType<? extends SeatEntity> seatType) {
        if (player == null || seatType == null) {
            return false;
        }

        if (isMountedOnSeat(player)) {
            return true;
        }

        try {
            if (player.isPassenger()) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        SeatEntity seat = new SeatEntity(seatType, level);
        seat.setPos(player.getX(), player.getY(), player.getZ());
        seat.setYRot(player.getYRot());
        seat.setXRot(0.0F);
        seat.setNoGravity(true);
        level.addFreshEntity(seat);

        boolean mounted;
        //? if mc == 1.21.1
        mounted = player.startRiding(seat, true);
        //? else
        mounted = player.startRiding(seat, true, true);
        //? endif

        if (!mounted) {
            seat.discard();
        }
        return mounted;
    }

    public static void dismount(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Entity vehicle = null;
        try {
            vehicle = player.getVehicle();
        } catch (Throwable ignored) {
        }

        if (isSeat(vehicle)) {
            try {
                player.stopRiding();
            } catch (Throwable ignored) {
            }
            try {
                vehicle.discard();
            } catch (Throwable ignored) {
            }
        }
    }
}
