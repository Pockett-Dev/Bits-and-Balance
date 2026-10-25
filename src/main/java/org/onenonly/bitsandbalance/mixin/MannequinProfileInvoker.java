package org.onenonly.bitsandbalance.mixin;

//? if mc != 1.21.1
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinProfileInvoker {
    @Invoker("setProfile")
    void bitsandbalance$setProfile(ResolvableProfile profile);
}
//? endif
