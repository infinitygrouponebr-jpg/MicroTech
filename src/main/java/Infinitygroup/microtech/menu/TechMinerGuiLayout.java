package Infinitygroup.microtech.menu;

public final class TechMinerGuiLayout {
    public static final int GUI_WIDTH = 256;
    public static final int GUI_HEIGHT = 256;
    public static final int SLOT_SIZE = 18;
    public static final int BAR_INSET = 1;

    public static final Rect TITLE = new Rect(8, 7, 240, 12);

    public static final Rect INFO_PANEL = new Rect(10, 24, 136, 92);
    public static final Pos INFO_TITLE = new Pos(16, 29);
    public static final Pos STATUS_TEXT = new Pos(16, 42);
    public static final Pos TARGETS_TEXT = new Pos(16, 53);
    public static final Pos FILTER_TEXT = new Pos(16, 64);
    public static final Pos NEXT_TARGET_TEXT = new Pos(16, 75);
    public static final Pos PROGRESS_TEXT = new Pos(16, 88);
    public static final Rect PROGRESS_BAR = new Rect(16, 101, 124, 8);

    public static final Rect UPGRADES_PANEL = new Rect(152, 24, 56, 92);
    public static final Pos UPGRADES_TITLE = new Pos(158, 29);
    public static final int UPGRADE_SLOT_COUNT = 4;
    public static final Pos[] UPGRADE_SLOTS = {
            new Pos(171, 35),
            new Pos(171, 55),
            new Pos(171, 75),
            new Pos(171, 95)
    };

    public static final Rect ENERGY_PANEL = new Rect(214, 24, 32, 92);
    public static final Pos ENERGY_TITLE = new Pos(224, 29);
    public static final Rect ENERGY_BAR = new Rect(225, 38, 10, 66);
    public static final Pos ENERGY_PERCENT = new Pos(221, 107);

    public static final Rect INVENTORY_BUTTON = new Rect(10, 121, 116, 18);
    public static final Rect CONFIG_BUTTON = new Rect(130, 121, 116, 18);
    public static final Rect SCAN_BUTTON = new Rect(10, 143, 116, 18);
    public static final Rect START_STOP_BUTTON = new Rect(130, 143, 116, 18);

    public static final Pos PLAYER_INVENTORY_TITLE = new Pos(47, 165);
    public static final Pos PLAYER_INVENTORY = new Pos(47, 177);
    public static final Pos PLAYER_HOTBAR = new Pos(47, 235);

    private TechMinerGuiLayout() {
    }

    public record Pos(int x, int y) {
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }
    }
}
