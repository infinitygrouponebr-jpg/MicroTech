package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.block.entity.BatteryBlockEntity;
import Infinitygroup.microtech.menu.BatteryMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BatteryScreen extends AbstractContainerScreen<BatteryMenu> {
    private static final int ENERGY_BAR_X = 172;
    private static final int ENERGY_BAR_Y = 20;
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 56;

    private List<Component> hoverTooltipLines = List.of();

    public BatteryScreen(BatteryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 196;
        this.imageHeight = 174;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF1A1D22);
        guiGraphics.fill(left + 6, top + 6, left + 166, top + 70, 0xFF272C33);
        guiGraphics.fill(left + 166, top + 6, left + 190, top + 70, 0xFF1C2026);
        guiGraphics.fill(left + 6, top + 80, left + 190, top + 168, 0xFF20252B);

        MicroTechGuiHelper.drawVerticalEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, this.menu.getEnergyStored(), this.menu.getMaxEnergy());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoverTooltipLines = List.of();

        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
        MicroTechGuiHelper.drawStatus(guiGraphics, this.font, 8, 20, MicroTechGuiHelper.getBatteryStatus(this.menu));
        guiGraphics.drawString(this.font,
                Component.translatable("gui.microtech.battery_t1.input", MicroTechGuiHelper.formatRateFE(BatteryBlockEntity.MAX_RECEIVE)),
                8, 34, 0xD0D0D0, false);
        guiGraphics.drawString(this.font,
                Component.translatable("gui.microtech.battery_t1.output", MicroTechGuiHelper.formatRateFE(BatteryBlockEntity.MAX_EXTRACT)),
                8, 46, 0xD0D0D0, false);

        if (MicroTechGuiHelper.isHovering(mouseX, mouseY, this.leftPos + ENERGY_BAR_X, this.topPos + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT)) {
            this.hoverTooltipLines = MicroTechGuiHelper.buildEnergyTooltip("gui.microtech.energy_hover", this.menu.getEnergyStored(), this.menu.getMaxEnergy());
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
