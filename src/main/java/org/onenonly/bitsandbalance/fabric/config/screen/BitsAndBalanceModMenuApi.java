package org.onenonly.bitsandbalance.fabric.config.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public final class BitsAndBalanceModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")
                ? FabricYaclConfigScreen.create(parent)
                : new FabricModMenuConfigScreen(parent);
    }
}
