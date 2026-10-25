package org.onenonly.bitsandbalance.common.entity;

//? if mc == 1.21.1
import net.minecraft.nbt.CompoundTag;
//? endif
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//? if mc != 1.21.1
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//? endif
import net.minecraft.world.phys.Vec3;

public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced data.
    }

    //? if mc == 1.21.1
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Seats are transient.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Seats are transient.
    }
    //? else
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // Seats are transient.
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // Seats are transient.
    }
    //? endif

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);

        if (!this.level().isClientSide() && this.getPassengers().isEmpty()) {
            this.discard();
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && this.getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    //? if mc == 1.21.1
    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        return false;
    }
    //? else
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }
    //? endif
}
