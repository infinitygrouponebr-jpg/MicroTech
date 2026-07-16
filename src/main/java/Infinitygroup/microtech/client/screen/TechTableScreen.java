package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.block.entity.TechTableBlockEntity;
import Infinitygroup.microtech.menu.TechTableMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

public class TechTableScreen extends AbstractContainerScreen<TechTableMenu> {
    private static final int INPUT_X = 56;
    private static final int INPUT_Y = 38;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 38;
    private static final int BUTTON_X = 138;
    private static final int BUTTON_Y = 24;
    private static final int BUTTON_WIDTH = 48;
    private static final int BUTTON_HEIGHT = 16;

    private final Component playerInventoryTitle;
    private Button actionButton;
    private List<Component> hoverTooltipLines = List.of();

    public TechTableScreen(TechTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.playerInventoryTitle = playerInventory.getDisplayName();
        this.imageWidth = 196;
        this.imageHeight = 194;
    }

    @Override
    protected void init() {
        super.init();
        this.actionButton = this.addRenderableWidget(Button.builder(Component.literal(""), button -> this.clickMachineButton(0))
                .bounds(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.updateButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF1F2329);
        guiGraphics.fill(left, top, left + this.imageWidth, top + 18, 0xFF30343A);
        guiGraphics.fill(left + 6, top + 20, left + 190, top + 74, 0xFF24292F);

        this.drawSlotBackgrounds(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoverTooltipLines = List.of();

        Level level = this.minecraft != null ? this.minecraft.level : null;
        TechTableBlockEntity blockEntity = this.menu.getBlockEntity(level);

        guiGraphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.status_label"), 8, 20, 0xD0D0D0, false);

        Component status = blockEntity != null ? blockEntity.getStatus().getText() : Component.translatable("screen.microtech.tech_table.idle");
        int statusColor = blockEntity != null ? blockEntity.getStatus().getColor() : 0xD0D0D0;
        guiGraphics.drawString(this.font, status, 56, 20, statusColor, false);

        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.recipe"), 8, 34, 0xD0D0D0, false);
        guiGraphics.drawString(this.font, blockEntity != null ? blockEntity.getRecipeDisplayName() : Component.translatable("screen.microtech.tech_table.no_recipe"), 56, 34, 0xFFFFFF, false);

        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.hits"), 8, 48, 0xD0D0D0, false);
        if (blockEntity != null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.hits_value", blockEntity.getSessionHits(), Math.max(1, blockEntity.getRequiredHits())), 56, 48, 0xFFFFFF, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.hits_value", 0, 1), 56, 48, 0xFFFFFF, false);
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.feedback"), 8, 62, 0xD0D0D0, false);
        if (blockEntity != null) {
            guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.feedback_value", blockEntity.getSessionMistakes()), 56, 62, 0xFFFFFF, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.feedback_value", 0), 56, 62, 0xFFFFFF, false);
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.input"), INPUT_X, 24, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.microtech.tech_table.output"), OUTPUT_X, 24, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 100, 0xFFFFFF, false);
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
        this.updateButtonState();
    }

    private void updateButtonState() {
        Level level = this.minecraft != null ? this.minecraft.level : null;
        TechTableBlockEntity blockEntity = this.menu.getBlockEntity(level);
        boolean owner = blockEntity != null && this.minecraft != null && this.minecraft.player != null && blockEntity.isSessionOwner(this.minecraft.player);
        boolean working = blockEntity != null && blockEntity.isWorking() && owner;
        boolean ready = blockEntity != null && blockEntity.getStatus() == TechTableBlockEntity.TechTableState.READY;

        if (this.actionButton != null) {
            this.actionButton.setMessage(Component.translatable(working ? "screen.microtech.tech_table.cancel" : "screen.microtech.tech_table.start"));
            this.actionButton.active = owner && (working || ready);
        }
    }

    private void drawSlotBackgrounds(GuiGraphics guiGraphics) {
        int left = this.leftPos;
        int top = this.topPos;

        for (var slot : this.menu.slots) {
            int x = left + slot.x;
            int y = top + slot.y;
            guiGraphics.fill(x, y, x + 18, y + 18, 0xFF555555);
            guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1F1F1F);
        }
    }
}
