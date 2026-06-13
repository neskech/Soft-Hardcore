package net.ness.softhardcore.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ness.softhardcore.networking.payload.LivesSyncBulkPayload;
import net.ness.softhardcore.networking.payload.LivesSyncOnePayload;
import net.ness.softhardcore.util.LivesCacheManager;

import java.util.Map;
import java.util.UUID;

public class NetworkManager {

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(LivesSyncOnePayload.ID, LivesSyncOnePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LivesSyncBulkPayload.ID, LivesSyncBulkPayload.CODEC);
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(LivesSyncOnePayload.ID, (payload, context) ->
                context.client().execute(() ->
                        LivesCacheManager.updateLivesCache(payload.uuid(), payload.lives())));

        ClientPlayNetworking.registerGlobalReceiver(LivesSyncBulkPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    for (Map.Entry<UUID, Integer> entry : payload.livesMap().entrySet()) {
                        LivesCacheManager.updateLivesCache(entry.getKey(), entry.getValue());
                    }
                }));
    }

    public static void sendLivesOneTo(ServerPlayerEntity player, UUID uuid, int lives) {
        ServerPlayNetworking.send(player, new LivesSyncOnePayload(uuid, lives));
    }

    public static void broadcastLivesOne(MinecraftServer server, UUID uuid, int lives) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendLivesOneTo(player, uuid, lives);
        }
    }

    public static void sendLivesBulkTo(ServerPlayerEntity target, Map<UUID, Integer> livesMap) {
        ServerPlayNetworking.send(target, new LivesSyncBulkPayload(livesMap));
    }
}
