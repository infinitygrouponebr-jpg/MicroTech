package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.client.TechSwordClientKeybinds;
import Infinitygroup.microtech.item.TechSwordData;
import Infinitygroup.microtech.network.TechSwordAbilitySelectionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class TechSwordAbilityScreen extends Screen {
    private static final int EMPTY_PANEL_WIDTH = 240;
    private static final int EMPTY_PANEL_HEIGHT = 70;
    private static final int ACTIVE_PANEL_WIDTH = 230;
    private static final int ACTIVE_PANEL_HEIGHT = 92;
    private static final int VISIBLE_SLOTS = 7;
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 6;

    private final ItemStack swordStack;
    private List<String> activeAbilities = List.of();
    private int selectedIndex;

    public TechSwordAbilityScreen(ItemStack swordStack) {
        super(Component.translatable("screen.microtech.tech_sword_abilities.title"));
        this.swordStack = swordStack;
    }

    @Override
    protected void init() {
        super.init();
        this.refreshAbilities();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x7F000000);

        boolean empty = this.activeAbilities.isEmpty();
        int panelWidth = empty ? EMPTY_PANEL_WIDTH : ACTIVE_PANEL_WIDTH;
        int panelHeight = empty ? EMPTY_PANEL_HEIGHT : ACTIVE_PANEL_HEIGHT;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = Math.max(20, this.height - panelHeight - 26);

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1B1E23);
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF23272D);
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 3, 0xFF6CE7FF);

        guiGraphics.drawCenteredString(this.font, this.title, panelX + panelWidth / 2, panelY + 8, 0xFFFFFF);

        if (empty) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.microtech.tech_sword_abilities.empty"),
                    panelX + panelWidth / 2,
                    panelY + 24,
                    0xD0D0D0
            );
        } else {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable(
                            "screen.microtech.tech_sword_abilities.selected",
                            this.getAbilityDisplayName(this.getSelectedAbilityLabel())
                    ),
                    panelX + panelWidth / 2,
                    panelY + 24,
                    0xB7F3FF
            );

            int slotRowWidth = VISIBLE_SLOTS * SLOT_SIZE + (VISIBLE_SLOTS - 1) * SLOT_GAP;
            int slotsX = panelX + (panelWidth - slotRowWidth) / 2;
            int slotsY = panelY + 40;
            int startIndex = this.getVisibleStartIndex();

            for (int slot = 0; slot < VISIBLE_SLOTS; slot++) {
                int slotX = slotsX + slot * (SLOT_SIZE + SLOT_GAP);
                int abilityIndex = startIndex + slot;
                boolean hasAbility = abilityIndex >= 0 && abilityIndex < this.activeAbilities.size();
                boolean selected = hasAbility && abilityIndex == this.selectedIndex;
                this.drawSlot(guiGraphics, slotX, slotsY, selected, hasAbility ? this.activeAbilities.get(abilityIndex) : "");
            }
        }

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.microtech.tech_sword_abilities.controls"),
                panelX + panelWidth / 2,
                panelY + (empty ? 44 : 78),
                0x808B95
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activeAbilities.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (scrollY > 0.0D) {
            this.moveSelection(-1);
            return true;
        }
        if (scrollY < 0.0D) {
            this.moveSelection(1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (!this.activeAbilities.isEmpty()) {
                this.confirmSelection();
                return true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (TechSwordClientKeybinds.OPEN_ABILITY_SELECTOR.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        if (this.minecraft != null) {
            if (this.minecraft.options.keyInventory.matches(keyCode, scanCode) || keyCode == 256) {
                this.onClose();
                return true;
            }
        }

        if (!this.activeAbilities.isEmpty()) {
            if (keyCode == 263 || keyCode == 65) {
                this.moveSelection(-1);
                return true;
            }

            if (keyCode == 262 || keyCode == 68) {
                this.moveSelection(1);
                return true;
            }

            if (keyCode == 257 || keyCode == 335) {
                this.confirmSelection();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void refreshAbilities() {
        this.activeAbilities = TechSwordData.getInstalledActiveAbilities(this.swordStack);
        String selectedAbility = TechSwordData.getSelectedActiveAbility(this.swordStack);
        this.selectedIndex = this.activeAbilities.indexOf(selectedAbility);
        if (this.selectedIndex < 0) {
            this.selectedIndex = 0;
        }
    }

    private int getVisibleStartIndex() {
        if (this.activeAbilities.size() <= VISIBLE_SLOTS) {
            return 0;
        }

        int centered = this.selectedIndex - (VISIBLE_SLOTS / 2);
        return Mth.clamp(centered, 0, this.activeAbilities.size() - VISIBLE_SLOTS);
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y, boolean selected, String abilityId) {
        int outer = selected ? 0xFF6CE7FF : 0xFF111418;
        int inner = selected ? 0xFF3C6D7A : 0xFF252B31;
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, outer);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, inner);

        if (!abilityId.isBlank()) {
            ItemStack icon = this.getAbilityIcon(abilityId);
            if (!icon.isEmpty()) {
                guiGraphics.renderItem(icon, x + 4, y + 4);
            } else {
                String label = prettifyAbilityId(abilityId);
                guiGraphics.drawCenteredString(this.font, Component.literal(label), x + SLOT_SIZE / 2, y + 8, 0xFFFFFF);
            }
        }
    }

    private void moveSelection(int delta) {
        if (this.activeAbilities.isEmpty()) {
            return;
        }

        this.selectedIndex = Mth.positiveModulo(this.selectedIndex + delta, this.activeAbilities.size());
    }

    private void confirmSelection() {
        if (this.activeAbilities.isEmpty() || this.minecraft == null) {
            return;
        }

        String selectedAbility = this.activeAbilities.get(this.selectedIndex);
        TechSwordData.setSelectedActiveAbility(this.swordStack, selectedAbility);
        PacketDistributor.sendToServer(new TechSwordAbilitySelectionPayload(selectedAbility));
        this.onClose();
    }

    private String getSelectedAbilityLabel() {
        if (this.activeAbilities.isEmpty()) {
            return "";
        }
        return this.activeAbilities.get(this.selectedIndex);
    }

    private Component getAbilityDisplayName(String abilityId) {
        return switch (abilityId) {
            case "overload" -> Component.translatable("ability.microtech.overload");
            default -> Component.literal(prettifyAbilityId(abilityId));
        };
    }

    private ItemStack getAbilityIcon(String abilityId) {
        return switch (abilityId) {
            case "overload" -> Microtech.OVERLOAD_CHIP.get().getDefaultInstance();
            default -> ItemStack.EMPTY;
        };
    }

    private static String prettifyAbilityId(String abilityId) {
        String[] parts = abilityId.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? abilityId : builder.toString();
    }
}
