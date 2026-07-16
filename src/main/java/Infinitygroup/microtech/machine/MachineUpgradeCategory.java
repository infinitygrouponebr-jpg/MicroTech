package Infinitygroup.microtech.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum MachineUpgradeCategory {
    UNIVERSAL("tooltip.microtech.machine_upgrade.category.universal", ChatFormatting.AQUA),
    MINING("tooltip.microtech.machine_upgrade.category.mining", ChatFormatting.GREEN),
    PROCESSING("tooltip.microtech.machine_upgrade.category.processing", ChatFormatting.GOLD),
    ENERGY_STORAGE("tooltip.microtech.machine_upgrade.category.energy_storage", ChatFormatting.BLUE),
    GENERATION("tooltip.microtech.machine_upgrade.category.generation", ChatFormatting.YELLOW),
    SPECIAL("tooltip.microtech.machine_upgrade.category.special", ChatFormatting.LIGHT_PURPLE);

    private final String translationKey;
    private final ChatFormatting color;

    MachineUpgradeCategory(String translationKey, ChatFormatting color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public ChatFormatting getColor() {
        return this.color;
    }

    public Component getComponent() {
        return Component.translatable(this.translationKey).withStyle(this.color);
    }
}
