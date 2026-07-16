package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.menu.EvoTableMenu;
import Infinitygroup.microtech.block.entity.EvoTableBlockEntity.EvoStatus;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EvoTableScreen extends AbstractContainerScreen<EvoTableMenu> {
    private Button startButton;

    public EvoTableScreen(EvoTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.startButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.microtech.evo_table.start"),
                button -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }
        ).bounds(this.leftPos + 72, this.topPos + 56, 44, 16).build());
        this.updateButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF1B1E23);
        guiGraphics.fill(left + 7, top + 7, left + 169, top + 161, 0xFF23272D);

        this.drawSlotPanel(guiGraphics, left + 53, top + 52);
        this.drawSlotPanel(guiGraphics, left + 116, top + 52);

        int progressWidth = 0;
        int duration = Math.max(this.menu.getEvolutionDuration(), 1);
        if (this.menu.isEvolving()) {
            progressWidth = (int) (24.0D * Math.min(this.menu.getEvolutionTicks(), duration) / (double) duration);
        }

        guiGraphics.fill(left + 74, top + 38, left + 102, top + 42, 0xFF0F1317);
        guiGraphics.fill(left + 75, top + 39, left + 75 + progressWidth, top + 41, 0xFF6CE7FF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
        MicroTechGuiHelper.drawStatus(guiGraphics, this.font, 8, 20, MicroTechGuiHelper.getEvoStatus(this.menu));
        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.evo_table.item"), 49, 38, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.evo_table.material"), 102, 38, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.evo_table.inventory"), 8, 74, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.updateButtonState();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawSlotPanel(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF111418);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF252B31);
    }

    private void updateButtonState() {
        if (this.startButton != null) {
            boolean ready = this.menu.getStatus() == EvoStatus.READY || this.menu.getStatus() == EvoStatus.FLIGHT_READY;
            this.startButton.visible = ready;
            this.startButton.active = ready;
        }
    }
}
