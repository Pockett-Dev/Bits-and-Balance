package org.onenonly.bitsandbalance.fabric.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared path helpers for Fabric config files.
 *
 * Goal:
 * - Store all Fabric config files under "config/Bits and Balance/" (matching NeoForge layout).
 * - Migrate legacy flat config files ("config/bitsandbalance-*.json") into that folder.
 */
public final class FabricConfigPaths {
    private static final String MOD_CONFIG_DIR_NAME = "Bits and Balance";

    private FabricConfigPaths() {
    }

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_CONFIG_DIR_NAME);
    }

    public static Path resolve(String fileName) {
        return configDir().resolve(fileName);
    }

    /**
     * If a legacy config exists at "config/<fileName>" and no new config exists at
     * "config/Bits and Balance/<fileName>", move the legacy file into the new directory.
     */
    public static void migrateLegacyIfPresent(String fileName) {
        Path legacy = FabricLoader.getInstance().getConfigDir().resolve(fileName);
        Path target = resolve(fileName);

        if (!Files.exists(legacy) || Files.exists(target)) {
            return;
        }

        try {
            Files.createDirectories(target.getParent());
            Files.move(legacy, target);
        } catch (IOException ignored) {
            // If migration fails, continue using defaults; the caller will still read from target.
        }
    }
}
