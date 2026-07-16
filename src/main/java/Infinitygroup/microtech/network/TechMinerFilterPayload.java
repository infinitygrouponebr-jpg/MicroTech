package Infinitygroup.microtech.network;

import Infinitygroup.microtech.Microtech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TechMinerFilterPayload(int x, int y, int z, int action, int index, String blockId) implements CustomPacketPayload {
    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;
    public static final int ACTION_CLEAR = 2;

    public static final Type<TechMinerFilterPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "tech_miner_filter")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TechMinerFilterPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TechMinerFilterPayload decode(RegistryFriendlyByteBuf buffer) {
            return new TechMinerFilterPayload(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, TechMinerFilterPayload payload) {
            buffer.writeVarInt(payload.x());
            buffer.writeVarInt(payload.y());
            buffer.writeVarInt(payload.z());
            buffer.writeVarInt(payload.action());
            buffer.writeVarInt(payload.index());
            buffer.writeUtf(payload.blockId());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
