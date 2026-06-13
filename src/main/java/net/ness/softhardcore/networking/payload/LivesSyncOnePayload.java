package net.ness.softhardcore.networking.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ness.softhardcore.SoftHardcore;

import java.util.UUID;

public record LivesSyncOnePayload(UUID uuid, int lives) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LivesSyncOnePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SoftHardcore.MOD_ID, "lives_sync_one"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LivesSyncOnePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LivesSyncOnePayload::uuid,
            ByteBufCodecs.VAR_INT, LivesSyncOnePayload::lives,
            LivesSyncOnePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
