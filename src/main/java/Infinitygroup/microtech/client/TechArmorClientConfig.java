package Infinitygroup.microtech.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TechArmorClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_TECH_ARMOR_HUD = BUILDER
            .comment("Enable the Tech Armor HUD overlay when the full set is equipped.")
            .define("enableTechArmorHud", true);

    public static final ModConfigSpec.DoubleValue HUD_OPACITY = BUILDER
            .comment("Opacity of the Tech Armor HUD overlay.")
            .defineInRange("hudOpacity", 0.45D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue HUD_SCALE = BUILDER
            .comment("Scale of the Tech Armor HUD overlay.")
            .defineInRange("hudScale", 0.75D, 0.5D, 1.0D);

    public static final ModConfigSpec.BooleanValue COMPACT_HUD = BUILDER
            .comment("Use the compact Tech Armor HUD layout.")
            .define("compactHud", true);

    public static final ModConfigSpec.IntValue PANEL_PADDING = BUILDER
            .comment("Internal padding for Tech Armor HUD panels.")
            .defineInRange("panelPadding", 4, 0, 12);

    public static final ModConfigSpec.IntValue LINE_HEIGHT = BUILDER
            .comment("Line spacing for the Tech Armor HUD.")
            .defineInRange("lineHeight", 10, 6, 16);

    public static final ModConfigSpec.BooleanValue SHOW_COORDINATES = BUILDER
            .comment("Show player coordinates on the Tech Armor HUD.")
            .define("showCoordinates", true);

    public static final ModConfigSpec.BooleanValue SHOW_PIECE_ENERGY = BUILDER
            .comment("Show per-piece armor energy on the Tech Armor HUD.")
            .define("showPieceEnergy", true);

    public static final ModConfigSpec.BooleanValue SHOW_ENERGY = BUILDER
            .comment("Show simulated or real energy values on the Tech Armor HUD.")
            .define("showEnergy", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TechArmorClientConfig() {
    }
}
