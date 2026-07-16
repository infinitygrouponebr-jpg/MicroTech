package Infinitygroup.microtech.network;

import Infinitygroup.microtech.Microtech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TechSwordAbilitySelectionPayload(String selectedAbility) implements CustomPacketPayload {
    public static final Type<TechSwordAbilitySelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Microtech.MODID, "tech_sword_ability_selection")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TechSwordAbilitySelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            TechSwordAbilitySelectionPayload::selectedAbility,
            TechSwordAbilitySelectionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
