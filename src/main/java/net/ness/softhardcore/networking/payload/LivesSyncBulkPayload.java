package net.ness.softhardcore.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.ness.softhardcore.SoftHardcore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record LivesSyncBulkPayload(Map<UUID, Integer> livesMap) implements CustomPayload {
    public static final CustomPayload.Id<LivesSyncBulkPayload> ID =
            new CustomPayload.Id<>(Identifier.of(SoftHardcore.MOD_ID, "lives_sync_bulk"));

    public static final PacketCodec<RegistryByteBuf, LivesSyncBulkPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.map(HashMap::new, Uuids.PACKET_CODEC, PacketCodecs.INTEGER),
            LivesSyncBulkPayload::livesMap,
            LivesSyncBulkPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
