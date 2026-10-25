package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.onenonly.bitsandbalance.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(Options.class)
public class OptionsMultiModifierMixin {
    @Shadow
    public KeyMapping[] keyMappings;

    @Shadow
    @Final
    private java.io.File optionsFile;

    @Inject(method = "load", at = @At("RETURN"))
    private void bitsandbalance$loadStoredModifierMasks(CallbackInfo ci) {
        Path path = this.optionsFile.toPath();
        if (!Files.isRegularFile(path)) {
            return;
        }

        Map<String, KeyMapping> mappingsByOptionName = new HashMap<>();
        for (KeyMapping mapping : this.keyMappings) {
            mappingsByOptionName.put("key_" + mapping.getName(), mapping);
        }

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                int separator = line.indexOf(':');
                if (separator < 0) {
                    continue;
                }

                KeyMapping mapping = mappingsByOptionName.get(line.substring(0, separator));
                if (mapping == null) {
                    continue;
                }

                int mask = KeybindModifierStore.parseSavedModifierMask(line.substring(separator + 1));
                KeybindModifierStore.loadStoredMask(mapping, mask);
            }
        } catch (IOException ignored) {
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void bitsandbalance$saveStoredModifierMasks(CallbackInfo ci) {
        Path path = this.optionsFile.toPath();
        if (!Files.isRegularFile(path)) {
            return;
        }

        Map<String, String> keyLines = new HashMap<>();
        for (KeyMapping mapping : this.keyMappings) {
            keyLines.put("key_" + mapping.getName(), KeybindModifierStore.appendSavedModifiers(mapping));
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                int separator = line.indexOf(':');
                if (separator < 0) {
                    continue;
                }

                String optionName = line.substring(0, separator);
                String keyValue = keyLines.get(optionName);
                if (keyValue != null) {
                    lines.set(index, optionName + ":" + keyValue);
                }
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}