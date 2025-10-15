package net.ness.softhardcore;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.ness.softhardcore.util.LivesCacheManager;

public class SoftHardcoreClient implements ClientModInitializer {
    private static MinecraftClient CLIENT;

    public static MinecraftClient getClient() {
        return CLIENT;
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            CLIENT = client;
            // LivesHudOverlayCallback callback = new LivesHudOverlayCallback(client);
            // HudRenderCallback.EVENT.register(callback); // Removed ugly HUD
        });
        net.ness.softhardcore.networking.NetworkManager.registerServerToClientPackets();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LivesCacheManager.clearLivesCache();
        });
    }
}
