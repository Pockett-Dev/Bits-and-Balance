package org.onenonly.bitsandbalance.common.items;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
//? if mc == 1.21.1
import net.minecraft.world.InteractionResultHolder;
//? else
import net.minecraft.world.InteractionResult;
//? endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.entity.GlowGooProjectile;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;

public final class GlowGooItem extends Item implements ProjectileItem {
    public static final float PROJECTILE_SHOOT_POWER = 1.5F;

    public GlowGooItem(Properties properties) {
        super(properties);
    }

    @Override
    public ProjectileItem.DispenseConfig createDispenseConfig() {
        return ProjectileItem.DispenseConfig.builder()
                .power(PROJECTILE_SHOOT_POWER)
                .uncertainty(1.0F)
                .build();
    }

//? if mc == 1.21.1
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!GlowGooRuntime.enabled) {
            return InteractionResultHolder.pass(stack);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            ItemStack projectileStack = stack.copy();
            projectileStack.setCount(1);
            GlowGooProjectile projectile = new GlowGooProjectile(serverLevel, player, projectileStack);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, PROJECTILE_SHOOT_POWER, 1.0F);
            serverLevel.addFreshEntity(projectile);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
//? else
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!GlowGooRuntime.enabled) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation(
                    (server, livingEntity, projectileStack) -> new GlowGooProjectile(server, livingEntity, projectileStack),
                    serverLevel,
                    stack,
                    player,
                    0.0F,
                    PROJECTILE_SHOOT_POWER,
                    1.0F
            );
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
//? endif

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        ItemStack projectileStack = stack.copy();
        projectileStack.setCount(1);
        return new GlowGooProjectile(level, position.x(), position.y(), position.z(), projectileStack);
    }
}
