package net.ness.softhardcore.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.ness.softhardcore.SoftHardcore;
import net.ness.softhardcore.util.LivesCacheManager;

import java.util.Map;
import java.util.UUID;

public class NetworkManager {
    public static final Identifier LIVES_SYNC_ONE = new Identifier(SoftHardcore.MOD_ID, "lives_sync_one");
    public static final Identifier LIVES_SYNC_BULK = new Identifier(SoftHardcore.MOD_ID, "lives_sync_bulk");

    public static void registerClientToServerPackets() {
        // No C2S packets required for lives at this time
    }

    public static void registerServerToClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(LIVES_SYNC_ONE, (client, handler, buf, responseSender) -> {
            UUID uuid = buf.readUuid();
            int lives = buf.readInt();
            client.execute(() -> LivesCacheManager.updateLivesCache(uuid, lives));
        });

        ClientPlayNetworking.registerGlobalReceiver(LIVES_SYNC_BULK, (client, handler, buf, responseSender) -> {
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                UUID uuid = buf.readUuid();
                int lives = buf.readInt();
                client.execute(() -> LivesCacheManager.updateLivesCache(uuid, lives));
            }
        });
    }

    public static void sendLivesOneTo(ServerPlayerEntity player, UUID uuid, int lives) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeUuid(uuid);
        out.writeInt(lives);
        ServerPlayNetworking.send(player, LIVES_SYNC_ONE, out);
    }

    public static void broadcastLivesOne(MinecraftServer server, UUID uuid, int lives) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            sendLivesOneTo(p, uuid, lives);
        }
    }

    public static void sendLivesBulkTo(ServerPlayerEntity target, Map<UUID, Integer> livesMap) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeVarInt(livesMap.size());
        for (Map.Entry<UUID, Integer> e : livesMap.entrySet()) {
            out.writeUuid(e.getKey());
            out.writeInt(e.getValue());
        }
        ServerPlayNetworking.send(target, LIVES_SYNC_BULK, out);
    }
}
