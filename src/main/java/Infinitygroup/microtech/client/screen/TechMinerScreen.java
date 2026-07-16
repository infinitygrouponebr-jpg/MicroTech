package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.item.MicroTechTooltipHelper;
import Infinitygroup.microtech.machine.MachineStatus;
import Infinitygroup.microtech.menu.TechMinerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import java.util.List;

public class TechMinerScreen extends AbstractContainerScreen<TechMinerMenu> {
    private static final int ENERGY_BAR_X = 245;
    private static final int ENERGY_BAR_Y = 36;
    private static final int ENERGY_BAR_WIDTH = 10;
    private static final int ENERGY_BAR_HEIGHT = 66;
    private static final int INFO_PANEL_X = 10;
    private static final int INFO_PANEL_Y = 24;
    private static final int INFO_PANEL_WIDTH = 146;
    private static final int INFO_PANEL_HEIGHT = 92;
    private static final int INFO_X = 16;
    private static final int INFO_Y = 42;
    private static final int INFO_WIDTH = 134;
    private static final int PROGRESS_X = 20;
    private static final int PROGRESS_Y = 78;
    private static final int PROGRESS_WIDTH = 118;
    private static final int PROGRESS_HEIGHT = 7;
    private static final int UPGRADE_PANEL_X = 164;
    private static final int UPGRADE_PANEL_Y = 24;
    private static final int UPGRADE_PANEL_WIDTH = 56;
    private static final int UPGRADE_PANEL_HEIGHT = 92;
    private static final int ENERGY_PANEL_X = 230;
    private static final int ENERGY_PANEL_Y = 24;
    private static final int ENERGY_PANEL_WIDTH = 34;
    private static final int ENERGY_PANEL_HEIGHT = 92;
    private static final int CONTROL_PANEL_X = 8;
    private static final int CONTROL_PANEL_Y = 114;
    private static final int CONTROL_PANEL_WIDTH = 260;
    private static final int CONTROL_PANEL_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 62;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_ONE_X = 10;
    private static final int BUTTON_TWO_X = 76;
    private static final int BUTTON_THREE_X = 142;
    private static final int BUTTON_FOUR_X = 208;
    private static final int BUTTON_ROW_Y = 117;
    private static final int PLAYER_PANEL_X = 48;
    private static final int PLAYER_PANEL_Y = 144;
    private static final int PLAYER_PANEL_WIDTH = 180;
    private static final int PLAYER_PANEL_HEIGHT = 86;

    private Button inventoryButton;
    private Button configButton;
    private Button scanButton;
    private Button startStopButton;
    private List<Component> hoverTooltipLines = List.of();

    public TechMinerScreen(TechMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 276;
        this.imageHeight = 232;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.inventory_button"),
                button -> this.clickMachineButton(3)
        ).bounds(this.leftPos + BUTTON_ONE_X, this.topPos + BUTTON_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.configButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.config"),
                button -> this.clickMachineButton(4)
        ).bounds(this.leftPos + BUTTON_TWO_X, this.topPos + BUTTON_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.scanButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.scan"),
                button -> this.clickMachineButton(0)
        ).bounds(this.leftPos + BUTTON_THREE_X, this.topPos + BUTTON_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.startStopButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.start"),
                button -> this.clickMachineButton(this.isRunning() ? 2 : 1)
        ).bounds(this.leftPos + BUTTON_FOUR_X, this.topPos + BUTTON_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
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
        guiGraphics.fill(left + CONTROL_PANEL_X, top + CONTROL_PANEL_Y, left + CONTROL_PANEL_X + CONTROL_PANEL_WIDTH, top + CONTROL_PANEL_Y + CONTROL_PANEL_HEIGHT, 0xFF151A20);
        guiGraphics.fill(left + PLAYER_PANEL_X, top + PLAYER_PANEL_Y, left + PLAYER_PANEL_X + PLAYER_PANEL_WIDTH, top + PLAYER_PANEL_Y + PLAYER_PANEL_HEIGHT, 0xFF181E25);

        this.drawPixelFrame(guiGraphics, left + INFO_PANEL_X, top + INFO_PANEL_Y, INFO_PANEL_WIDTH, INFO_PANEL_HEIGHT);
        this.drawPixelFrame(guiGraphics, left + UPGRADE_PANEL_X, top + UPGRADE_PANEL_Y, UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT);
        this.drawPixelFrame(guiGraphics, left + ENERGY_PANEL_X, top + ENERGY_PANEL_Y, ENERGY_PANEL_WIDTH, ENERGY_PANEL_HEIGHT);
        this.drawPixelFrame(guiGraphics, left + CONTROL_PANEL_X, top + CONTROL_PANEL_Y, CONTROL_PANEL_WIDTH, CONTROL_PANEL_HEIGHT);
        this.drawPixelFrame(guiGraphics, left + PLAYER_PANEL_X, top + PLAYER_PANEL_Y, PLAYER_PANEL_WIDTH, PLAYER_PANEL_HEIGHT);
        this.drawSlotBackgrounds(guiGraphics);

        MicroTechGuiHelper.drawVerticalEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, this.menu.getEnergyStored(), this.menu.getMaxEnergy());

        guiGraphics.fill(left + PROGRESS_X, top + PROGRESS_Y, left + PROGRESS_X + PROGRESS_WIDTH, top + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF0C1015);
        guiGraphics.fill(left + PROGRESS_X + 1, top + PROGRESS_Y + 1, left + PROGRESS_X + PROGRESS_WIDTH - 1, top + PROGRESS_Y + PROGRESS_HEIGHT - 1, 0xFF252C34);
        int progressWidth = this.menu.getProgressScaled(PROGRESS_WIDTH - 2);
        guiGraphics.fill(left + PROGRESS_X + 1, top + PROGRESS_Y + 1, left + PROGRESS_X + 1 + progressWidth, top + PROGRESS_Y + PROGRESS_HEIGHT - 1, 0xFF36D5FF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Level level = Minecraft.getInstance().level;
        TechMinerBlockEntity blockEntity = this.menu.getBlockEntity(level);
        this.hoverTooltipLines = List.of();

        guiGraphics.drawString(this.font, this.title, 12, 7, 0xEAFBFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.info"), INFO_X, INFO_PANEL_Y + 8, 0x6CE7FF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.upgrades"), UPGRADE_PANEL_X + 8, UPGRADE_PANEL_Y + 8, 0xEAFBFF, false);
        guiGraphics.drawString(this.font, Component.literal(MicroTechGuiHelper.trimToWidth(this.font, Component.translatable("gui.microtech.energy_label"), ENERGY_PANEL_WIDTH - 6)), ENERGY_PANEL_X + 4, ENERGY_PANEL_Y + 8, 0xEAFBFF, false);

        MachineStatus status = this.menu.getStatus();
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.status", status.getText()), INFO_X, INFO_Y, status.getColor(), false);

        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.progress", this.menu.getProgressPercent() + "%"), INFO_X, INFO_Y + 20, 0xD8E0E6, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.targets", this.menu.getTargetCount()), INFO_X, INFO_Y + 44, 0xD8E0E6, false);

        Component nextTarget = blockEntity == null ? Component.translatable("gui.microtech.tech_miner.no_targets") : blockEntity.getNextTargetDisplayName();
        Component nextLine = Component.translatable("gui.microtech.tech_miner.next_target", nextTarget);
        guiGraphics.drawString(this.font, Component.literal(MicroTechGuiHelper.trimToWidth(this.font, nextLine, INFO_WIDTH)), INFO_X, INFO_Y + 56, 0xD8E0E6, false);

        int capacity = this.menu.getFilterCapacity();
        Component filterText = capacity <= 0
                ? Component.translatable("gui.microtech.tech_miner.filter_disabled")
                : Component.translatable("gui.microtech.tech_miner.filter_active", this.menu.getActiveFilterEntryCount(), capacity);
        guiGraphics.drawString(this.font, Component.literal(MicroTechGuiHelper.trimToWidth(this.font, filterText, INFO_WIDTH)), INFO_X, INFO_Y + 68, 0x9FEFFF, false);

        guiGraphics.drawString(this.font, this.playerInventoryTitle, TechMinerMenu.PLAYER_INV_X, 142, 0xEAFBFF, false);

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

        if (this.configButton != null) {
            this.configButton.active = this.menu.getFilterCapacity() > 0;
        }
        if (this.scanButton != null) {
            this.scanButton.active = this.menu.canStartScan();
        }
        if (this.startStopButton != null) {
            this.startStopButton.setMessage(Component.translatable(running ? "gui.microtech.tech_miner.stop" : "gui.microtech.tech_miner.start"));
            this.startStopButton.active = running || this.menu.canStartMining();
        }
    }

    private void drawPixelFrame(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF5D6874);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF080B0F);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF5D6874);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF080B0F);
    }

    private void drawSlotBackgrounds(GuiGraphics guiGraphics) {
        for (SlotView slot : SlotView.upgradeSlots()) {
            this.drawSlot(guiGraphics, this.leftPos + slot.x(), this.topPos + slot.y());
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.drawSlot(guiGraphics, this.leftPos + TechMinerMenu.PLAYER_INV_X + column * 18, this.topPos + TechMinerMenu.PLAYER_INV_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            this.drawSlot(guiGraphics, this.leftPos + TechMinerMenu.PLAYER_INV_X + column * 18, this.topPos + TechMinerMenu.HOTBAR_Y);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF59636E);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF11161C);
    }

    private boolean isMouseOverEnergyBar(int mouseX, int mouseY) {
        int x = this.leftPos + ENERGY_BAR_X;
        int y = this.topPos + ENERGY_BAR_Y;
        return mouseX >= x && mouseX < x + ENERGY_BAR_WIDTH && mouseY >= y && mouseY < y + ENERGY_BAR_HEIGHT;
    }

    private record SlotView(int x, int y) {
        private static List<SlotView> upgradeSlots() {
            return List.of(
                    new SlotView(TechMinerMenu.UPGRADE_X, TechMinerMenu.UPGRADE_Y),
                    new SlotView(TechMinerMenu.UPGRADE_X, TechMinerMenu.UPGRADE_Y + 20),
                    new SlotView(TechMinerMenu.UPGRADE_X, TechMinerMenu.UPGRADE_Y + 40),
                    new SlotView(TechMinerMenu.UPGRADE_X, TechMinerMenu.UPGRADE_Y + 60)
            );
        }
    }
}
