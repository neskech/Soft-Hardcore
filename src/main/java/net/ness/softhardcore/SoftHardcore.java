package net.ness.softhardcore;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.ness.softhardcore.command.Commands;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.item.ModItems;
import net.ness.softhardcore.networking.NetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SoftHardcore.MOD_ID)
public class SoftHardcore {
    public static final String MOD_ID = "softhardcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SoftHardcore(IEventBus modEventBus) {
        LOGGER.info("Initializing SoftHardcore");

        MyConfig.registerConfig();
        ModItems.register(modEventBus);
        modEventBus.addListener(NetworkManager::registerPayloadHandlers);
        modEventBus.addListener(ModItems::addCreative);

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                Commands.register(event.getDispatcher(), event.getBuildContext()));

        NeoForge.EVENT_BUS.register(EventLogic.class);
        NeoForge.EVENT_BUS.register(LifeRegenerationTask.class);
    }
}
