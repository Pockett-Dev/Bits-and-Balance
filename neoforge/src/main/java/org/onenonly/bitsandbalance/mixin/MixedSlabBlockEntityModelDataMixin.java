package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import net.neoforged.neoforge.model.data.ModelData;
import org.onenonly.bitsandbalance.client.model.MixedSlabModelData;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MixedSlabBlockEntity.class)
public abstract class MixedSlabBlockEntityModelDataMixin implements IBlockEntityExtension {
    @Shadow
    public abstract BlockState getBottomSlab();

    @Shadow
    public abstract BlockState getTopSlab();

    @Override
    public ModelData getModelData() {
        return MixedSlabModelData.of(getBottomSlab(), getTopSlab());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void bitsandbalance$refreshModelDataAfterLoad(ValueInput input, CallbackInfo ci) {
        requestModelDataUpdate();
    }

    @Inject(method = "setSlabs", at = @At("TAIL"))
    private void bitsandbalance$refreshModelDataAfterSet(BlockState bottom, BlockState top, CallbackInfo ci) {
        requestModelDataUpdate();
    }
}