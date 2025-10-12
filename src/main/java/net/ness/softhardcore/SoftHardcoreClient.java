package net.ness.softhardcore;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

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
    }
}
