package net.ness.softhardcore.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ness.softhardcore.networking.payload.LivesSyncBulkPayload;
import net.ness.softhardcore.networking.payload.LivesSyncOnePayload;
import net.ness.softhardcore.util.LivesCacheManager;
import net.ness.softhardcore.SoftHardcore;

import java.util.Map;
import java.util.UUID;

public class NetworkManager {

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(SoftHardcore.MOD_ID);
        registrar.playToClient(LivesSyncOnePayload.TYPE, LivesSyncOnePayload.STREAM_CODEC, NetworkManager::handleLivesSyncOne);
        registrar.playToClient(LivesSyncBulkPayload.TYPE, LivesSyncBulkPayload.STREAM_CODEC, NetworkManager::handleLivesSyncBulk);
    }

    private static void handleLivesSyncOne(LivesSyncOnePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> LivesCacheManager.updateLivesCache(payload.uuid(), payload.lives()));
    }

    private static void handleLivesSyncBulk(LivesSyncBulkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            for (Map.Entry<UUID, Integer> entry : payload.livesMap().entrySet()) {
                LivesCacheManager.updateLivesCache(entry.getKey(), entry.getValue());
            }
        });
    }

    public static void sendLivesOneTo(ServerPlayer player, UUID uuid, int lives) {
        PacketDistributor.sendToPlayer(player, new LivesSyncOnePayload(uuid, lives));
    }

    public static void broadcastLivesOne(net.minecraft.server.MinecraftServer server, UUID uuid, int lives) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendLivesOneTo(player, uuid, lives);
        }
    }

    public static void sendLivesBulkTo(ServerPlayer target, Map<UUID, Integer> livesMap) {
        PacketDistributor.sendToPlayer(target, new LivesSyncBulkPayload(livesMap));
    }
}
