package Infinitygroup.microtech.machine;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.gui.screens.Screen;

public class MachineUpgradeItem extends Item {
    private final MachineUpgradeType upgradeType;

    public MachineUpgradeItem(MachineUpgradeType upgradeType, Properties properties) {
        super(properties);
        this.upgradeType = upgradeType;
    }

    public MachineUpgradeType getUpgradeType() {
        return this.upgradeType;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MachineUpgradeType displayType = this.upgradeType.getDisplayType();
        tooltip.add(Component.translatable("tooltip.microtech.machine_upgrade.category", displayType.getCategory().getComponent()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Stacks in the same slot combine by count.").withStyle(ChatFormatting.DARK_GRAY));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable(displayType.getDescriptionKey()).withStyle(ChatFormatting.GRAY));
            tooltip.addAll(MachineUpgradeHelper.getUpgradeTooltipDetails(displayType).stream().map(component -> component.copy().withStyle(ChatFormatting.DARK_GRAY)).toList());
            tooltip.add(Component.translatable("tooltip.microtech.machine_upgrade.compat", String.join(", ", displayType.getCompatibleMachines())).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Effective cap: " + MachineUpgradeHelper.getUpgradeCap(displayType) + " chips").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.machine_upgrade.active").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.microtech.machine_upgrade.compat_short").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.microtech.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
