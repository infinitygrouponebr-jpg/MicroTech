package Infinitygroup.microtech.item;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MicroTechTooltipHelper {
    private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    private MicroTechTooltipHelper() {
    }

    public static String formatFE(int value) {
        return INTEGER_FORMAT.format(Math.max(0, value));
    }

    public static String formatCompactNumber(int value) {
        int safeValue = Math.max(0, value);
        if (safeValue < 1_000) {
            return Integer.toString(safeValue);
        }
        if (safeValue < 1_000_000) {
            return (safeValue / 1_000) + "k";
        }
        if (safeValue < 1_000_000_000) {
            return (safeValue / 1_000_000) + "M";
        }
        return (safeValue / 1_000_000_000) + "B";
    }

    public static String formatPercent(int current, int max) {
        if (max <= 0) {
            return "0%";
        }

        int percent = Math.round(Math.max(0, current) * 100.0F / max);
        return percent + "%";
    }

    public static boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    public static void addHint(List<Component> tooltip, String translationKey) {
        tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void addHoldShiftHint(List<Component> tooltip) {
        if (!isShiftDown()) {
            addHint(tooltip, "tooltip.microtech.hold_shift");
        }
    }

    public static void addHeader(List<Component> tooltip, String translationKey) {
        tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.AQUA));
    }

    public static void addDetail(List<Component> tooltip, String translationKey) {
        tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.GRAY));
    }

    public static void addImportant(List<Component> tooltip, String translationKey) {
        tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.WHITE));
    }

    public static void addEnergyLine(List<Component> tooltip, String translationKey, int current, int max) {
        tooltip.add(Component.translatable(
                translationKey,
                formatFE(current),
                formatFE(max)
        ).withStyle(ChatFormatting.AQUA));
    }

    public static void addRateLine(List<Component> tooltip, String translationKey, int amountPerTick) {
        tooltip.add(Component.translatable(
                translationKey,
                formatFE(amountPerTick)
        ).withStyle(ChatFormatting.GRAY));
    }

    public static void addPercentageLine(List<Component> tooltip, String translationKey, int current, int max, ChatFormatting color) {
        tooltip.add(Component.translatable(
                translationKey,
                formatPercent(current, max)
        ).withStyle(color));
    }
}
