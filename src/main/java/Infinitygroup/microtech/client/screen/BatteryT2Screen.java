package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.menu.BatteryT2Menu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BatteryT2Screen extends AbstractContainerScreen<BatteryT2Menu> {
    private static final int ENERGY_BAR_X = 172;
    private static final int ENERGY_BAR_Y = 20;
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 56;

    private List<Component> hoverTooltipLines = List.of();

    public BatteryT2Screen(BatteryT2Menu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 196;
        this.imageHeight = 196;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF1A1D22);
        guiGraphics.fill(left + 6, top + 6, left + 166, top + 74, 0xFF272C33);
        guiGraphics.fill(left + 166, top + 6, left + 190, top + 74, 0xFF1C2026);
        guiGraphics.fill(left + 6, top + 84, left + 190, top + 190, 0xFF20252B);
        guiGraphics.fill(left + 72, top + 73, left + 104, top + 101, 0xFF15191E);
        guiGraphics.fill(left + 73, top + 74, left + 103, top + 100, 0xFF2C3138);

        MicroTechGuiHelper.drawVerticalEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, this.menu.getEnergyStored(), this.menu.getMaxEnergy());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoverTooltipLines = List.of();

        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
        MicroTechGuiHelper.drawStatus(guiGraphics, this.font, 8, 20, MicroTechGuiHelper.getBatteryT2Status(this.menu));
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.upgrades"), 148, 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.microtech.item_energy",
                        MicroTechGuiHelper.formatCompactFE(this.menu.getChargingItemEnergyStored()),
                        MicroTechGuiHelper.formatCompactFE(this.menu.getChargingItemMaxEnergy())),
                8, 48, 0xD0D0D0, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.battery_t2.item"), 8, 66, 0xFFFFFF, false);

        if (MicroTechGuiHelper.isHovering(mouseX, mouseY, this.leftPos + ENERGY_BAR_X, this.topPos + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT)) {
            this.hoverTooltipLines = MicroTechGuiHelper.buildEnergyTooltip("gui.microtech.energy_hover", this.menu.getEnergyStored(), this.menu.getMaxEnergy());
        } else if (MicroTechGuiHelper.isHovering(mouseX, mouseY, this.leftPos + 8, this.topPos + 48, 110, this.font.lineHeight)) {
            this.hoverTooltipLines = MicroTechGuiHelper.buildEnergyTooltip("gui.microtech.item_energy_hover", this.menu.getChargingItemEnergyStored(), this.menu.getChargingItemMaxEnergy());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!this.hoverTooltipLines.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.hoverTooltipLines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }
}
