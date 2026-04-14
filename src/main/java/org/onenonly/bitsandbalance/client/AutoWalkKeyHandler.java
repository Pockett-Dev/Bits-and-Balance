package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.onenonly.bitsandbalance.BitsAndBalance;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Registers and stores the Auto Walk key mapping and state.
 */
public class AutoWalkKeyHandler {
    public static KeyMapping AUTO_WALK_KEY;
    // Indicates whether Auto Walk is currently active
    public static boolean active = false;

    private static final KeyMapping.Category CATEGORY = BnbKeyCategories.MAIN;

    public static void registerKeybinding(RegisterKeyMappingsEvent event) {
        // Only register if not already registered to prevent duplicates
        if (AUTO_WALK_KEY == null) {
            // Unbound by default. Put under Bits and Balance category.
            AUTO_WALK_KEY = new KeyMapping(
                "key." + BitsAndBalance.MODID + ".auto_walk",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                -1,
                CATEGORY
            );
            event.register(AUTO_WALK_KEY);
        }
    }
}
