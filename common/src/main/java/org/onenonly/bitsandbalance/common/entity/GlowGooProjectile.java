package org.onenonly.bitsandbalance.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blocks.GooSplatterBlock;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceSyncBridge;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;

public final class GlowGooProjectile extends ThrowableItemProjectile {
    public GlowGooProjectile(EntityType<? extends GlowGooProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GlowGooProjectile(EntityType<? extends GlowGooProjectile> entityType, double x, double y, double z, Level level, ItemStack stack) {
        super(entityType, x, y, z, level, stack);
    }

    public GlowGooProjectile(EntityType<? extends GlowGooProjectile> entityType, LivingEntity owner, Level level, ItemStack stack) {
        super(entityType, owner, level, stack);
    }

    public GlowGooProjectile(Level level, double x, double y, double z, ItemStack stack) {
        this(ModGlowGoo.glowGooProjectileType(), x, y, z, level, stack);
    }

    public GlowGooProjectile(Level level, LivingEntity owner, ItemStack stack) {
        this(ModGlowGoo.glowGooProjectileType(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModGlowGoo.glowGoo();
    }

    @Override
    public void tick() {
        if (bitsandbalance$discardIfTouchingLava()) {
            return;
        }

        super.tick();

        bitsandbalance$discardIfTouchingLava();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (!this.level().isClientSide()) {
            bitsandbalance$spawnImpactParticles(hitResult.getLocation());
            boolean placed = GlowGooRuntime.enabled && bitsandbalance$tryPlaceFromBlockHit(hitResult);
            if (!placed) {
                bitsandbalance$playImpactSound(hitResult.getBlockPos());
            }
        }
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (!this.level().isClientSide()) {
            bitsandbalance$spawnImpactParticles(hitResult.getLocation());
            if (hitResult.getEntity() instanceof LivingEntity livingEntity) {
                bitsandbalance$applyBioluminescence(livingEntity);
            }
            bitsandbalance$playImpactSound(hitResult.getEntity().blockPosition());
        }
        this.discard();
    }

    private boolean bitsandbalance$tryPlaceFromBlockHit(BlockHitResult hitResult) {
        BlockPos blockPos = hitResult.getBlockPos();
        Direction direction = hitResult.getDirection();
        return bitsandbalance$tryPlaceAt(blockPos.relative(direction), direction);
    }

    private boolean bitsandbalance$tryPlaceAt(BlockPos pos, Direction facing) {
        Level level = this.level();
        if (level.getFluidState(pos).is(FluidTags.LAVA)) {
            return false;
        }

        BlockState existingState = level.getBlockState(pos);
        if (!existingState.canBeReplaced()) {
            return false;
        }

        Vec3 incomingDirection = this.getDeltaMovement().reverse();
        int rotation = GooSplatterBlock.rotationFromIncomingDirection(facing, incomingDirection);

        BlockState splatterState = ModGlowGoo.gooSplatter().defaultBlockState()
                .setValue(GooSplatterBlock.FACING, facing)
                .setValue(GooSplatterBlock.ROTATION, rotation)
                .setValue(GooSplatterBlock.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        if (!splatterState.canSurvive(level, pos)) {
            return false;
        }
        if (!level.setBlock(pos, splatterState, 3)) {
            return false;
        }

        SoundType soundType = splatterState.getSoundType();
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        return true;
    }

    private boolean bitsandbalance$discardIfTouchingLava() {
        if (!this.isAlive() || !this.isInLava()) {
            return false;
        }

        if (!this.level().isClientSide()) {
            Vec3 pos = this.position();
            ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.SMOKE,
                    pos.x,
                    pos.y + 0.05D,
                    pos.z,
                    8,
                    0.08D,
                    0.04D,
                    0.08D,
                    0.01D
            );
            this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    0.5F, 1.6F + this.random.nextFloat() * 0.2F);
        }

        this.discard();
        return true;
    }

    private void bitsandbalance$playImpactSound(BlockPos pos) {
        this.level().playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS,
                0.6F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    private void bitsandbalance$spawnImpactParticles(Vec3 hitLocation) {
        if (!GlowGooRuntime.impactParticlesEnabled || GlowGooRuntime.impactParticleCount <= 0) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
            ParticleTypes.GLOW,
                hitLocation.x,
                hitLocation.y,
                hitLocation.z,
                GlowGooRuntime.impactParticleCount,
                0.18D,
                0.12D,
                0.18D,
                0.02D
        );
    }

    private void bitsandbalance$applyBioluminescence(LivingEntity livingEntity) {
        if (!GlowGooRuntime.bioluminescenceEnabled || GlowGooRuntime.bioluminescenceDurationTicks <= 0) {
            return;
        }

        livingEntity.addEffect(new MobEffectInstance(ModGlowGoo.bioluminescence(), GlowGooRuntime.bioluminescenceDurationTicks, 0, false, false, true));
        BioluminescenceSyncBridge.syncIfServer(livingEntity, GlowGooRuntime.bioluminescenceDurationTicks);
    }
}