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
    private static final int ENERGY_BAR_X = 228;
    private static final int ENERGY_BAR_Y = 22;
    private static final int ENERGY_BAR_WIDTH = 10;
    private static final int ENERGY_BAR_HEIGHT = 116;
    private static final int INFO_X = 14;
    private static final int INFO_Y = 24;
    private static final int INFO_WIDTH = 132;
    private static final int PROGRESS_X = 18;
    private static final int PROGRESS_Y = 58;
    private static final int PROGRESS_WIDTH = 118;
    private static final int PROGRESS_HEIGHT = 7;

    private Button inventoryButton;
    private Button configButton;
    private Button scanButton;
    private Button startStopButton;
    private List<Component> hoverTooltipLines = List.of();

    public TechMinerScreen(TechMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 246;
        this.imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.inventory_button"),
                button -> this.clickMachineButton(3)
        ).bounds(this.leftPos + 16, this.topPos + 112, 62, 18).build());
        this.configButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.config"),
                button -> this.clickMachineButton(4)
        ).bounds(this.leftPos + 84, this.topPos + 112, 62, 18).build());
        this.scanButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.scan"),
                button -> this.clickMachineButton(0)
        ).bounds(this.leftPos + 154, this.topPos + 112, 62, 18).build());
        this.startStopButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.start"),
                button -> this.clickMachineButton(this.isRunning() ? 2 : 1)
        ).bounds(this.leftPos + 154, this.topPos + 134, 62, 18).build());
        this.updateButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF11161C);
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + this.imageHeight - 4, 0xFF20262D);
        guiGraphics.fill(left + 10, top + 18, left + 150, top + 108, 0xFF151A20);
        guiGraphics.fill(left + 158, top + 18, left + 222, top + 104, 0xFF171E25);
        guiGraphics.fill(left + 224, top + 18, left + 242, top + 146, 0xFF10151B);
        guiGraphics.fill(left + 22, top + 138, left + 208, top + 220, 0xFF181E25);

        this.drawPixelFrame(guiGraphics, left + 10, top + 18, 140, 90);
        this.drawPixelFrame(guiGraphics, left + 158, top + 18, 64, 86);
        this.drawSlotBackgrounds(guiGraphics);

        Level level = Minecraft.getInstance().level;
        MicroTechGuiHelper.drawVerticalEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, this.menu.getEnergyStored(level), this.menu.getMaxEnergy(level));

        int duration = Math.max(1, this.menu.getProcessDuration(level));
        int completed = Math.max(0, duration - this.menu.getProcessTicks(level));
        guiGraphics.fill(left + PROGRESS_X, top + PROGRESS_Y, left + PROGRESS_X + PROGRESS_WIDTH, top + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF0C1015);
        guiGraphics.fill(left + PROGRESS_X + 1, top + PROGRESS_Y + 1, left + PROGRESS_X + PROGRESS_WIDTH - 1, top + PROGRESS_Y + PROGRESS_HEIGHT - 1, 0xFF252C34);
        int progressWidth = duration <= 0 ? 0 : Math.max(0, Math.min(PROGRESS_WIDTH - 2, completed * (PROGRESS_WIDTH - 2) / duration));
        guiGraphics.fill(left + PROGRESS_X + 1, top + PROGRESS_Y + 1, left + PROGRESS_X + 1 + progressWidth, top + PROGRESS_Y + PROGRESS_HEIGHT - 1, 0xFF36D5FF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Level level = Minecraft.getInstance().level;
        TechMinerBlockEntity blockEntity = this.menu.getBlockEntity(level);
        this.hoverTooltipLines = List.of();

        guiGraphics.drawString(this.font, this.title, 12, 7, 0xEAFBFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.info"), INFO_X, INFO_Y - 10, 0x6CE7FF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.upgrades"), 168, 8, 0xEAFBFF, false);

        MachineStatus status = this.menu.getStatus(level);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.status", status.getText()), INFO_X, INFO_Y, status.getColor(), false);

        int duration = Math.max(1, this.menu.getProcessDuration(level));
        int completed = Math.max(0, duration - this.menu.getProcessTicks(level));
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.progress", MicroTechTooltipHelper.formatPercent(completed, duration)), INFO_X, INFO_Y + 20, 0xD8E0E6, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.targets", this.menu.getTargetCount(level)), INFO_X, INFO_Y + 44, 0xD8E0E6, false);

        Component nextTarget = blockEntity == null ? Component.translatable("gui.microtech.tech_miner.no_targets") : blockEntity.getNextTargetDisplayName();
        Component nextLine = Component.translatable("gui.microtech.tech_miner.next_target", nextTarget);
        guiGraphics.drawString(this.font, Component.literal(MicroTechGuiHelper.trimToWidth(this.font, nextLine, INFO_WIDTH)), INFO_X, INFO_Y + 56, 0xD8E0E6, false);

        int capacity = this.menu.getFilterCapacity(level);
        Component filterText = capacity <= 0
                ? Component.translatable("gui.microtech.tech_miner.filter_disabled")
                : Component.translatable("gui.microtech.tech_miner.filter_active", this.menu.getActiveFilterEntryCount(level), capacity);
        guiGraphics.drawString(this.font, Component.literal(MicroTechGuiHelper.trimToWidth(this.font, filterText, INFO_WIDTH)), INFO_X, INFO_Y + 68, 0x9FEFFF, false);

        guiGraphics.drawString(this.font, this.playerInventoryTitle, 28, 132, 0xEAFBFF, false);

        if (this.isMouseOverEnergyBar(mouseX, mouseY)) {
            int energy = this.menu.getEnergyStored(level);
            int max = this.menu.getMaxEnergy(level);
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
        Level level = Minecraft.getInstance().level;
        TechMinerBlockEntity blockEntity = this.menu.getBlockEntity(level);
        return blockEntity != null && blockEntity.isProcessing();
    }

    private void updateButtonState() {
        Level level = Minecraft.getInstance().level;
        TechMinerBlockEntity blockEntity = this.menu.getBlockEntity(level);
        boolean running = blockEntity != null && blockEntity.isProcessing();

        if (this.configButton != null) {
            this.configButton.active = this.menu.getFilterCapacity(level) > 0;
        }
        if (this.scanButton != null) {
            this.scanButton.active = blockEntity != null && blockEntity.canStartScan();
        }
        if (this.startStopButton != null) {
            this.startStopButton.setMessage(Component.translatable(running ? "gui.microtech.tech_miner.stop" : "gui.microtech.tech_miner.start"));
            this.startStopButton.active = blockEntity != null && (running || blockEntity.canStartMining());
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
