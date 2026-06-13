package net.ness.softhardcore.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ness.softhardcore.SoftHardcore;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SoftHardcore.MOD_ID);

    public static final DeferredHolder<Item, LifeHeartItem> LIFE_HEART =
            ITEMS.register("life_heart", () -> new LifeHeartItem(new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        SoftHardcore.LOGGER.info("Registering mod items for {}", SoftHardcore.MOD_ID);
        ITEMS.register(modEventBus);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(LIFE_HEART.get());
        }
    }
}
