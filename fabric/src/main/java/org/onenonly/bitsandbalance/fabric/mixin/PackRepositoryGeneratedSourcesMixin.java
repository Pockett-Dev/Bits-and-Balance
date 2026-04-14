package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.packs.repository.PackRepository;
import org.onenonly.bitsandbalance.fabric.recipe.FabricGeneratedWorldDataPacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackRepository.class)
public class PackRepositoryGeneratedSourcesMixin {
    @Inject(method = "reload", at = @At("HEAD"), require = 0)
    private void bitsandbalance$ensureGeneratedSourcesBeforeReload(CallbackInfo ci) {
        FabricGeneratedWorldDataPacks.ensureRepositorySources((PackRepository) (Object) this);
    }
}