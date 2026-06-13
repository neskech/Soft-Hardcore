package net.ness.softhardcore.networking.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.ness.softhardcore.SoftHardcore;

import java.util.UUID;

public record LivesSyncOnePayload(UUID uuid, int lives) implements CustomPayload {
    public static final CustomPayload.Id<LivesSyncOnePayload> ID =
            new CustomPayload.Id<>(Identifier.of(SoftHardcore.MOD_ID, "lives_sync_one"));

    public static final PacketCodec<RegistryByteBuf, LivesSyncOnePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, LivesSyncOnePayload::uuid,
            PacketCodecs.INTEGER, LivesSyncOnePayload::lives,
            LivesSyncOnePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
