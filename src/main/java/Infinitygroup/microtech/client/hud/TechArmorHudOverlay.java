package Infinitygroup.microtech.client.hud;

import Infinitygroup.microtech.Microtech;
import Infinitygroup.microtech.client.TechArmorClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Microtech.MODID, value = Dist.CLIENT)
public final class TechArmorHudOverlay {
    private TechArmorHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        if (!TechArmorClientConfig.ENABLE_TECH_ARMOR_HUD.get()) {
            return;
        }

        if (minecraft.screen != null || minecraft.options.hideGui || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        if (!isFullTechArmor(player)) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        TechArmorHudData data = TechArmorHudData.collect(player);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float scale = TechArmorClientConfig.HUD_SCALE.get().floatValue();
        int alpha = (int) (TechArmorClientConfig.HUD_OPACITY.get() * 255.0D) & 0xFF;
        int pad = TechArmorClientConfig.PANEL_PADDING.get();
        int line = TechArmorClientConfig.LINE_HEIGHT.get();
        int bg = argb(alpha, 10, 16, 24);
        int cyan = argb(255, 83, 221, 255);
        int cyanSoft = argb(alpha, 83, 221, 255);
        int white = argb(255, 240, 248, 255);
        int gray = argb(255, 150, 163, 176);
        int red = argb(255, 255, 92, 92);
        int yellow = argb(255, 255, 208, 92);
        int green = argb(255, 98, 255, 189);
        int statusColor = switch (data.status()) {
            case "ONLINE" -> cyan;
            case "OFFLINE" -> gray;
            case "LOW POWER" -> yellow;
            default -> red;
        };

        var pose = graphics.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0F);
        try {
            int topLeftX = hud(8, scale);
            int topLeftY = hud(8, scale);
            int topLeftW = hud(96, scale);
            int topLeftH = hud(36, scale);

            int topRightW = hud(128, scale);
            int topRightH = hud(40, scale);
            int topRightX = hud(width - 8 - 128, scale);
            int topRightY = hud(8, scale);

            int bottomLeftW = hud(92, scale);
            int bottomLeftH = hud(50, scale);
            int bottomLeftX = hud(8, scale);
            int bottomLeftY = hud(height - 8 - 50, scale);

            int bottomRightW = hud(116, scale);
            int bottomRightH = hud(50, scale);
            int bottomRightX = hud(width - 8 - 116, scale);
            int bottomRightY = hud(height - 8 - 50, scale);

            drawPanel(graphics, topLeftX, topLeftY, topLeftW, topLeftH, bg, cyanSoft);
            drawPanel(graphics, topRightX, topRightY, topRightW, topRightH, bg, cyanSoft);
            drawPanel(graphics, bottomLeftX, bottomLeftY, bottomLeftW, bottomLeftH, bg, cyanSoft);
            drawPanel(graphics, bottomRightX, bottomRightY, bottomRightW, bottomRightH, bg, cyanSoft);

            int topLeftTextX = topLeftX + pad;
            int topLeftTextY = topLeftY + pad;
            graphics.drawString(font, "TECH ARMOR", topLeftTextX, topLeftTextY, white, false);
            graphics.drawString(font, data.status(), topLeftTextX, topLeftTextY + line, statusColor, false);
            graphics.drawString(font, String.format("%d%%", data.chargePercent()), topLeftTextX, topLeftTextY + line * 2, cyan, false);

            if (TechArmorClientConfig.SHOW_COORDINATES.get()) {
                int topRightTextX = topRightX + pad;
                int topRightTextY = topRightY + pad;
                graphics.drawString(font, data.coords(), topRightTextX, topRightTextY, white, false);
                graphics.drawString(font, "DIR " + data.direction(), topRightTextX, topRightTextY + line, cyan, false);
                graphics.drawString(font, String.format("HEALTH %.0f/%.0f", data.healthCurrent(), data.healthMax()), topRightTextX, topRightTextY + line * 2, green, false);
            }

            if (TechArmorClientConfig.SHOW_PIECE_ENERGY.get()) {
                int bottomLeftTextX = bottomLeftX + pad;
                int bottomLeftTextY = bottomLeftY + pad;
                graphics.drawString(font, "H " + data.helmetPercent() + "%", bottomLeftTextX, bottomLeftTextY, white, false);
                graphics.drawString(font, "C " + data.chestPercent() + "%", bottomLeftTextX, bottomLeftTextY + line, white, false);
                graphics.drawString(font, "L " + data.legsPercent() + "%", bottomLeftTextX, bottomLeftTextY + line * 2, white, false);
                graphics.drawString(font, "B " + data.bootsPercent() + "%", bottomLeftTextX, bottomLeftTextY + line * 3, white, false);
            }

            if (TechArmorClientConfig.SHOW_ENERGY.get()) {
                int bottomRightTextX = bottomRightX + pad;
                int bottomRightTextY = bottomRightY + pad;
                graphics.drawString(font, data.energyCurrent() + " / " + data.energyMax() + " FE", bottomRightTextX, bottomRightTextY, white, false);
                graphics.drawString(font, data.flightLine(), bottomRightTextX, bottomRightTextY + line, cyan, false);
                drawVerticalBar(graphics, bottomRightX + bottomRightW - hud(10, scale), bottomRightY + hud(4, scale), hud(6, scale), bottomRightH - hud(8, scale), data.chargePercent(), cyan, red);
            }

            drawProtectionIcon(graphics, bottomRightX + hud(6, scale), bottomRightY + hud(20, scale), statusColor, cyanSoft);
        } finally {
            pose.popPose();
        }
    }

    private static boolean isFullTechArmor(LocalPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        return head.is(Microtech.TECH_ARMOR_HELMET.get())
                && chest.is(Microtech.TECH_ARMOR_CHESTPLATE.get())
                && legs.is(Microtech.TECH_ARMOR_LEGGINGS.get())
                && boots.is(Microtech.TECH_ARMOR_BOOTS.get());
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill, int outline) {
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, outline);
        graphics.fill(x, y + height - 1, x + width, y + height, outline);
        graphics.fill(x, y, x + 1, y + height, outline);
        graphics.fill(x + width - 1, y, x + width, y + height, outline);
    }

    private static void drawVerticalBar(GuiGraphics graphics, int x, int y, int width, int height, int percent, int fillColor, int emptyColor) {
        graphics.fill(x, y, x + width, y + height, emptyColor);
        int filled = Math.max(0, Math.min(height, Math.round(height * (percent / 100.0F))));
        graphics.fill(x, y + height - filled, x + width, y + height, fillColor);
        graphics.fill(x - 1, y - 1, x + width + 1, y, emptyColor);
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, emptyColor);
    }

    private static void drawProtectionIcon(GuiGraphics graphics, int x, int y, int color, int accent) {
        graphics.fill(x, y + 6, x + 10, y + 12, accent);
        graphics.fill(x + 2, y + 2, x + 8, y + 6, color);
        graphics.fill(x + 4, y, x + 6, y + 2, color);
        graphics.fill(x + 3, y + 8, x + 7, y + 10, color);
    }

    private static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private static int hud(int px, float scale) {
        return Math.round(px / scale);
    }
}
