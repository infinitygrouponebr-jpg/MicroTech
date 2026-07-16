package Infinitygroup.microtech.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TechChipItem extends Item {
    private final TechChipType chipType;

    public TechChipItem(TechChipType chipType, Properties properties) {
        super(properties);
        this.chipType = chipType;
    }

    public TechChipType getChipType() {
        return this.chipType;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String baseKey = "tooltip.microtech." + this.chipType.getId() + "_chip.";
        tooltip.add(Component.translatable(baseKey + "class").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(baseKey + "desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(baseKey + "desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(baseKey + "desc3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
