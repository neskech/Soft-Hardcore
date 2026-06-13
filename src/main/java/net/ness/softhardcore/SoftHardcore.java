package net.ness.softhardcore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.ness.softhardcore.command.Commands;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.item.ModItems;
import net.ness.softhardcore.networking.NetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoftHardcore implements ModInitializer {
    public static final String MOD_ID = "softhardcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing SoftHardcore");

        MyConfig.registerConfig();
        NetworkManager.registerPayloadTypes();
        ModItems.register();
        EventLogic.registerEventLogic();
        LifeRegenerationTask.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                Commands.register(dispatcher, registryAccess));
    }
}
