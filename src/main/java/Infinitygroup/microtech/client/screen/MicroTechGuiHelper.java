package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.block.entity.BasicMachineBlockEntity;
import Infinitygroup.microtech.block.entity.BatteryBlockEntity;
import Infinitygroup.microtech.block.entity.BatteryT2BlockEntity;
import Infinitygroup.microtech.block.entity.TechCrusherBlockEntity;
import Infinitygroup.microtech.block.entity.EvoTableBlockEntity;
import Infinitygroup.microtech.block.entity.SolarPanelBlockEntity;
import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.item.MicroTechTooltipHelper;
import Infinitygroup.microtech.item.TechArmorEnergyHelper;
import Infinitygroup.microtech.machine.MachineStatus;
import Infinitygroup.microtech.menu.BasicMachineMenu;
import Infinitygroup.microtech.menu.BatteryMenu;
import Infinitygroup.microtech.menu.BatteryT2Menu;
import Infinitygroup.microtech.menu.ElectricFurnaceMenu;
import Infinitygroup.microtech.menu.EvoTableMenu;
import Infinitygroup.microtech.menu.TechCrusherMenu;
import Infinitygroup.microtech.menu.SolarPanelMenu;
import Infinitygroup.microtech.menu.TechMinerMenu;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public final class MicroTechGuiHelper {
    private static final NumberFormat FULL_FE_US = NumberFormat.getIntegerInstance(Locale.US);
    private static final NumberFormat FULL_FE_BR = NumberFormat.getIntegerInstance(new Locale("pt", "BR"));

    private MicroTechGuiHelper() {
    }

    public static int getStatusColor(MachineStatus status) {
        return status != null ? status.getColor() : MachineStatus.IDLE.getColor();
    }

    public static void drawEnergyLine(GuiGraphics guiGraphics, Font font, int x, int y, String labelKey, int current, int max) {
        guiGraphics.drawString(font, Component.translatable(labelKey, MicroTechTooltipHelper.formatFE(current), MicroTechTooltipHelper.formatFE(max)), x, y, 0xA0E0FF, false);
    }

    public static void drawCompactEnergyLine(GuiGraphics guiGraphics, Font font, int x, int y, String labelKey, int current, int max) {
        guiGraphics.drawString(font, Component.translatable(labelKey, MicroTechTooltipHelper.formatCompactNumber(current), MicroTechTooltipHelper.formatCompactNumber(max)), x, y, 0xA0E0FF, false);
    }

    public static String formatCompactFE(int value) {
        return MicroTechTooltipHelper.formatCompactNumber(value);
    }

    public static String formatRateFE(int value) {
        return formatFullFE(value);
    }

    public static String formatFullFE(int value) {
        NumberFormat format = isPortugueseLanguage() ? FULL_FE_BR : FULL_FE_US;
        return format.format(Math.max(0, value));
    }

    public static void drawPercentLine(GuiGraphics guiGraphics, Font font, int x, int y, String labelKey, int current, int max) {
        guiGraphics.drawString(font, Component.translatable(labelKey, MicroTechTooltipHelper.formatPercent(current, max)), x, y, 0xD0D0D0, false);
    }

    public static void drawRateLine(GuiGraphics guiGraphics, Font font, int x, int y, String labelKey, int rate) {
        guiGraphics.drawString(font, Component.translatable(labelKey, formatRateFE(rate)), x, y, 0xD0D0D0, false);
    }

    public static MachineStatus getTechCrusherStatus(TechCrusherMenu menu, Level level) {
        TechCrusherBlockEntity blockEntity = menu.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getStatus(level) : MachineStatus.IDLE;
    }

    public static void drawStatus(GuiGraphics guiGraphics, Font font, int x, int y, MachineStatus status) {
        guiGraphics.drawString(font, Component.translatable("gui.microtech.status", status.getText()), x, y, getStatusColor(status), false);
    }

    public static void drawProgress(GuiGraphics guiGraphics, int x, int y, int width, int color, int progress, int maxProgress) {
        int progressWidth = maxProgress <= 0 ? 0 : progress * width / maxProgress;
        guiGraphics.fill(x, y, x + progressWidth, y + 3, color);
    }

    public static void drawVerticalEnergyBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int current, int max) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0F1216);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF1A1F25);

        if (max <= 0 || current <= 0) {
            return;
        }

        int innerHeight = Math.max(0, height - 2);
        int fillHeight = Math.max(0, innerHeight * Math.min(current, max) / max);
        int fillTop = y + height - 1 - fillHeight;
        guiGraphics.fill(x + 1, fillTop, x + width - 1, y + height - 1, 0xFF6CE7FF);
        guiGraphics.fill(x + 1, fillTop, x + width - 1, Math.min(y + height - 1, fillTop + 1), 0xFFB8F3FF);
    }

    public static List<Component> buildEnergyTooltip(String titleKey, int current, int max) {
        return List.of(
                Component.translatable(titleKey),
                Component.literal(formatFullFE(current) + " / " + formatFullFE(max) + " FE"),
                Component.literal(MicroTechTooltipHelper.formatPercent(current, max))
        );
    }

    public static boolean isHovering(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static String trimToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        int ellipsisWidth = font.width("...");
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - ellipsisWidth)) + "...";
    }

    public static String trimToWidth(Font font, Component component, int maxWidth) {
        return trimToWidth(font, component.getString(), maxWidth);
    }

    public static boolean isTruncated(Font font, String text, int maxWidth) {
        return font.width(text) > maxWidth;
    }

    public static boolean isTruncated(Font font, Component text, int maxWidth) {
        return isTruncated(font, text.getString(), maxWidth);
    }

    public static MachineStatus getBatteryStatus(BatteryMenu menu) {
        int energy = menu.getEnergyStored();
        int max = menu.getMaxEnergy();
        if (energy <= 0) {
            return MachineStatus.NO_POWER;
        }
        if (energy >= max) {
            return MachineStatus.FULL;
        }

        return switch (menu.getChargingStatus()) {
            case BatteryBlockEntity.STATUS_CHARGING -> MachineStatus.CHARGING;
            case BatteryBlockEntity.STATUS_DISCHARGING -> MachineStatus.DISCHARGING;
            default -> MachineStatus.IDLE;
        };
    }

    public static MachineStatus getBatteryT2Status(BatteryT2Menu menu) {
        ItemStack stack = menu.getChargingStack();
        if (stack.isEmpty()) {
            return MachineStatus.IDLE;
        }

        int itemMax = menu.getChargingItemMaxEnergy();
        if (itemMax <= 0) {
            return MachineStatus.INVALID_ITEM;
        }

        int itemEnergy = menu.getChargingItemEnergyStored();
        if (itemEnergy >= itemMax) {
            return MachineStatus.FULL;
        }

        return switch (menu.getChargingStatus()) {
            case BatteryT2BlockEntity.STATUS_INCOMPATIBLE -> MachineStatus.INVALID_ITEM;
            case BatteryT2BlockEntity.STATUS_FULL -> MachineStatus.FULL;
            case BatteryT2BlockEntity.STATUS_CHARGING -> MachineStatus.CHARGING;
            default -> menu.getEnergyStored() <= 0 ? MachineStatus.NO_POWER : MachineStatus.IDLE;
        };
    }

    public static MachineStatus getEnergyConverterStatus(BasicMachineMenu menu) {
        if (menu.getEnergyStored() >= menu.getMaxEnergy()) {
            return MachineStatus.FULL;
        }

        ItemStack input = menu.getInputStack();
        if (input.isEmpty()) {
            return MachineStatus.NO_INPUT;
        }

        if (!BasicMachineBlockEntity.isConvertible(input)) {
            return MachineStatus.INVALID_ITEM;
        }

        return menu.getPendingEnergy() > 0 ? MachineStatus.RUNNING : MachineStatus.WAITING;
    }

    public static MachineStatus getElectricFurnaceStatus(ElectricFurnaceMenu menu) {
        ItemStack input = menu.getInputStack();
        if (input.isEmpty()) {
            return MachineStatus.NO_INPUT;
        }

        if (menu.getProgress() > 0) {
            return MachineStatus.PROCESSING;
        }

        if (menu.getEnergyStored() <= 0) {
            return MachineStatus.NO_POWER;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return MachineStatus.WAITING;
        }

        RecipeHolder<SmeltingRecipe> recipe = minecraft.level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), minecraft.level)
                .orElse(null);
        if (recipe == null) {
            return MachineStatus.INVALID_ITEM;
        }

        ItemStack result = recipe.value().getResultItem(minecraft.level.registryAccess()).copy();
        ItemStack output = menu.getOutputStack();
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, result) || output.getCount() + result.getCount() > output.getMaxStackSize()) {
                return MachineStatus.BLOCKED;
            }
        }

        return MachineStatus.READY;
    }

    public static MachineStatus getEvoStatus(EvoTableMenu menu) {
        EvoTableBlockEntity.EvoStatus status = menu.getStatus();
        return switch (status) {
            case INACTIVE -> MachineStatus.IDLE;
            case INVALID_ITEM, INVALID_CHIP, FLIGHT_INVALID_ITEM, FLIGHT_INVALID_CHIP -> MachineStatus.INVALID_ITEM;
            case READY, FLIGHT_READY -> MachineStatus.READY;
            case EVOLVING, FLIGHT_INSTALLING -> MachineStatus.PROCESSING;
            case CHIP_INSTALLED, CHIP_UPGRADED, CHIP_MAX_LEVEL, FLIGHT_INSTALLED, FLIGHT_ALREADY_INSTALLED -> MachineStatus.FULL;
        };
    }

    public static MachineStatus getSolarStatus(SolarPanelMenu menu) {
        SolarPanelBlockEntity.SolarStatus status = menu.getStatus();
        return switch (status) {
            case GENERATING -> MachineStatus.RUNNING;
            case BLOCKED -> MachineStatus.BLOCKED;
            case NO_SUN, NIGHT, RAIN -> MachineStatus.NO_POWER;
        };
    }

    public static MachineStatus getTechMinerStatus(TechMinerMenu menu, net.minecraft.world.level.Level level) {
        TechMinerBlockEntity blockEntity = menu.getBlockEntity(level);
        return blockEntity != null ? blockEntity.getStatus() : MachineStatus.IDLE;
    }

    public static Component formatItemEnergy(int current, int max) {
        if (max <= 0) {
            return Component.translatable("gui.microtech.item_empty");
        }
        return Component.translatable("gui.microtech.item_energy", MicroTechTooltipHelper.formatFE(current), MicroTechTooltipHelper.formatFE(max));
    }

    private static boolean isPortugueseLanguage() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return false;
        }

        String languageCode = minecraft.options.languageCode;
        return languageCode != null && languageCode.toLowerCase(Locale.ROOT).startsWith("pt");
    }
}
