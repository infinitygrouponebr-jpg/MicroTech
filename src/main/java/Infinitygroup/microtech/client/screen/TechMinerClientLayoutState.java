package Infinitygroup.microtech.client.screen;

import Infinitygroup.microtech.client.TechArmorClientConfig;
import Infinitygroup.microtech.menu.TechMinerGuiLayout;
import net.minecraft.client.Minecraft;

public final class TechMinerClientLayoutState {
    private static boolean smallResolutionWarningPending;
    private static boolean smallResolutionWarningShown;

    private TechMinerClientLayoutState() {
    }

    public static String chooseInitialTechMinerLayoutName() {
        if (!TechArmorClientConfig.USE_SEPARATED_TECH_MINER_LAYOUT.get()) {
            return TechMinerGuiLayout.COMPACT.name();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return TechMinerGuiLayout.COMPACT.name();
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (!TechMinerGuiLayout.canFitSeparated(width, height)) {
            if (!smallResolutionWarningShown) {
                smallResolutionWarningPending = true;
            }
            return TechMinerGuiLayout.COMPACT.name();
        }

        return TechMinerGuiLayout.SEPARATED.name();
    }

    public static boolean consumeSmallResolutionWarning() {
        if (!smallResolutionWarningPending || smallResolutionWarningShown) {
            return false;
        }

        smallResolutionWarningPending = false;
        smallResolutionWarningShown = true;
        return true;
    }
}
