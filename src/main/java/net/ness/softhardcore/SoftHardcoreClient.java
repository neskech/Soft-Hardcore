package net.ness.softhardcore;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.ness.softhardcore.networking.NetworkManager;
import net.ness.softhardcore.util.LivesCacheManager;

public class SoftHardcoreClient implements ClientModInitializer {
    private static MinecraftClient CLIENT;

    public static MinecraftClient getClient() {
        return CLIENT != null ? CLIENT : MinecraftClient.getInstance();
    }

    @Override
    public void onInitializeClient() {
        NetworkManager.registerClientReceivers();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CLIENT = null;
            LivesCacheManager.clearLivesCache();
        });
    }
}
