package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.item.MicroTechTooltipHelper;
import Infinitygroup.microtech.menu.TechMinerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class TechMinerScreen extends AbstractContainerScreen<TechMinerMenu> {
    private static final float PANEL_TEXT_SCALE = 0.55F;
    private static final int BAR_MARGIN = 1;
    private static final int CANVAS_WIDTH = 182;
    private static final int CANVAS_HEIGHT = 240;
    private static final int TITLE_X = 76;
    private static final int TITLE_Y = 4;
    private static final int TITLE_WIDTH = 56;
    private static final int INFO_PANEL_X = 6;
    private static final int INFO_PANEL_Y = 52;
    private static final int INFO_PANEL_WIDTH = 82;
    private static final int INFO_PANEL_HEIGHT = 96;
    private static final int STATUS_LABEL_X = 10;
    private static final int STATUS_LABEL_Y = 76;
    private static final int PROGRESS_LABEL_X = 10;
    private static final int PROGRESS_LABEL_Y = 68;
    private static final int PROGRESS_BAR_X = 56;
    private static final int PROGRESS_BAR_Y = 70;
    private static final int PROGRESS_BAR_WIDTH = 30;
    private static final int PROGRESS_BAR_HEIGHT = 4;
    private static final int TARGETS_LABEL_X = 10;
    private static final int TARGETS_LABEL_Y = 84;
    private static final int FILTER_LABEL_X = 10;
    private static final int FILTER_LABEL_Y = 92;
    private static final int NEXT_TARGET_LABEL_X = 10;
    private static final int NEXT_TARGET_LABEL_Y = 108;
    private static final int UPGRADE_PANEL_X = 6;
    private static final int UPGRADE_PANEL_Y = 16;
    private static final int UPGRADE_PANEL_WIDTH = 84;
    private static final int UPGRADE_PANEL_HEIGHT = 34;
    private static final int ENERGY_PANEL_X = 136;
    private static final int ENERGY_PANEL_Y = 66;
    private static final int ENERGY_PANEL_WIDTH = 38;
    private static final int ENERGY_PANEL_HEIGHT = 82;
    private static final int ENERGY_BAR_X = 150;
    private static final int ENERGY_BAR_Y = 78;
    private static final int ENERGY_BAR_WIDTH = 10;
    private static final int ENERGY_BAR_HEIGHT = 66;
    private static final int BUTTON_SCAN_X = 96;
    private static final int BUTTON_SCAN_Y = 20;
    private static final int BUTTON_SCAN_WIDTH = 34;
    private static final int BUTTON_SCAN_HEIGHT = 14;
    private static final int BUTTON_INVENTORY_X = 134;
    private static final int BUTTON_INVENTORY_Y = 20;
    private static final int BUTTON_INVENTORY_WIDTH = 42;
    private static final int BUTTON_INVENTORY_HEIGHT = 14;
    private static final int BUTTON_CONFIG_X = 96;
    private static final int BUTTON_CONFIG_Y = 36;
    private static final int BUTTON_CONFIG_WIDTH = 34;
    private static final int BUTTON_CONFIG_HEIGHT = 16;
    private static final int BUTTON_START_STOP_X = 134;
    private static final int BUTTON_START_STOP_Y = 36;
    private static final int BUTTON_START_STOP_WIDTH = 36;
    private static final int BUTTON_START_STOP_HEIGHT = 16;
    private static final int PLAYER_INVENTORY_TITLE_X = 32;
    private static final int PLAYER_INVENTORY_TITLE_Y = 148;

    private Button inventoryButton;
    private Button configButton;
    private Button scanButton;
    private Button startStopButton;
    private List<Component> hoverTooltipLines = List.of();

    public TechMinerScreen(TechMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = CANVAS_WIDTH;
        this.imageHeight = CANVAS_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.inventory_button"),
                button -> this.clickMachineButton(3)
        ).bounds(this.leftPos + BUTTON_INVENTORY_X, this.topPos + BUTTON_INVENTORY_Y, BUTTON_INVENTORY_WIDTH, BUTTON_INVENTORY_HEIGHT).build());
        this.configButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.config"),
                button -> this.clickMachineButton(4)
        ).bounds(this.leftPos + BUTTON_CONFIG_X, this.topPos + BUTTON_CONFIG_Y, BUTTON_CONFIG_WIDTH, BUTTON_CONFIG_HEIGHT).build());
        this.scanButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.scan"),
                button -> this.clickMachineButton(0)
        ).bounds(this.leftPos + BUTTON_SCAN_X, this.topPos + BUTTON_SCAN_Y, BUTTON_SCAN_WIDTH, BUTTON_SCAN_HEIGHT).build());
        this.startStopButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.start"),
                button -> this.clickMachineButton(this.isRunning() ? 2 : 1)
        ).bounds(this.leftPos + BUTTON_START_STOP_X, this.topPos + BUTTON_START_STOP_Y, BUTTON_START_STOP_WIDTH, BUTTON_START_STOP_HEIGHT).build());
        this.updateButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF11161C);
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + this.imageHeight - 4, 0xFF20262D);
        guiGraphics.fill(left + INFO_PANEL_X, top + INFO_PANEL_Y, left + INFO_PANEL_X + INFO_PANEL_WIDTH, top + INFO_PANEL_Y + INFO_PANEL_HEIGHT, 0xFF151A20);
        guiGraphics.fill(left + UPGRADE_PANEL_X, top + UPGRADE_PANEL_Y, left + UPGRADE_PANEL_X + UPGRADE_PANEL_WIDTH, top + UPGRADE_PANEL_Y + UPGRADE_PANEL_HEIGHT, 0xFF171E25);
        guiGraphics.fill(left + ENERGY_PANEL_X, top + ENERGY_PANEL_Y, left + ENERGY_PANEL_X + ENERGY_PANEL_WIDTH, top + ENERGY_PANEL_Y + ENERGY_PANEL_HEIGHT, 0xFF10151B);

        this.drawPixelFrame(guiGraphics, left + INFO_PANEL_X, top + INFO_PANEL_Y, INFO_PANEL_WIDTH, INFO_PANEL_HEIGHT);
        this.drawPixelFrame(guiGraphics, left + UPGRADE_PANEL_X, top + UPGRADE_PANEL_Y, UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT);
        this.drawPixelFrame(guiGraphics, left + ENERGY_PANEL_X, top + ENERGY_PANEL_Y, ENERGY_PANEL_WIDTH, ENERGY_PANEL_HEIGHT);
        this.drawSlotBackgrounds(guiGraphics);

        this.drawEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);

        guiGraphics.fill(left + PROGRESS_BAR_X, top + PROGRESS_BAR_Y, left + PROGRESS_BAR_X + PROGRESS_BAR_WIDTH, top + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, 0xFF0C1015);
        guiGraphics.fill(left + PROGRESS_BAR_X + BAR_MARGIN, top + PROGRESS_BAR_Y + BAR_MARGIN, left + PROGRESS_BAR_X + PROGRESS_BAR_WIDTH - BAR_MARGIN, top + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT - BAR_MARGIN, 0xFF252C34);
        int innerWidth = Math.max(0, PROGRESS_BAR_WIDTH - BAR_MARGIN * 2);
        int progressWidth = MthFloorPercent(innerWidth, this.menu.getProgressPercent());
        guiGraphics.fill(left + PROGRESS_BAR_X + BAR_MARGIN, top + PROGRESS_BAR_Y + BAR_MARGIN, left + PROGRESS_BAR_X + BAR_MARGIN + progressWidth, top + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT - BAR_MARGIN, 0xFF36D5FF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoverTooltipLines = List.of();

        guiGraphics.drawString(this.font, this.title, TITLE_X, TITLE_Y, 0xEAFBFF, false);
        this.drawStringScaled(guiGraphics, Component.translatable("gui.microtech.tech_miner.info_panel"), INFO_PANEL_X + 4, INFO_PANEL_Y + 2, 0x6CE7FF, PANEL_TEXT_SCALE);
        this.drawStringScaled(guiGraphics, Component.translatable("gui.microtech.tech_miner.upgrades_panel"), UPGRADE_PANEL_X + 4, UPGRADE_PANEL_Y + 4, 0xEAFBFF, PANEL_TEXT_SCALE);
        this.drawStringScaled(guiGraphics, Component.translatable("gui.microtech.tech_miner.energy_short"), ENERGY_PANEL_X + 4, ENERGY_PANEL_Y + 3, 0xEAFBFF, PANEL_TEXT_SCALE);

        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.status", this.menu.getStatusText()), STATUS_LABEL_X, STATUS_LABEL_Y, 0xD8E0E6, false);

        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.progress_value", this.menu.getProgressPercent()), PROGRESS_LABEL_X, PROGRESS_LABEL_Y, 0xD8E0E6, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.targets", this.menu.getTargetCount()), TARGETS_LABEL_X, TARGETS_LABEL_Y, 0xD8E0E6, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.filter_value", this.menu.getFilterStatusText()), FILTER_LABEL_X, FILTER_LABEL_Y, 0x9FEFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.next_target", this.menu.getNextTargetText()), NEXT_TARGET_LABEL_X, NEXT_TARGET_LABEL_Y, 0x8FA4B5, false);
        Component energyText = this.menu.getMaxEnergy() <= 0
                ? Component.translatable("gui.microtech.tech_miner.energy_empty")
                : Component.translatable("gui.microtech.tech_miner.energy_value",
                MicroTechTooltipHelper.formatCompactNumber(this.menu.getEnergyStored()),
                MicroTechTooltipHelper.formatCompactNumber(this.menu.getMaxEnergy()));
        guiGraphics.drawString(this.font, energyText, ENERGY_PANEL_X + 4, ENERGY_PANEL_Y + 11, 0x9FEFFF, false);

        guiGraphics.drawString(this.font, this.playerInventoryTitle, PLAYER_INVENTORY_TITLE_X, PLAYER_INVENTORY_TITLE_Y, 0xEAFBFF, false);

        if (this.isMouseOverEnergyBar(mouseX, mouseY)) {
            int energy = this.menu.getEnergyStored();
            int max = this.menu.getMaxEnergy();
            this.hoverTooltipLines = List.of(Component.translatable("gui.microtech.energy", MicroTechTooltipHelper.formatFE(energy), MicroTechTooltipHelper.formatFE(max)));
        } else if (this.configButton != null && !this.configButton.active && this.configButton.isHoveredOrFocused()) {
            this.hoverTooltipLines = List.of(Component.translatable("tooltip.microtech.tech_miner.filter_requires_upgrade"));
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

    private boolean isRunning() {
        return this.menu.isProcessing();
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

    private void drawPixelFrame(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF5D6874);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF080B0F);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF5D6874);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF080B0F);
    }

    private void drawStringScaled(GuiGraphics guiGraphics, Component text, int x, int y, int color, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawSlotBackgrounds(GuiGraphics guiGraphics) {
        for (int slot = 0; slot < TechMinerMenu.UPGRADE_SLOT_COUNT; slot++) {
            this.drawSlot(guiGraphics, this.leftPos + TechMinerMenu.UPGRADE_SLOT_X[slot], this.topPos + TechMinerMenu.UPGRADE_Y);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.drawSlot(guiGraphics, this.leftPos + TechMinerMenu.PLAYER_INV_X + column * TechMinerMenu.SLOT_SIZE, this.topPos + TechMinerMenu.PLAYER_INV_Y + row * TechMinerMenu.SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            this.drawSlot(guiGraphics, this.leftPos + TechMinerMenu.PLAYER_INV_X + column * TechMinerMenu.SLOT_SIZE, this.topPos + TechMinerMenu.HOTBAR_Y);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + TechMinerMenu.SLOT_SIZE, y + TechMinerMenu.SLOT_SIZE, 0xFF59636E);
        guiGraphics.fill(x + 1, y + 1, x + TechMinerMenu.SLOT_SIZE - 1, y + TechMinerMenu.SLOT_SIZE - 1, 0xFF11161C);
    }

    private boolean isMouseOverEnergyBar(int mouseX, int mouseY) {
        int x = this.leftPos + ENERGY_BAR_X;
        int y = this.topPos + ENERGY_BAR_Y;
        return mouseX >= x && mouseX < x + ENERGY_BAR_WIDTH && mouseY >= y && mouseY < y + ENERGY_BAR_HEIGHT;
    }

    private void drawEnergyBar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF0F1216);
        guiGraphics.fill(x + BAR_MARGIN, y + BAR_MARGIN, x + width - BAR_MARGIN, y + height - BAR_MARGIN, 0xFF1A1F25);

        int max = this.menu.getMaxEnergy();
        int current = this.menu.getEnergyStored();
        if (max <= 0 || current <= 0) {
            return;
        }

        int innerHeight = Math.max(0, height - BAR_MARGIN * 2);
        int fillHeight = innerHeight * Math.min(current, max) / max;
        int innerBottom = y + BAR_MARGIN + innerHeight;
        guiGraphics.fill(x + BAR_MARGIN, innerBottom - fillHeight, x + width - BAR_MARGIN, innerBottom, 0xFF6CE7FF);
    }

    private static int MthFloorPercent(int width, int percent) {
        int clampedPercent = Math.max(0, Math.min(100, percent));
        return width * clampedPercent / 100;
    }
}
