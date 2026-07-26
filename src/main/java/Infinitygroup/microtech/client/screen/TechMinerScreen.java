package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.client.TechArmorClientConfig;
import Infinitygroup.microtech.item.MicroTechTooltipHelper;
import Infinitygroup.microtech.menu.TechMinerGuiLayout;
import Infinitygroup.microtech.menu.TechMinerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class TechMinerScreen extends AbstractContainerScreen<TechMinerMenu> {
    private static final ResourceLocation MACHINE_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Microtech.MODID, "textures/gui/tech_miner_machine_panel.png");
    private static final ResourceLocation PLAYER_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Microtech.MODID, "textures/gui/player_inventory_panel.png");
    private static final int BACKGROUND = 0xFF11161C;
    private static final int SURFACE = 0xFF20262D;
    private static final int PANEL = 0xFF151A20;
    private static final int PANEL_DARK = 0xFF10151B;
    private static final int TEXT = 0xFFEAFBFF;
    private static final int TEXT_MUTED = 0xFFD8E0E6;
    private static final int TEXT_ACCENT = 0xFF9FEFFF;
    private static final int TEXT_SUBTLE = 0xFF8FA4B5;

    private final TechMinerGuiLayout.Layout layout;
    private Button inventoryButton;
    private Button configButton;
    private Button scanButton;
    private Button startStopButton;
    private List<Component> hoverTooltipLines = List.of();

    public TechMinerScreen(TechMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.layout = menu.getLayout();
        this.imageWidth = this.layout.guiWidth();
        this.imageHeight = this.layout.guiHeight();
    }

    @Override
    protected void init() {
        super.init();
        if (TechMinerClientLayoutState.consumeSmallResolutionWarning() && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.translatable("message.microtech.tech_miner.separated_layout_fallback"), true);
        }

        this.inventoryButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.inventory_button"),
                button -> this.clickMachineButton(3)
        ).bounds(this.leftPos + this.layout.inventoryButton().x(), this.topPos + this.layout.inventoryButton().y(),
                this.layout.inventoryButton().width(), this.layout.inventoryButton().height()).build());
        this.configButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.config"),
                button -> this.clickMachineButton(4)
        ).bounds(this.leftPos + this.layout.configButton().x(), this.topPos + this.layout.configButton().y(),
                this.layout.configButton().width(), this.layout.configButton().height()).build());
        this.scanButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.scan"),
                button -> this.clickMachineButton(0)
        ).bounds(this.leftPos + this.layout.scanButton().x(), this.topPos + this.layout.scanButton().y(),
                this.layout.scanButton().width(), this.layout.scanButton().height()).build());
        this.startStopButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.start"),
                button -> this.clickMachineButton(this.menu.isProcessing() ? 2 : 1)
        ).bounds(this.leftPos + this.layout.startStopButton().x(), this.topPos + this.layout.startStopButton().y(),
                this.layout.startStopButton().width(), this.layout.startStopButton().height()).build());
        this.updateButtonState();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.layout.separated()) {
            super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        double dim = Mth.clamp(TechArmorClientConfig.SEPARATED_TECH_MINER_BACKGROUND_DIM.get(), 0.0D, 1.0D);
        if (dim > 0.0D) {
            int alpha = Mth.clamp((int) Math.round(dim * 255.0D), 0, 255);
            guiGraphics.fill(0, 0, this.width, this.height, alpha << 24);
        }
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        if (this.layout.separated()) {
            this.blitPanel(guiGraphics, MACHINE_PANEL_TEXTURE, this.layout.machinePanel(), left, top);
            this.blitPanel(guiGraphics, PLAYER_PANEL_TEXTURE, this.layout.playerPanel(), left, top);
        } else {
            guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, BACKGROUND);
            guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + this.imageHeight - 4, SURFACE);
            this.drawPanel(guiGraphics, left, top, this.layout.infoPanel(), PANEL);
            this.drawPanel(guiGraphics, left, top, this.layout.upgradesPanel(), PANEL);
            this.drawPanel(guiGraphics, left, top, this.layout.energyPanel(), PANEL_DARK);
        }

        this.drawSlotBackgrounds(guiGraphics);
        this.drawProgressBar(guiGraphics, left, top);
        this.drawEnergyBar(guiGraphics, left, top);
        if (TechArmorClientConfig.DEBUG_TECH_MINER_GUI_BOUNDS.get()) {
            this.drawDebugLayout(guiGraphics, left, top);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoverTooltipLines = List.of();

        this.drawCentered(guiGraphics, this.title, this.layout.title(), TEXT);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.info_panel"),
                this.layout.infoTitle().x(), this.layout.infoTitle().y(), TEXT_ACCENT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.upgrades_panel"),
                this.layout.upgradesTitle().x(), this.layout.upgradesTitle().y(), TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.energy_short"),
                this.layout.energyTitle().x(), this.layout.energyTitle().y(), TEXT, false);

        int infoWidth = this.layout.infoPanel().width() - 12;
        this.drawFitted(guiGraphics, Component.translatable("gui.microtech.status", this.menu.getStatusText()),
                this.layout.statusText(), infoWidth, TEXT_MUTED);
        this.drawFitted(guiGraphics, Component.translatable("gui.microtech.tech_miner.targets", this.menu.getTargetCount()),
                this.layout.targetsText(), infoWidth, TEXT_MUTED);
        this.drawFitted(guiGraphics, Component.translatable("gui.microtech.tech_miner.filter_value", this.menu.getFilterStatusText()),
                this.layout.filterText(), infoWidth, TEXT_ACCENT);

        Component nextTarget = Component.translatable("gui.microtech.tech_miner.next_target", this.menu.getNextTargetText());
        this.drawFitted(guiGraphics, nextTarget, this.layout.nextTargetText(), infoWidth, TEXT_SUBTLE);
        this.drawFitted(guiGraphics, Component.translatable("gui.microtech.tech_miner.progress_value", this.menu.getProgressPercent()),
                this.layout.progressText(), infoWidth, TEXT_MUTED);

        this.drawCentered(guiGraphics, Component.translatable("gui.microtech.tech_miner.energy_percent", this.getEnergyPercent()),
                new TechMinerGuiLayout.Rect(this.layout.energyPanel().x(), this.layout.energyPercent().y(),
                        this.layout.energyPanel().width(), 9), TEXT_ACCENT);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                this.layout.playerInventoryTitle().x(), this.layout.playerInventoryTitle().y(), TEXT, false);

        if (this.isMouseOverEnergyBar(mouseX, mouseY)) {
            this.hoverTooltipLines = List.of(Component.translatable("gui.microtech.energy",
                    MicroTechTooltipHelper.formatFE(this.menu.getEnergyStored()),
                    MicroTechTooltipHelper.formatFE(this.menu.getMaxEnergy())));
        } else if (this.isMouseOverNextTarget(mouseX, mouseY)) {
            this.hoverTooltipLines = this.menu.getNextTargetTooltip();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.updateButtonState();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.hoverTooltipLines.isEmpty()) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        } else {
            guiGraphics.renderTooltip(this.font, this.hoverTooltipLines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void clickMachineButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
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

    private void blitPanel(GuiGraphics guiGraphics, ResourceLocation texture, TechMinerGuiLayout.Rect rect, int left, int top) {
        guiGraphics.blit(texture, left + rect.x(), top + rect.y(), 0.0F, 0.0F, rect.width(), rect.height(), rect.width(), rect.height());
    }

    private void drawPanel(GuiGraphics guiGraphics, int left, int top, TechMinerGuiLayout.Rect rect, int color) {
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.right(), top + rect.bottom(), color);
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.right(), top + rect.y() + 1, 0xFF5D6874);
        guiGraphics.fill(left + rect.x(), top + rect.bottom() - 1, left + rect.right(), top + rect.bottom(), 0xFF080B0F);
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.x() + 1, top + rect.bottom(), 0xFF5D6874);
        guiGraphics.fill(left + rect.right() - 1, top + rect.y(), left + rect.right(), top + rect.bottom(), 0xFF080B0F);
    }

    private void drawProgressBar(GuiGraphics guiGraphics, int left, int top) {
        TechMinerGuiLayout.Rect bar = this.layout.progressBar();
        int inset = TechMinerGuiLayout.BAR_INSET;
        int innerWidth = Math.max(0, bar.width() - inset * 2);
        int innerHeight = Math.max(0, bar.height() - inset * 2);
        int filledWidth = innerWidth * Math.max(0, Math.min(100, this.menu.getProgressPercent())) / 100;

        guiGraphics.fill(left + bar.x(), top + bar.y(), left + bar.right(), top + bar.bottom(), 0xFF0C1015);
        guiGraphics.fill(left + bar.x() + inset, top + bar.y() + inset, left + bar.x() + inset + innerWidth, top + bar.y() + inset + innerHeight, 0xFF252C34);
        guiGraphics.fill(left + bar.x() + inset, top + bar.y() + inset, left + bar.x() + inset + filledWidth, top + bar.y() + inset + innerHeight, 0xFF36D5FF);
    }

    private void drawEnergyBar(GuiGraphics guiGraphics, int left, int top) {
        TechMinerGuiLayout.Rect bar = this.layout.energyBar();
        int inset = TechMinerGuiLayout.BAR_INSET;
        int innerHeight = Math.max(0, bar.height() - inset * 2);
        int innerWidth = Math.max(0, bar.width() - inset * 2);
        int max = this.menu.getMaxEnergy();
        int current = this.menu.getEnergyStored();
        int filledHeight = max <= 0 || current <= 0 ? 0 : innerHeight * Math.min(current, max) / max;
        int innerX = left + bar.x() + inset;
        int innerY = top + bar.y() + inset;
        int innerBottom = innerY + innerHeight;

        guiGraphics.fill(left + bar.x(), top + bar.y(), left + bar.right(), top + bar.bottom(), 0xFF0F1216);
        guiGraphics.fill(innerX, innerY, innerX + innerWidth, innerBottom, 0xFF1A1F25);
        guiGraphics.fill(innerX, innerBottom - filledHeight, innerX + innerWidth, innerBottom, 0xFF6CE7FF);
    }

    private void drawSlotBackgrounds(GuiGraphics guiGraphics) {
        for (TechMinerGuiLayout.Pos slot : this.layout.upgradeSlots()) {
            this.drawSlot(guiGraphics, this.leftPos + slot.x(), this.topPos + slot.y());
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.drawSlot(guiGraphics,
                        this.leftPos + this.layout.playerInventory().x() + column * TechMinerGuiLayout.SLOT_SIZE,
                        this.topPos + this.layout.playerInventory().y() + row * TechMinerGuiLayout.SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            this.drawSlot(guiGraphics,
                    this.leftPos + this.layout.playerHotbar().x() + column * TechMinerGuiLayout.SLOT_SIZE,
                    this.topPos + this.layout.playerHotbar().y());
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + TechMinerGuiLayout.SLOT_SIZE, y + TechMinerGuiLayout.SLOT_SIZE, 0xFF59636E);
        guiGraphics.fill(x + 1, y + 1, x + TechMinerGuiLayout.SLOT_SIZE - 1, y + TechMinerGuiLayout.SLOT_SIZE - 1, 0xFF11161C);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, TechMinerGuiLayout.Rect area, int color) {
        int x = area.x() + (area.width() - this.font.width(text)) / 2;
        guiGraphics.drawString(this.font, text, x, area.y(), color, false);
    }

    private void drawFitted(GuiGraphics guiGraphics, Component text, TechMinerGuiLayout.Pos pos, int maxWidth, int color) {
        String value = text.getString();
        if (this.font.width(value) > maxWidth) {
            int ellipsisWidth = this.font.width("...");
            value = this.font.plainSubstrByWidth(value, Math.max(0, maxWidth - ellipsisWidth)) + "...";
        }
        guiGraphics.drawString(this.font, value, pos.x(), pos.y(), color, false);
    }

    private int getEnergyPercent() {
        int max = this.menu.getMaxEnergy();
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, this.menu.getEnergyStored() * 100 / max));
    }

    private boolean isMouseOverEnergyBar(int mouseX, int mouseY) {
        TechMinerGuiLayout.Rect bar = this.layout.energyBar();
        int x = this.leftPos + bar.x();
        int y = this.topPos + bar.y();
        return mouseX >= x && mouseX < x + bar.width() && mouseY >= y && mouseY < y + bar.height();
    }

    private boolean isMouseOverNextTarget(int mouseX, int mouseY) {
        int x = this.leftPos + this.layout.nextTargetText().x();
        int y = this.topPos + this.layout.nextTargetText().y();
        int width = this.layout.infoPanel().width() - 12;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + this.font.lineHeight;
    }

    private void drawDebugLayout(GuiGraphics guiGraphics, int left, int top) {
        this.drawOutline(guiGraphics, left, top, new TechMinerGuiLayout.Rect(0, 0, this.layout.guiWidth(), this.layout.guiHeight()), 0xFFFF0000);
        this.drawOutline(guiGraphics, left, top, this.layout.machinePanel(), 0xFFFFFF00);
        this.drawOutline(guiGraphics, left, top, this.layout.playerPanel(), 0xFFFFAA00);
        this.drawOutline(guiGraphics, left, top, this.layout.infoPanel(), 0xFF00FFFF);
        this.drawOutline(guiGraphics, left, top, this.layout.upgradesPanel(), 0xFFFFFF00);
        this.drawOutline(guiGraphics, left, top, this.layout.energyPanel(), 0xFF00AAFF);
        this.drawOutline(guiGraphics, left, top, this.layout.inventoryButton(), 0xFF00FF00);
        this.drawOutline(guiGraphics, left, top, this.layout.configButton(), 0xFF00FF00);
        this.drawOutline(guiGraphics, left, top, this.layout.scanButton(), 0xFF00FF00);
        this.drawOutline(guiGraphics, left, top, this.layout.startStopButton(), 0xFF00FF00);
        this.drawOutline(guiGraphics, left, top, this.layout.progressBar(), 0xFFFF00FF);
        this.drawOutline(guiGraphics, left, top, this.layout.energyBar(), 0xFFFF00FF);
        for (TechMinerGuiLayout.Pos slot : this.layout.upgradeSlots()) {
            this.drawOutline(guiGraphics, left, top, new TechMinerGuiLayout.Rect(slot.x(), slot.y(), TechMinerGuiLayout.SLOT_SIZE, TechMinerGuiLayout.SLOT_SIZE), 0xFFFF55FF);
        }
        this.drawSlotGridDebug(guiGraphics, left, top, this.layout.playerInventory(), 3, 0xFF55FF55);
        this.drawSlotGridDebug(guiGraphics, left, top, this.layout.playerHotbar(), 1, 0xFF5599FF);
    }

    private void drawSlotGridDebug(GuiGraphics guiGraphics, int left, int top, TechMinerGuiLayout.Pos origin, int rows, int color) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                this.drawOutline(guiGraphics, left, top,
                        new TechMinerGuiLayout.Rect(origin.x() + column * TechMinerGuiLayout.SLOT_SIZE,
                                origin.y() + row * TechMinerGuiLayout.SLOT_SIZE,
                                TechMinerGuiLayout.SLOT_SIZE,
                                TechMinerGuiLayout.SLOT_SIZE), color);
            }
        }
    }

    private void drawOutline(GuiGraphics guiGraphics, int left, int top, TechMinerGuiLayout.Rect rect, int color) {
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.right(), top + rect.y() + 1, color);
        guiGraphics.fill(left + rect.x(), top + rect.bottom() - 1, left + rect.right(), top + rect.bottom(), color);
        guiGraphics.fill(left + rect.x(), top + rect.y(), left + rect.x() + 1, top + rect.bottom(), color);
        guiGraphics.fill(left + rect.right() - 1, top + rect.y(), left + rect.right(), top + rect.bottom(), color);
    }
}
