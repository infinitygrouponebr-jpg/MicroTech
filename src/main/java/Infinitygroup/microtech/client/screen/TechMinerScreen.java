package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.item.MicroTechTooltipHelper;
import Infinitygroup.microtech.menu.TechMinerGuiLayout;
import Infinitygroup.microtech.menu.TechMinerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class TechMinerScreen extends AbstractContainerScreen<TechMinerMenu> {
    private static final boolean DEBUG_LAYOUT = false;
    private static final int BACKGROUND = 0xFF11161C;
    private static final int SURFACE = 0xFF20262D;
    private static final int PANEL = 0xFF151A20;
    private static final int PANEL_DARK = 0xFF10151B;
    private static final int TEXT = 0xFFEAFBFF;
    private static final int TEXT_MUTED = 0xFFD8E0E6;
    private static final int TEXT_ACCENT = 0xFF9FEFFF;
    private static final int TEXT_SUBTLE = 0xFF8FA4B5;

    private Button inventoryButton;
    private Button configButton;
    private Button scanButton;
    private Button startStopButton;
    private List<Component> hoverTooltipLines = List.of();

    public TechMinerScreen(TechMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TechMinerGuiLayout.GUI_WIDTH;
        this.imageHeight = TechMinerGuiLayout.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.inventory_button"),
                button -> this.clickMachineButton(3)
        ).bounds(this.leftPos + TechMinerGuiLayout.INVENTORY_BUTTON.x(), this.topPos + TechMinerGuiLayout.INVENTORY_BUTTON.y(),
                TechMinerGuiLayout.INVENTORY_BUTTON.width(), TechMinerGuiLayout.INVENTORY_BUTTON.height()).build());
        this.configButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.config"),
                button -> this.clickMachineButton(4)
        ).bounds(this.leftPos + TechMinerGuiLayout.CONFIG_BUTTON.x(), this.topPos + TechMinerGuiLayout.CONFIG_BUTTON.y(),
                TechMinerGuiLayout.CONFIG_BUTTON.width(), TechMinerGuiLayout.CONFIG_BUTTON.height()).build());
        this.scanButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.scan"),
                button -> this.clickMachineButton(0)
        ).bounds(this.leftPos + TechMinerGuiLayout.SCAN_BUTTON.x(), this.topPos + TechMinerGuiLayout.SCAN_BUTTON.y(),
                TechMinerGuiLayout.SCAN_BUTTON.width(), TechMinerGuiLayout.SCAN_BUTTON.height()).build());
        this.startStopButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.start"),
                button -> this.clickMachineButton(this.menu.isProcessing() ? 2 : 1)
        ).bounds(this.leftPos + TechMinerGuiLayout.START_STOP_BUTTON.x(), this.topPos + TechMinerGuiLayout.START_STOP_BUTTON.y(),
                TechMinerGuiLayout.START_STOP_BUTTON.width(), TechMinerGuiLayout.START_STOP_BUTTON.height()).build());
        this.updateButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, BACKGROUND);
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + this.imageHeight - 4, SURFACE);

        this.drawPanel(guiGraphics, left, top, TechMinerGuiLayout.INFO_PANEL, PANEL);
        this.drawPanel(guiGraphics, left, top, TechMinerGuiLayout.UPGRADES_PANEL, PANEL);
        this.drawPanel(guiGraphics, left, top, TechMinerGuiLayout.ENERGY_PANEL, PANEL_DARK);
        this.drawSlotBackgrounds(guiGraphics);
        this.drawProgressBar(guiGraphics, left, top);
        this.drawEnergyBar(guiGraphics, left, top);
        if (DEBUG_LAYOUT) {
            this.drawDebugLayout(guiGraphics, left, top);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoverTooltipLines = List.of();

        this.drawCentered(guiGraphics, this.title, TechMinerGuiLayout.TITLE, TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.info_panel"),
                TechMinerGuiLayout.INFO_TITLE.x(), TechMinerGuiLayout.INFO_TITLE.y(), TEXT_ACCENT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.upgrades_panel"),
                TechMinerGuiLayout.UPGRADES_TITLE.x(), TechMinerGuiLayout.UPGRADES_TITLE.y(), TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.energy_short"),
                TechMinerGuiLayout.ENERGY_TITLE.x(), TechMinerGuiLayout.ENERGY_TITLE.y(), TEXT, false);

        int infoWidth = TechMinerGuiLayout.INFO_PANEL.width() - 12;
        this.drawFitted(guiGraphics, Component.translatable("gui.microtech.status", this.menu.getStatusText()),
                TechMinerGuiLayout.STATUS_TEXT, infoWidth, TEXT_MUTED);
        this.drawFitted(guiGraphics, Component.translatable("gui.microtech.tech_miner.targets", this.menu.getTargetCount()),
                TechMinerGuiLayout.TARGETS_TEXT, infoWidth, TEXT_MUTED);
        this.drawFitted(guiGraphics, Component.translatable("gui.microtech.tech_miner.filter_value", this.menu.getFilterStatusText()),
                TechMinerGuiLayout.FILTER_TEXT, infoWidth, TEXT_ACCENT);

        Component nextTarget = Component.translatable("gui.microtech.tech_miner.next_target", this.menu.getNextTargetText());
        this.drawFitted(guiGraphics, nextTarget, TechMinerGuiLayout.NEXT_TARGET_TEXT, infoWidth, TEXT_SUBTLE);
        this.drawFitted(guiGraphics, Component.translatable("gui.microtech.tech_miner.progress_value", this.menu.getProgressPercent()),
                TechMinerGuiLayout.PROGRESS_TEXT, infoWidth, TEXT_MUTED);

        this.drawCentered(guiGraphics, Component.translatable("gui.microtech.tech_miner.energy_percent", this.getEnergyPercent()),
                new TechMinerGuiLayout.Rect(TechMinerGuiLayout.ENERGY_PANEL.x(), TechMinerGuiLayout.ENERGY_PERCENT.y(),
                        TechMinerGuiLayout.ENERGY_PANEL.width(), 9), TEXT_ACCENT);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                TechMinerGuiLayout.PLAYER_INVENTORY_TITLE.x(), TechMinerGuiLayout.PLAYER_INVENTORY_TITLE.y(), TEXT, false);

        if (this.isMouseOverEnergyBar(mouseX, mouseY)) {
            this.hoverTooltipLines = List.of(Component.translatable("gui.microtech.energy",
                    MicroTechTooltipHelper.formatFE(this.menu.getEnergyStored()),
                    MicroTechTooltipHelper.formatFE(this.menu.getMaxEnergy())));
        } else if (this.isMouseOverNextTarget(mouseX, mouseY)) {
            this.hoverTooltipLines = this.menu.getNextTargetTooltip();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.updateButtonState();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!this.hoverTooltipLines.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.hoverTooltipLines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void clickMachineButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void updateButtonState() {
        boolean running = this.menu.isProcessing();
        if (this.inventoryButton != null) {
            this.inventoryButton.active = true;
        }
        if (this.configButton != null) {
            this.configButton.active = true;
        }
        if (this.scanButton != null) {
            this.scanButton.active = true;
        }
        if (this.startStopButton != null) {
            this.startStopButton.setMessage(Component.translatable(running ? "gui.microtech.tech_miner.stop" : "gui.microtech.tech_miner.start"));
            this.startStopButton.active = true;
        }
    }

    private void drawPanel(GuiGraphics guiGraphics, int left, int top, TechMinerGuiLayout.Rect rect, int color) {
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.right(), top + rect.bottom(), color);
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.right(), top + rect.y() + 1, 0xFF5D6874);
        guiGraphics.fill(left + rect.x(), top + rect.bottom() - 1, left + rect.right(), top + rect.bottom(), 0xFF080B0F);
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.x() + 1, top + rect.bottom(), 0xFF5D6874);
        guiGraphics.fill(left + rect.right() - 1, top + rect.y(), left + rect.right(), top + rect.bottom(), 0xFF080B0F);
    }

    private void drawProgressBar(GuiGraphics guiGraphics, int left, int top) {
        TechMinerGuiLayout.Rect bar = TechMinerGuiLayout.PROGRESS_BAR;
        int inset = TechMinerGuiLayout.BAR_INSET;
        int innerWidth = Math.max(0, bar.width() - inset * 2);
        int innerHeight = Math.max(0, bar.height() - inset * 2);
        int filledWidth = innerWidth * Math.max(0, Math.min(100, this.menu.getProgressPercent())) / 100;

        guiGraphics.fill(left + bar.x(), top + bar.y(), left + bar.right(), top + bar.bottom(), 0xFF0C1015);
        guiGraphics.fill(left + bar.x() + inset, top + bar.y() + inset, left + bar.x() + inset + innerWidth, top + bar.y() + inset + innerHeight, 0xFF252C34);
        guiGraphics.fill(left + bar.x() + inset, top + bar.y() + inset, left + bar.x() + inset + filledWidth, top + bar.y() + inset + innerHeight, 0xFF36D5FF);
    }

    private void drawEnergyBar(GuiGraphics guiGraphics, int left, int top) {
        TechMinerGuiLayout.Rect bar = TechMinerGuiLayout.ENERGY_BAR;
        int inset = TechMinerGuiLayout.BAR_INSET;
        int innerHeight = Math.max(0, bar.height() - inset * 2);
        int innerWidth = Math.max(0, bar.width() - inset * 2);
        int max = this.menu.getMaxEnergy();
        int current = this.menu.getEnergyStored();
        int filledHeight = max <= 0 || current <= 0 ? 0 : innerHeight * Math.min(current, max) / max;
        int innerX = left + bar.x() + inset;
        int innerY = top + bar.y() + inset;
        int innerBottom = innerY + innerHeight;

        guiGraphics.fill(left + bar.x(), top + bar.y(), left + bar.right(), top + bar.bottom(), 0xFF0F1216);
        guiGraphics.fill(innerX, innerY, innerX + innerWidth, innerBottom, 0xFF1A1F25);
        guiGraphics.fill(innerX, innerBottom - filledHeight, innerX + innerWidth, innerBottom, 0xFF6CE7FF);
    }

    private void drawSlotBackgrounds(GuiGraphics guiGraphics) {
        for (TechMinerGuiLayout.Pos slot : TechMinerGuiLayout.UPGRADE_SLOTS) {
            this.drawSlot(guiGraphics, this.leftPos + slot.x(), this.topPos + slot.y());
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.drawSlot(guiGraphics,
                        this.leftPos + TechMinerGuiLayout.PLAYER_INVENTORY.x() + column * TechMinerGuiLayout.SLOT_SIZE,
                        this.topPos + TechMinerGuiLayout.PLAYER_INVENTORY.y() + row * TechMinerGuiLayout.SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            this.drawSlot(guiGraphics,
                    this.leftPos + TechMinerGuiLayout.PLAYER_HOTBAR.x() + column * TechMinerGuiLayout.SLOT_SIZE,
                    this.topPos + TechMinerGuiLayout.PLAYER_HOTBAR.y());
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + TechMinerGuiLayout.SLOT_SIZE, y + TechMinerGuiLayout.SLOT_SIZE, 0xFF59636E);
        guiGraphics.fill(x + 1, y + 1, x + TechMinerGuiLayout.SLOT_SIZE - 1, y + TechMinerGuiLayout.SLOT_SIZE - 1, 0xFF11161C);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, TechMinerGuiLayout.Rect area, int color) {
        int x = area.x() + (area.width() - this.font.width(text)) / 2;
        guiGraphics.drawString(this.font, text, x, area.y(), color, false);
    }

    private void drawFitted(GuiGraphics guiGraphics, Component text, TechMinerGuiLayout.Pos pos, int maxWidth, int color) {
        String value = text.getString();
        if (this.font.width(value) > maxWidth) {
            int ellipsisWidth = this.font.width("...");
            value = this.font.plainSubstrByWidth(value, Math.max(0, maxWidth - ellipsisWidth)) + "...";
        }
        guiGraphics.drawString(this.font, value, pos.x(), pos.y(), color, false);
    }

    private int getEnergyPercent() {
        int max = this.menu.getMaxEnergy();
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, this.menu.getEnergyStored() * 100 / max));
    }

    private boolean isMouseOverEnergyBar(int mouseX, int mouseY) {
        TechMinerGuiLayout.Rect bar = TechMinerGuiLayout.ENERGY_BAR;
        int x = this.leftPos + bar.x();
        int y = this.topPos + bar.y();
        return mouseX >= x && mouseX < x + bar.width() && mouseY >= y && mouseY < y + bar.height();
    }

    private boolean isMouseOverNextTarget(int mouseX, int mouseY) {
        int x = this.leftPos + TechMinerGuiLayout.NEXT_TARGET_TEXT.x();
        int y = this.topPos + TechMinerGuiLayout.NEXT_TARGET_TEXT.y();
        int width = TechMinerGuiLayout.INFO_PANEL.width() - 12;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + this.font.lineHeight;
    }

    private void drawDebugLayout(GuiGraphics guiGraphics, int left, int top) {
        this.drawOutline(guiGraphics, left, top, new TechMinerGuiLayout.Rect(0, 0, TechMinerGuiLayout.GUI_WIDTH, TechMinerGuiLayout.GUI_HEIGHT), 0xFFFF0000);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.INFO_PANEL, 0xFFFFFF00);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.UPGRADES_PANEL, 0xFFFFFF00);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.ENERGY_PANEL, 0xFFFFFF00);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.INVENTORY_BUTTON, 0xFF00FF00);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.CONFIG_BUTTON, 0xFF00FF00);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.SCAN_BUTTON, 0xFF00FF00);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.START_STOP_BUTTON, 0xFF00FF00);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.PROGRESS_BAR, 0xFF00FFFF);
        this.drawOutline(guiGraphics, left + TechMinerGuiLayout.BAR_INSET, top + TechMinerGuiLayout.BAR_INSET,
                new TechMinerGuiLayout.Rect(TechMinerGuiLayout.PROGRESS_BAR.x(), TechMinerGuiLayout.PROGRESS_BAR.y(),
                        TechMinerGuiLayout.PROGRESS_BAR.width() - TechMinerGuiLayout.BAR_INSET * 2,
                        TechMinerGuiLayout.PROGRESS_BAR.height() - TechMinerGuiLayout.BAR_INSET * 2), 0xFF0088FF);
        this.drawOutline(guiGraphics, left, top, TechMinerGuiLayout.ENERGY_BAR, 0xFF00FFFF);
        for (TechMinerGuiLayout.Pos slot : TechMinerGuiLayout.UPGRADE_SLOTS) {
            this.drawOutline(guiGraphics, left, top, new TechMinerGuiLayout.Rect(slot.x(), slot.y(), TechMinerGuiLayout.SLOT_SIZE, TechMinerGuiLayout.SLOT_SIZE), 0xFFFF00FF);
        }
    }

    private void drawOutline(GuiGraphics guiGraphics, int left, int top, TechMinerGuiLayout.Rect rect, int color) {
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.right(), top + rect.y() + 1, color);
        guiGraphics.fill(left + rect.x(), top + rect.bottom() - 1, left + rect.right(), top + rect.bottom(), color);
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.x() + 1, top + rect.bottom(), color);
        guiGraphics.fill(left + rect.right() - 1, top + rect.y(), left + rect.right(), top + rect.bottom(), color);
    }
}
