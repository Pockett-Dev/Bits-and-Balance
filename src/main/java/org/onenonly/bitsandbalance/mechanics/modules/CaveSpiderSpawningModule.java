package org.onenonly.bitsandbalance.mechanics.modules;

import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;

/**
 * Cave Spider Spawning Module
 *
 * Handles cave spider spawning configuration.
 * Currently disabled - feature removed.
 */
public class CaveSpiderSpawningModule implements FeatureModule {

    @Override
    public String getFeatureName() {
        return "Cave Spider Spawning";
    }

    @Override
    public boolean isEnabled() {
        return false; // Always disabled
    }

    @Override
    public int getInitializationPriority() {
        return 100;
    }
}
