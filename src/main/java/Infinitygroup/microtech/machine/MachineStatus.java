package Infinitygroup.microtech.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum MachineStatus {
    RUNNING("status.microtech.running", 0x6EE7A8),
    IDLE("status.microtech.idle", 0xD0D0D0),
    NO_POWER("status.microtech.no_power", 0xFF8A8A),
    FULL("status.microtech.full", 0x6CE7FF),
    BLOCKED("status.microtech.blocked", 0xFF8A8A),
    NO_INPUT("status.microtech.no_input", 0xFFCC6C),
    NO_OUTPUT("status.microtech.no_output", 0xFF8A8A),
    NO_TARGETS("status.microtech.no_targets", 0xD0D0D0),
    CHARGING("status.microtech.charging", 0xB9F4FF),
    DISCHARGING("status.microtech.discharging", 0xF5D36C),
    SCANNING("status.microtech.scanning", 0xF5D36C),
    WAITING("status.microtech.waiting", 0xD0D0D0),
    PAUSED("status.microtech.paused", 0xB8C0C8),
    INVALID_ITEM("status.microtech.invalid_item", 0xFF8A8A),
    READY("status.microtech.ready", 0x7DF5A2),
    PROCESSING("status.microtech.processing", 0xF5D36C);

    private final String translationKey;
    private final int color;

    MachineStatus(String translationKey, int color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public int getColor() {
        return this.color;
    }

    public Component getText() {
        return Component.translatable(this.translationKey);
    }

    public ChatFormatting getFormatting() {
        return switch (this) {
            case RUNNING, READY, CHARGING -> ChatFormatting.GREEN;
            case FULL -> ChatFormatting.AQUA;
            case NO_POWER, BLOCKED, NO_OUTPUT, INVALID_ITEM -> ChatFormatting.RED;
            case NO_INPUT, PROCESSING, SCANNING -> ChatFormatting.GOLD;
            case IDLE, WAITING, PAUSED, DISCHARGING, NO_TARGETS -> ChatFormatting.GRAY;
        };
    }
}
