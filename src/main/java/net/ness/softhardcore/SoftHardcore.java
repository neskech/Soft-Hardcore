package net.ness.softhardcore;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.ness.softhardcore.component.MyComponents;
import net.ness.softhardcore.config.MyConfig;
import net.ness.softhardcore.item.ModItems;
import net.ness.softhardcore.event.PlayerDeathCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoftHardcore implements ModInitializer {
	public static final String MOD_ID = "softhardcore";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
		MyConfig.registerConfig();
		EventLogic.registerEventLogic();
		ModItems.ReigsterModItems();
		LifeRegenerationTask.register();



	}
}