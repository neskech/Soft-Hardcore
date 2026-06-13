package net.ness.softhardcore.networking.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ness.softhardcore.SoftHardcore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record LivesSyncBulkPayload(Map<UUID, Integer> livesMap) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LivesSyncBulkPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SoftHardcore.MOD_ID, "lives_sync_bulk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LivesSyncBulkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            LivesSyncBulkPayload::livesMap,
            LivesSyncBulkPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
