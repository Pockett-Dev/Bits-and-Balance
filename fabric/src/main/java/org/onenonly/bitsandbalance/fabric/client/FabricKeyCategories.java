package org.onenonly.bitsandbalance.fabric.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

public final class FabricKeyCategories {
    public static final KeyMapping.Category MAIN = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "main")
    );

    private FabricKeyCategories() {
    }
}
