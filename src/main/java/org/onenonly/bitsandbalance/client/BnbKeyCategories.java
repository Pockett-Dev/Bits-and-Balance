package org.onenonly.bitsandbalance.client;

import net.minecraft.client.KeyMapping;
//? if mc != 1.21.1
import net.minecraft.resources.Identifier;
//? endif
import org.onenonly.bitsandbalance.BitsAndBalance;

public final class BnbKeyCategories {
    //? if mc == 1.21.1
    public static final String MAIN = "key.category." + BitsAndBalance.MODID + ".main";
    //? else
    public static final KeyMapping.Category MAIN = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "main")
    );
    //? endif

    private BnbKeyCategories() {
    }
}
