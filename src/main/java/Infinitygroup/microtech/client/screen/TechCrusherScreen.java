package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.machine.MachineStatus;
import Infinitygroup.microtech.menu.TechCrusherMenu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

public class TechCrusherScreen extends AbstractContainerScreen<TechCrusherMenu> {
    private static final int ENERGY_BAR_X = 172;
    private static final int ENERGY_BAR_Y = 20;
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 56;

    private final Component playerInventoryTitle;
    private List<Component> hoverTooltipLines = List.of();

    public TechCrusherScreen(TechCrusherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.playerInventoryTitle = playerInventory.getDisplayName();
        this.imageWidth = 196;
        this.imageHeight = 194;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        Level level = Minecraft.getInstance().level;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF2B2B2B);
        guiGraphics.fill(left, top, left + this.imageWidth, top + 18, 0xFF3A3A3A);
        guiGraphics.fill(left + 6, top + 20, left + 166, top + 74, 0xFF23272D);
        guiGraphics.fill(left + 166, top + 20, left + 190, top + 74, 0xFF1B1F24);

        for (var slot : this.menu.slots) {
            int x = left + slot.x;
            int y = top + slot.y;
            guiGraphics.fill(x, y, x + 18, y + 18, 0xFF555555);
            guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1F1F1F);
        }

        MicroTechGuiHelper.drawProgress(guiGraphics, left + 80, top + 72, 24, 0xFFB5B5B5, this.menu.getProgress(), this.menu.getMaxProgress());
        guiGraphics.drawString(this.font, "->", left + 84, top + 66, 0xFFFFFF, false);
        MicroTechGuiHelper.drawVerticalEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, this.menu.getEnergyStored(), this.menu.getMaxEnergy());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoverTooltipLines = List.of();
        Level level = Minecraft.getInstance().level;
        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
        MicroTechGuiHelper.drawStatus(guiGraphics, this.font, 8, 20, MicroTechGuiHelper.getTechCrusherStatus(this.menu, level));
        MicroTechGuiHelper.drawPercentLine(guiGraphics, this.font, 8, 34, "gui.microtech.progress", this.menu.getProgress(), this.menu.getMaxProgress());
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.upgrades"), 148, 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_crusher.input"), 50, 54, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_crusher.output"), 118, 54, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 100, 0xFFFFFF, false);

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
