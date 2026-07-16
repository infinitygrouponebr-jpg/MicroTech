package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.menu.BasicMachineMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BasicMachineScreen extends AbstractContainerScreen<BasicMachineMenu> {
    private static final int ENERGY_BAR_X = 172;
    private static final int ENERGY_BAR_Y = 20;
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 56;

    private final Component playerInventoryTitle;
    private List<Component> hoverTooltipLines = List.of();

    public BasicMachineScreen(BasicMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.playerInventoryTitle = playerInventory.getDisplayName();
        this.imageWidth = 196;
        this.imageHeight = 184;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF2B2B2B);
        guiGraphics.fill(left, top, left + this.imageWidth, top + 18, 0xFF3A3A3A);
        guiGraphics.fill(left + 6, top + 20, left + 166, top + 74, 0xFF23272D);
        guiGraphics.fill(left + 166, top + 20, left + 190, top + 74, 0xFF1B1F24);

        MicroTechGuiHelper.drawVerticalEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, this.menu.getEnergyStored(), this.menu.getMaxEnergy());

        for (var slot : this.menu.slots) {
            int x = left + slot.x;
            int y = top + slot.y;
            guiGraphics.fill(x, y, x + 18, y + 18, 0xFF555555);
            guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1F1F1F);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoverTooltipLines = List.of();

        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
        MicroTechGuiHelper.drawStatus(guiGraphics, this.font, 8, 20, MicroTechGuiHelper.getEnergyConverterStatus(this.menu));
        guiGraphics.drawString(this.font,
                Component.translatable("gui.microtech.energy_converter_t1.pending", MicroTechGuiHelper.formatCompactFE(this.menu.getPendingEnergy())),
                8, 34, 0xD0D0D0, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.upgrades"), 148, 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.energy_converter_t1.fuel"), 56, 54, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.energy_converter_t1.residue"), 116, 54, 0xFFFFFF, false);
        this.drawCompactEnergyState(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 86, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!this.hoverTooltipLines.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.hoverTooltipLines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void drawCompactEnergyState(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (MicroTechGuiHelper.isHovering(mouseX, mouseY, this.leftPos + ENERGY_BAR_X, this.topPos + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT)) {
            this.hoverTooltipLines = MicroTechGuiHelper.buildEnergyTooltip("gui.microtech.energy_hover", this.menu.getEnergyStored(), this.menu.getMaxEnergy());
        }
    }
}
