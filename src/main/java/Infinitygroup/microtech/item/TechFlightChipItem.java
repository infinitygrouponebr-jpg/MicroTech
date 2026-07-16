package Infinitygroup.microtech.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class TechFlightChipItem extends Item {
    public TechFlightChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.microtech.tech_flight_chip.class").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.microtech.tech_flight_chip.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.tech_flight_chip.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.tech_flight_chip.desc3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.microtech.tech_flight_chip.install_time").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.microtech.tech_flight_chip.desc4").withStyle(ChatFormatting.DARK_GRAY));
    }
}
