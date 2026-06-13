package net.ness.softhardcore.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.SoftHardcore;

public class ModItems {
    public static final Item LIFE_HEART = register("life_heart", new LifeHeartItem(new Item.Settings()));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(SoftHardcore.MOD_ID, name), item);
    }

    public static void register() {
        SoftHardcore.LOGGER.info("Registering mod items for {}", SoftHardcore.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> entries.add(LIFE_HEART));
    }
}
