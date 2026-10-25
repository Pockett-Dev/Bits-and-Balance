package org.onenonly.bitsandbalance.common.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.registry.ContentRegistrar;

import java.util.List;

public final class ModDogMusicDisc {
    public static final Identifier MUSIC_DISC_DOG_ID = id("music_disc_dog");
    public static final Identifier DOG_JUKEBOX_SONG_ID = id("dog");
    public static final TagKey<Item> CREEPER_DROP_MUSIC_DISCS = TagKey.create(Registries.ITEM, Identifier.parse("minecraft:creeper_drop_music_discs"));

    public static final float SIMPLE_DUNGEON_LOOT_CHANCE = 0.215F;
    public static final float ANCIENT_CITY_LOOT_CHANCE = 0.161F;
    public static final float WOODLAND_MANSION_LOOT_CHANCE = 0.218F;

    private ModDogMusicDisc() {
    }

    public static void register(ContentRegistrar registrar) {
        registrar.registerItem(MUSIC_DISC_DOG_ID, () -> new Item(itemProps(MUSIC_DISC_DOG_ID)
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, DOG_JUKEBOX_SONG_ID))));
    }

    public static Item musicDiscDog() {
        return BuiltInRegistries.ITEM.get(MUSIC_DISC_DOG_ID)
                .map(holder -> holder.value())
                .orElse(Items.AIR);
    }

    public static Item randomReplacementCreeperDropDisc(RandomSource random) {
        Item dogDisc = musicDiscDog();
        List<Item> candidates = BuiltInRegistries.ITEM.stream()
            .filter(item -> item != Items.AIR)
            .filter(item -> item != dogDisc)
            .filter(item -> item.builtInRegistryHolder().is(CREEPER_DROP_MUSIC_DISCS))
                .toList();
        if (candidates.isEmpty()) {
            return Items.AIR;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }

    private static Item.Properties itemProps(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
}