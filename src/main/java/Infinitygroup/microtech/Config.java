package Infinitygroup.microtech;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = Microtech.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    public static final ModConfigSpec.BooleanValue ENABLE_CONTROLLER_CHIP = BUILDER
            .comment("Enables the Controller Chip item.")
            .define("enableControllerChip", true);
    public static final ModConfigSpec.BooleanValue CONSUME_CHIP_ON_USE = BUILDER
            .comment("Consumes one Controller Chip when successfully installed outside creative mode.")
            .define("consumeChipOnUse", true);
    public static final ModConfigSpec.BooleanValue ALLOW_HOSTILE_MOBS = BUILDER
            .comment("Allows hostile mobs to be controlled.")
            .define("allowHostileMobs", true);
    public static final ModConfigSpec.BooleanValue ALLOW_NEUTRAL_MOBS = BUILDER
            .comment("Allows neutral mobs to be controlled.")
            .define("allowNeutralMobs", true);
    public static final ModConfigSpec.BooleanValue ALLOW_PASSIVE_MOBS = BUILDER
            .comment("Allows passive mobs without owners to be controlled.")
            .define("allowPassiveMobs", true);
    public static final ModConfigSpec.BooleanValue ALLOW_MODDED_MOBS = BUILDER
            .comment("Allows compatible mobs from other mods to be controlled.")
            .define("allowModdedMobs", true);
    public static final ModConfigSpec.BooleanValue ALLOW_BOSSES = BUILDER
            .comment("Allows boss-like entities. Default false to avoid unsafe behavior.")
            .define("allowBosses", false);
    public static final ModConfigSpec.IntValue MAX_CONTROLLED_MOBS_PER_PLAYER = BUILDER
            .comment("Maximum loaded controlled mobs per player.")
            .defineInRange("maxControlledMobsPerPlayer", 8, 0, 128);
    public static final ModConfigSpec.DoubleValue FOLLOW_DISTANCE = BUILDER
            .comment("Preferred minimum distance from the controller.")
            .defineInRange("followDistance", 3.0D, 1.0D, 32.0D);
    public static final ModConfigSpec.DoubleValue START_FOLLOWING_DISTANCE = BUILDER
            .comment("Distance at which controlled mobs start following.")
            .defineInRange("startFollowingDistance", 8.0D, 2.0D, 64.0D);
    public static final ModConfigSpec.DoubleValue TELEPORT_DISTANCE = BUILDER
            .comment("Distance at which recovery teleport may be attempted.")
            .defineInRange("teleportDistance", 32.0D, 8.0D, 256.0D);
    public static final ModConfigSpec.DoubleValue DEFEND_RADIUS = BUILDER
            .comment("Radius used to find threats around controlled mobs and their controller.")
            .defineInRange("defendRadius", 18.0D, 4.0D, 64.0D);
    public static final ModConfigSpec.DoubleValue OWNER_SEARCH_RADIUS = BUILDER
            .comment("Reserved search radius for owner-related checks.")
            .defineInRange("ownerSearchRadius", 32.0D, 4.0D, 128.0D);
    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE = BUILDER
            .comment("Allows controlled mobs, controller and allies to damage each other.")
            .define("friendlyFire", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_MOBS_HELP_EACH_OTHER = BUILDER
            .comment("Controlled mobs owned by the same player defend each other.")
            .define("controlledMobsHelpEachOther", true);
    public static final ModConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION_TELEPORT = BUILDER
            .comment("Allows future cross-dimension recovery teleport. Current implementation keeps this disabled operationally for safety.")
            .define("allowCrossDimensionTeleport", false);
    public static final ModConfigSpec.BooleanValue CONTROLLER_CHIP_DROPS_ON_CONTROLLED_MOB_DEATH = BUILDER
            .comment("Drops one Controller Chip when a controlled mob dies.")
            .define("controllerChipDropsOnControlledMobDeath", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTROLLER_CHIP_ENTITY_ALLOWLIST = BUILDER
            .comment("If non-empty, only these entity IDs can receive a Controller Chip.")
            .defineListAllowEmpty("controllerChipEntityAllowlist", List.of(), Config::validateEntityName);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTROLLER_CHIP_ENTITY_DENYLIST = BUILDER
            .comment("Entity IDs that can never receive a Controller Chip.")
            .defineListAllowEmpty("controllerChipEntityDenylist", List.of(), Config::validateEntityName);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    public static boolean enableControllerChip;
    public static boolean consumeChipOnUse;
    public static boolean allowHostileMobs;
    public static boolean allowNeutralMobs;
    public static boolean allowPassiveMobs;
    public static boolean allowModdedMobs;
    public static boolean allowBosses;
    public static int maxControlledMobsPerPlayer;
    public static double followDistance;
    public static double startFollowingDistance;
    public static double teleportDistance;
    public static double defendRadius;
    public static double ownerSearchRadius;
    public static boolean friendlyFire;
    public static boolean controlledMobsHelpEachOther;
    public static boolean allowCrossDimensionTeleport;
    public static boolean controllerChipDropsOnControlledMobDeath;
    public static Set<ResourceLocation> controllerChipEntityAllowlist = Set.of();
    public static Set<ResourceLocation> controllerChipEntityDenylist = Set.of();
    public static Set<ResourceLocation> controllerChipBossDenylist = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "wither"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "ender_dragon"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "warden")
    );

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    private static boolean validateEntityName(final Object obj) {
        if (!(obj instanceof String entityName)) {
            return false;
        }
        try {
            ResourceLocation id = ResourceLocation.parse(entityName);
            return BuiltInRegistries.ENTITY_TYPE.containsKey(id);
        } catch (Exception ignored) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        loadValues();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        loadValues();
    }

    private static void loadValues() {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream().map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName))).collect(Collectors.toSet());
        enableControllerChip = ENABLE_CONTROLLER_CHIP.get();
        consumeChipOnUse = CONSUME_CHIP_ON_USE.get();
        allowHostileMobs = ALLOW_HOSTILE_MOBS.get();
        allowNeutralMobs = ALLOW_NEUTRAL_MOBS.get();
        allowPassiveMobs = ALLOW_PASSIVE_MOBS.get();
        allowModdedMobs = ALLOW_MODDED_MOBS.get();
        allowBosses = ALLOW_BOSSES.get();
        maxControlledMobsPerPlayer = MAX_CONTROLLED_MOBS_PER_PLAYER.get();
        followDistance = FOLLOW_DISTANCE.get();
        startFollowingDistance = START_FOLLOWING_DISTANCE.get();
        teleportDistance = TELEPORT_DISTANCE.get();
        defendRadius = DEFEND_RADIUS.get();
        ownerSearchRadius = OWNER_SEARCH_RADIUS.get();
        friendlyFire = FRIENDLY_FIRE.get();
        controlledMobsHelpEachOther = CONTROLLED_MOBS_HELP_EACH_OTHER.get();
        allowCrossDimensionTeleport = ALLOW_CROSS_DIMENSION_TELEPORT.get();
        controllerChipDropsOnControlledMobDeath = CONTROLLER_CHIP_DROPS_ON_CONTROLLED_MOB_DEATH.get();
        controllerChipEntityAllowlist = parseEntitySet(CONTROLLER_CHIP_ENTITY_ALLOWLIST.get());
        controllerChipEntityDenylist = parseEntitySet(CONTROLLER_CHIP_ENTITY_DENYLIST.get());
    }

    private static Set<ResourceLocation> parseEntitySet(List<? extends String> values) {
        Set<ResourceLocation> parsed = new HashSet<>();
        for (String value : values) {
            try {
                parsed.add(ResourceLocation.parse(value));
            } catch (Exception ignored) {
            }
        }
        return parsed;
    }
}
