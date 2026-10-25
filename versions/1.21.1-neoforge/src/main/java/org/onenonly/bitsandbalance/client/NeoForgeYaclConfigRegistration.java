package org.onenonly.bitsandbalance.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.onenonly.bitsandbalance.BitsAndBalance;

public final class NeoForgeYaclConfigRegistration {
    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    private NeoForgeYaclConfigRegistration() {
    }

    public static void register(ModContainer modContainer) {
        if (!ModList.get().isLoaded(YACL_MOD_ID)) {
            BitsAndBalance.LOGGER.debug("[{}] YACL is not installed; skipping NeoForge config screen registration.", BitsAndBalance.MODID);
            return;
        }

        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> NeoForgeYaclConfigScreen.create(parent)
        );
    }
}
