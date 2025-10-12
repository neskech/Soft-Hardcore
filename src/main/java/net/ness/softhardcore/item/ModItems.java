package net.ness.softhardcore.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.SoftHardcore;

public class ModItems {
    public static final Item LIFE_HEART = RegisterItem("life_heart", new Item(new FabricItemSettings()));

    private static Item RegisterItem(String name, Item item) {
        Identifier id = new Identifier(SoftHardcore.MOD_ID, name);
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void ReigsterModItems() {
        SoftHardcore.LOGGER.info("Registering mod items for " + SoftHardcore.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register((entries) -> entries.add(LIFE_HEART));
    }
}
