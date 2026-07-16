package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.menu.TechMinerOutputMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TechMinerOutputScreen extends AbstractContainerScreen<TechMinerOutputMenu> {
    private Button backButton;

    public TechMinerOutputScreen(TechMinerOutputMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 216;
        this.imageHeight = 194;
    }

    @Override
    protected void init() {
        super.init();
        this.backButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.back"),
                button -> this.clickMachineButton(0)
        ).bounds(this.leftPos + 141, this.topPos + 6, 62, 18).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF11161C);
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + this.imageHeight - 4, 0xFF20262D);
        guiGraphics.fill(left + 20, top + 24, left + 196, top + 86, 0xFF151A20);
        guiGraphics.fill(left + 20, top + 96, left + 196, top + 190, 0xFF181E25);
        this.drawSlotBackgrounds(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 10, 10, 0xEAFBFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 28, 90, 0xEAFBFF, false);
    }

    private void clickMachineButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void drawSlotBackgrounds(GuiGraphics guiGraphics) {
        for (int slot = 0; slot < TechMinerOutputMenu.OUTPUT_SLOT_COUNT; slot++) {
            int column = slot % 9;
            int row = slot / 9;
            this.drawSlot(guiGraphics, this.leftPos + TechMinerOutputMenu.OUTPUT_X + column * 18, this.topPos + TechMinerOutputMenu.OUTPUT_Y + row * 18);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.drawSlot(guiGraphics, this.leftPos + TechMinerOutputMenu.PLAYER_INV_X + column * 18, this.topPos + TechMinerOutputMenu.PLAYER_INV_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            this.drawSlot(guiGraphics, this.leftPos + TechMinerOutputMenu.PLAYER_INV_X + column * 18, this.topPos + TechMinerOutputMenu.HOTBAR_Y);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF59636E);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF11161C);
    }
}
