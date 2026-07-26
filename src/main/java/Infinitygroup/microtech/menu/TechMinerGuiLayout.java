package Infinitygroup.microtech.menu;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;

public final class TechMinerGuiLayout {
    public static final int SLOT_SIZE = 18;
    public static final int BAR_INSET = 1;
    public static final int SEPARATED_MIN_MARGIN = 8;

    public static final Layout COMPACT = new Layout(
            "COMPACT",
            false,
            256,
            256,
            new Rect(0, 0, 256, 256),
            new Rect(0, 0, 256, 256),
            new Rect(10, 24, 136, 92),
            new Rect(152, 24, 56, 92),
            new Rect(214, 24, 32, 92),
            new Rect(16, 101, 124, 8),
            new Rect(225, 38, 10, 66),
            new Rect(10, 121, 116, 18),
            new Rect(130, 121, 116, 18),
            new Rect(10, 143, 116, 18),
            new Rect(130, 143, 116, 18),
            new Rect(8, 7, 240, 12),
            new Pos(16, 29),
            new Pos(16, 42),
            new Pos(16, 53),
            new Pos(16, 64),
            new Pos(16, 75),
            new Pos(16, 88),
            new Pos(158, 29),
            new Pos(224, 29),
            new Pos(221, 107),
            new Pos(47, 165),
            new Pos(47, 177),
            new Pos(47, 235),
            new Pos[] {
                    new Pos(171, 35),
                    new Pos(171, 55),
                    new Pos(171, 75),
                    new Pos(171, 95)
            }
    );

    public static final Layout SEPARATED = new Layout(
            "SEPARATED",
            true,
            408,
            238,
            new Rect(0, 0, 210, 218),
            new Rect(232, 130, 176, 108),
            new Rect(8, 22, 138, 92),
            new Rect(152, 22, 50, 92),
            new Rect(8, 118, 38, 78),
            new Rect(14, 99, 126, 8),
            new Rect(22, 134, 10, 50),
            new Rect(52, 120, 72, 18),
            new Rect(130, 120, 72, 18),
            new Rect(52, 142, 72, 18),
            new Rect(130, 142, 72, 18),
            new Rect(8, 7, 194, 12),
            new Pos(14, 27),
            new Pos(14, 40),
            new Pos(14, 51),
            new Pos(14, 62),
            new Pos(14, 73),
            new Pos(14, 86),
            new Pos(155, 27),
            new Pos(18, 123),
            new Pos(11, 187),
            new Pos(239, 137),
            new Pos(239, 147),
            new Pos(239, 205),
            new Pos[] {
                    new Pos(168, 36),
                    new Pos(168, 56),
                    new Pos(168, 76),
                    new Pos(168, 96)
            }
    );

    public static final int UPGRADE_SLOT_COUNT = 4;

    private TechMinerGuiLayout() {
    }

    public static Layout resolveInitialLayout() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> stateClass = Class.forName("Infinitygroup.microtech.client.screen.TechMinerClientLayoutState");
                Method method = stateClass.getMethod("chooseInitialTechMinerLayoutName");
                Object result = method.invoke(null);
                return SEPARATED.name().equals(result) ? SEPARATED : COMPACT;
            } catch (ReflectiveOperationException ignored) {
                return COMPACT;
            }
        }
        return COMPACT;
    }

    public static boolean canFitSeparated(int guiWidth, int guiHeight) {
        return guiWidth >= SEPARATED.guiWidth() + SEPARATED_MIN_MARGIN
                && guiHeight >= SEPARATED.guiHeight() + SEPARATED_MIN_MARGIN;
    }

    public record Layout(
            String name,
            boolean separated,
            int guiWidth,
            int guiHeight,
            Rect machinePanel,
            Rect playerPanel,
            Rect infoPanel,
            Rect upgradesPanel,
            Rect energyPanel,
            Rect progressBar,
            Rect energyBar,
            Rect inventoryButton,
            Rect configButton,
            Rect scanButton,
            Rect startStopButton,
            Rect title,
            Pos infoTitle,
            Pos statusText,
            Pos targetsText,
            Pos filterText,
            Pos nextTargetText,
            Pos progressText,
            Pos upgradesTitle,
            Pos energyTitle,
            Pos energyPercent,
            Pos playerInventoryTitle,
            Pos playerInventory,
            Pos playerHotbar,
            Pos[] upgradeSlots
    ) {
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
