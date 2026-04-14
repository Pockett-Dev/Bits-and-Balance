package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Custom block that properly handles loot table drops
 */
public class OreVariantBlock extends Block {
    public OreVariantBlock(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Use loot table for drops instead of default behavior
        var lootTableKey = this.getLootTable().orElse(null);
        if (lootTableKey == null) {
            return super.getDrops(state, builder);
        }
        return builder.getLevel().getServer().reloadableRegistries()
            .getLootTable(lootTableKey)
            .getRandomItems(builder.create(LootContextParamSets.BLOCK));
    }
}
