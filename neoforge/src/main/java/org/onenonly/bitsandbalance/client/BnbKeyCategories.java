package org.onenonly.bitsandbalance.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.BitsAndBalance;

public final class BnbKeyCategories {
    public static final KeyMapping.Category MAIN = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "main")
    );

    private BnbKeyCategories() {
    }
}
