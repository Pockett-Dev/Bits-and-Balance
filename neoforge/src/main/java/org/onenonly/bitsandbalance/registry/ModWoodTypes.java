package org.onenonly.bitsandbalance.registry;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Registers custom wood types for the mod.
 * Must be called early in mod initialization before blocks are registered.
 */
public class ModWoodTypes {
    
    // Create a custom BlockSetType for azalea wood
    // Uses WOOD sound type and similar properties to oak
    public static final BlockSetType AZALEA_BLOCK_SET = BlockSetType.register(
        new BlockSetType(BitsAndBalance.MODID + ":azalea")
    );
    
    // Create a custom WoodType for azalea wood
    // This is used by signs and hanging signs for rendering and GUI textures
    // Use "bitsandbalance:azalea" - Sheets will parse this and look for textures at:
    // - Regular signs: assets/bitsandbalance/textures/entity/signs/azalea.png
    // - Hanging signs: assets/bitsandbalance/textures/entity/signs/hanging/azalea.png
    // The namespace (bitsandbalance) tells it which mod, the path (azalea) is the filename
    public static final WoodType AZALEA_WOOD_TYPE = WoodType.register(
        new WoodType(BitsAndBalance.MODID + ":azalea", AZALEA_BLOCK_SET)
    );
    
    /**
     * Call this method early in mod initialization to register wood types.
     * This must happen before blocks are registered.
     */
    public static void register() {
        BitsAndBalance.LOGGER.info("Registered Azalea BlockSetType and WoodType");
    }
}
