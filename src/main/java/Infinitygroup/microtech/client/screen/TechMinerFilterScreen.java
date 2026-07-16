package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.block.entity.TechMinerBlockEntity;
import Infinitygroup.microtech.item.TechMinerTargetHelper;
import Infinitygroup.microtech.menu.TechMinerFilterMenu;
import Infinitygroup.microtech.network.TechMinerFilterPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class TechMinerFilterScreen extends AbstractContainerScreen<TechMinerFilterMenu> {
    private static final int FILTER_X = 36;
    private static final int FILTER_Y = 42;
    private static final int FILTER_COLUMNS = 3;
    private static final int SLOT_SIZE = 18;

    private Button clearButton;
    private Button backButton;
    private long clearConfirmUntil;
    private List<Component> hoverTooltipLines = List.of();

    public TechMinerFilterScreen(TechMinerFilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 216;
        this.imageHeight = 204;
    }

    @Override
    protected void init() {
        super.init();
        this.clearButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.clear_filter"),
                button -> this.handleClearFilter()
        ).bounds(this.leftPos + 106, this.topPos + 42, 86, 18).build());
        this.backButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.microtech.tech_miner.back"),
                button -> this.clickMachineButton(0)
        ).bounds(this.leftPos + 130, this.topPos + 8, 62, 18).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF11161C);
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + this.imageHeight - 4, 0xFF20262D);
        guiGraphics.fill(left + 22, top + 36, left + 94, top + 108, 0xFF151A20);
        guiGraphics.fill(left + 102, top + 36, left + 198, top + 88, 0xFF171E25);
        guiGraphics.fill(left + 22, top + 106, left + 198, top + 200, 0xFF181E25);

        this.drawFilterSlots(guiGraphics);
        this.drawPlayerSlotBackgrounds(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Level level = Minecraft.getInstance().level;
        TechMinerBlockEntity blockEntity = this.menu.getBlockEntity(level);
        this.hoverTooltipLines = List.of();

        guiGraphics.drawString(this.font, this.title, 10, 10, 0xEAFBFF, false);
        int tier = blockEntity == null ? 0 : blockEntity.getFilterTier();
        int capacity = blockEntity == null ? 0 : blockEntity.getFilterCapacity();
        int entries = blockEntity == null ? 0 : blockEntity.getActiveFilterEntryCount();

        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.filter_tier", tier), 106, 66, 0x9FEFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.microtech.tech_miner.filter_entries", entries, capacity), 106, 78, 0xD8E0E6, false);
        guiGraphics.drawString(this.font, Component.literal(MicroTechGuiHelper.trimToWidth(this.font, Component.translatable("gui.microtech.tech_miner.filter_help"), 158)), 28, 90, 0x8FA4B5, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 28, 100, 0xEAFBFF, false);

        int hovered = this.getHoveredFilterSlot(mouseX, mouseY);
        if (hovered >= 0) {
            if (hovered >= capacity) {
                this.hoverTooltipLines = List.of(Component.translatable("tooltip.microtech.tech_miner.filter_slot_locked"));
            } else if (blockEntity != null && !blockEntity.getFilterDisplayStack(hovered).isEmpty()) {
                this.hoverTooltipLines = List.of(blockEntity.getFilterDisplayStack(hovered).getHoverName());
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!this.hoverTooltipLines.isEmpty()) {
            guiGraphics.renderTooltip(this.font, this.hoverTooltipLines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = this.getHoveredFilterSlot((int) mouseX, (int) mouseY);
        if (index >= 0) {
            this.handleFilterSlotClick(index, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleFilterSlotClick(int index, int button) {
        Level level = Minecraft.getInstance().level;
        TechMinerBlockEntity blockEntity = this.menu.getBlockEntity(level);
        if (blockEntity == null || index < 0 || index >= TechMinerBlockEntity.MAX_FILTER_ENTRIES || index >= blockEntity.getFilterCapacity()) {
            return;
        }

        if (button == 1) {
            this.sendFilterPayload(TechMinerFilterPayload.ACTION_REMOVE, index, "");
            return;
        }

        ItemStack carried = this.menu.getCarried();
        if (carried.isEmpty() || !(carried.getItem() instanceof BlockItem blockItem)) {
            this.showClientMessage("message.microtech.tech_miner.filter_invalid");
            return;
        }
        if (!TechMinerTargetHelper.isValidTarget(blockItem.getBlock().defaultBlockState())) {
            this.showClientMessage("message.microtech.tech_miner.filter_invalid");
            return;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        this.sendFilterPayload(TechMinerFilterPayload.ACTION_SET, index, blockId.toString());
    }

    private void handleClearFilter() {
        long now = Util.getMillis();
        if (now > this.clearConfirmUntil) {
            this.clearConfirmUntil = now + 1500L;
            if (this.clearButton != null) {
                this.clearButton.setMessage(Component.translatable("gui.microtech.tech_miner.clear_filter_confirm"));
            }
            return;
        }

        if (this.clearButton != null) {
            this.clearButton.setMessage(Component.translatable("gui.microtech.tech_miner.clear_filter"));
        }
        this.clearConfirmUntil = 0L;
        this.sendFilterPayload(TechMinerFilterPayload.ACTION_CLEAR, 0, "");
    }

    private void clickMachineButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void sendFilterPayload(int action, int index, String blockId) {
        var pos = this.menu.getBlockPos();
        PacketDistributor.sendToServer(new TechMinerFilterPayload(pos.getX(), pos.getY(), pos.getZ(), action, index, blockId));
    }

    private void showClientMessage(String key) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.translatable(key), true);
        }
    }

    private void drawFilterSlots(GuiGraphics guiGraphics) {
        Level level = Minecraft.getInstance().level;
        TechMinerBlockEntity blockEntity = this.menu.getBlockEntity(level);
        int capacity = blockEntity == null ? 0 : blockEntity.getFilterCapacity();

        for (int index = 0; index < TechMinerBlockEntity.MAX_FILTER_ENTRIES; index++) {
            int column = index % FILTER_COLUMNS;
            int row = index / FILTER_COLUMNS;
            int x = this.leftPos + FILTER_X + column * SLOT_SIZE;
            int y = this.topPos + FILTER_Y + row * SLOT_SIZE;
            this.drawSlot(guiGraphics, x, y);

            if (blockEntity != null) {
                ItemStack stack = blockEntity.getFilterDisplayStack(index);
                if (!stack.isEmpty()) {
                    guiGraphics.renderItem(stack, x + 1, y + 1);
                    guiGraphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
                }
            }

            if (index >= capacity) {
                guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xAA000000);
            }
        }
    }

    private void drawPlayerSlotBackgrounds(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.drawSlot(guiGraphics, this.leftPos + TechMinerFilterMenu.PLAYER_INV_X + column * 18, this.topPos + TechMinerFilterMenu.PLAYER_INV_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            this.drawSlot(guiGraphics, this.leftPos + TechMinerFilterMenu.PLAYER_INV_X + column * 18, this.topPos + TechMinerFilterMenu.HOTBAR_Y);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF59636E);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF11161C);
    }

    private int getHoveredFilterSlot(int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos - FILTER_X;
        int localY = mouseY - this.topPos - FILTER_Y;
        if (localX < 0 || localY < 0) {
            return -1;
        }
        int column = localX / SLOT_SIZE;
        int row = localY / SLOT_SIZE;
        if (column < 0 || column >= FILTER_COLUMNS || row < 0 || row >= 3) {
            return -1;
        }
        int withinX = localX % SLOT_SIZE;
        int withinY = localY % SLOT_SIZE;
        if (withinX >= 18 || withinY >= 18) {
            return -1;
        }
        return row * FILTER_COLUMNS + column;
    }
}
