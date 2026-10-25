package org.onenonly.bitsandbalance.fabric.potions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;

/**
 * Keeps 26.3's native brewing recipes aligned with the existing potion-family
 * configuration. Conditions are evaluated during datapack loading, before the
 * recipe codec resolves potion registry entries that may be disabled.
 */
public record FabricCustomBrewingResourceCondition(String feature) implements ResourceCondition {
    private static final MapCodec<FabricCustomBrewingResourceCondition> CODEC =
            Codec.STRING.fieldOf("feature").xmap(FabricCustomBrewingResourceCondition::new, FabricCustomBrewingResourceCondition::feature);

    private static final ResourceConditionType<FabricCustomBrewingResourceCondition> TYPE =
            ResourceConditionType.create(
                    Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "custom_brewing_enabled"),
                    CODEC
            );

    public static void register() {
        ResourceConditions.register(TYPE);
    }

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.RegistryInfoLookup registryInfoLookup) {
        if (!FabricPotionConfig.enableCustomBrewing) {
            return false;
        }

        return switch (feature) {
            case "withering" -> FabricPotionConfig.enableWitheringPotion;
            case "levitation" -> FabricPotionConfig.enableLevitationPotion;
            case "haste" -> FabricPotionConfig.enableHastePotion;
            case "blindness" -> FabricPotionConfig.enableBlindnessPotion;
            case "glowing" -> FabricPotionConfig.enableGlowingPotion;
            case "bioluminescence" -> FabricPotionConfig.enableGlowingPotion
                    && FabricPotionConfig.enableBioluminescencePotion;
            case "displacement" -> FabricPotionConfig.enableDisplacementPotion;
            case "returning" -> FabricPotionConfig.enableDisplacementPotion
                    && FabricPotionConfig.enableReturningPotion;
            default -> false;
        };
    }
}
