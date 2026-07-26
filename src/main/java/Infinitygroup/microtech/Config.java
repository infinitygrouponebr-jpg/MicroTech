package Infinitygroup.microtech;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public static final ModConfigSpec.BooleanValue CONTROLLER_CHIP_DEBUG = BUILDER
            .comment("Enables low-volume debug logs for Controller Chip decisions.")
            .define("controllerChipDebug", false);
    public static final ModConfigSpec.BooleanValue CONTROLLER_CHIP_ALLOW_CREEPER_EXPLOSION = BUILDER
            .comment("Allows controlled creepers to use suicidal explosion behavior when safe. Default false.")
            .define("controllerChipAllowCreeperExplosion", false);
    public static final ModConfigSpec.BooleanValue CONTROLLER_CHIP_ALLOW_VILLAGERS = BUILDER
            .comment("Allows installing Controller Chips in villagers. Default false to avoid profession, village and trade conflicts.")
            .define("controllerChipAllowVillagers", false);
    public static final ModConfigSpec.IntValue CONTROLLER_CHIP_THREAT_MEMORY_TICKS = BUILDER
            .comment("Ticks before remembered controller threats expire.")
            .defineInRange("controllerChipThreatMemoryTicks", 200, 20, 1200);
    public static final ModConfigSpec.IntValue CONTROLLED_TARGET_COMMAND_DURATION = BUILDER
            .comment("Ticks before a controller-issued attack target expires.")
            .defineInRange("controlledTargetCommandDuration", 400, 20, 2400);
    public static final ModConfigSpec.DoubleValue CONTROLLED_TARGET_MAXIMUM_DISTANCE = BUILDER
            .comment("Maximum distance for controller-issued attack targets.")
            .defineInRange("controlledTargetMaximumDistance", 64.0D, 4.0D, 256.0D);
    public static final ModConfigSpec.BooleanValue ENABLE_PASSIVE_MOB_BUFFS = BUILDER
            .comment("Allows support-role controlled passive mobs to grant short buffs to their controller.")
            .define("enablePassiveMobBuffs", true);
    public static final ModConfigSpec.DoubleValue PASSIVE_BUFF_RADIUS = BUILDER
            .comment("Maximum distance for support buffs.")
            .defineInRange("passiveBuffRadius", 12.0D, 2.0D, 32.0D);
    public static final ModConfigSpec.IntValue PASSIVE_BUFF_REFRESH_INTERVAL = BUILDER
            .comment("Ticks between support buff refresh attempts.")
            .defineInRange("passiveBuffRefreshInterval", 40, 10, 200);
    public static final ModConfigSpec.BooleanValue ALLOW_BUFF_STACKING = BUILDER
            .comment("Allows equal-strength support buffs to refresh aggressively.")
            .define("allowBuffStacking", false);
    public static final ModConfigSpec.IntValue MAX_SUPPORT_MOBS_AFFECTING_PLAYER = BUILDER
            .comment("Reserved cap for support effects per player.")
            .defineInRange("maxSupportMobsAffectingPlayer", 4, 1, 32);
    public static final ModConfigSpec.BooleanValue CHICKEN_BUFF_ENABLED = BUILDER.define("chickenBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue PIG_BUFF_ENABLED = BUILDER.define("pigBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue COW_BUFF_ENABLED = BUILDER.define("cowBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue SHEEP_BUFF_ENABLED = BUILDER.define("sheepBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue RABBIT_BUFF_ENABLED = BUILDER.define("rabbitBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue MINI_SLIME_BUFF_ENABLED = BUILDER.define("miniSlimeBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue BEE_BUFF_ENABLED = BUILDER.define("beeBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue HORSE_BUFF_ENABLED = BUILDER.define("horseBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue TURTLE_BUFF_ENABLED = BUILDER.define("turtleBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue AXOLOTL_BUFF_ENABLED = BUILDER.define("axolotlBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue BAT_BUFF_ENABLED = BUILDER.define("batBuffEnabled", true);
    public static final ModConfigSpec.BooleanValue FOX_BUFF_ENABLED = BUILDER.define("foxBuffEnabled", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTROLLER_CHIP_ENTITY_ALLOWLIST = BUILDER
            .comment("If non-empty, only these entity IDs can receive a Controller Chip.")
            .defineListAllowEmpty("controllerChipEntityAllowlist", List.of(), Config::validateEntityName);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTROLLER_CHIP_ENTITY_DENYLIST = BUILDER
            .comment("Entity IDs that can never receive a Controller Chip.")
            .defineListAllowEmpty("controllerChipEntityDenylist", List.of(), Config::validateEntityName);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTROLLER_CHIP_ROLE_DENYLIST = BUILDER
            .comment("Entity IDs that keep control behavior but never receive combat/support actions.")
            .defineListAllowEmpty("controllerChipRoleDenylist", List.of(), Config::validateEntityName);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTROLLER_CHIP_FORCED_ROLES = BUILDER
            .comment("Forced roles in the form namespace:entity=ROLE. Valid roles: MELEE, RANGED, MAGIC, SUPPORT, HYBRID, NONE.")
            .defineListAllowEmpty("controllerChipForcedRoles", List.of(), Config::validateForcedRole);
    public static final ModConfigSpec.BooleanValue ADVANCED_CONTROLLER_CHIP_ENABLED = BUILDER.define("advancedControllerChipEnabled", true);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_ALLOW_BOSSES = BUILDER.define("advancedChipAllowBosses", true);
    public static final ModConfigSpec.DoubleValue ADVANCED_CHIP_BOSS_HEALTH_THRESHOLD = BUILDER.defineInRange("advancedChipBossHealthThreshold", 0.25D, 0.01D, 1.0D);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_CREATIVE_BYPASSES_HEALTH_REQUIREMENT = BUILDER.define("advancedChipCreativeBypassesHealthRequirement", true);
    public static final ModConfigSpec.IntValue ADVANCED_CHIP_MAX_BOSSES_PER_PLAYER = BUILDER.defineInRange("advancedChipMaxBossesPerPlayer", 1, 0, 16);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_ALLOW_NORMAL_MOBS = BUILDER.define("advancedChipAllowNormalMobs", true);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_ALLOW_MODDED_BOSSES = BUILDER.define("advancedChipAllowModdedBosses", true);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_ALLOW_ENDER_DRAGON = BUILDER.define("advancedChipAllowEnderDragon", true);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_ALLOW_WITHER = BUILDER.define("advancedChipAllowWither", true);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_ALLOW_WARDEN = BUILDER.define("advancedChipAllowWarden", true);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_ALLOW_ELDER_GUARDIAN = BUILDER.define("advancedChipAllowElderGuardian", true);
    public static final ModConfigSpec.BooleanValue CONTROLLED_WITHER_BLOCK_GRIEFING = BUILDER.define("controlledWitherBlockGriefing", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_WITHER_FRIENDLY_FIRE = BUILDER.define("controlledWitherFriendlyFire", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_WITHER_DAMAGES_ALLIES = BUILDER.define("controlledWitherDamagesAllies", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_DRAGON_BLOCK_GRIEFING = BUILDER.define("controlledDragonBlockGriefing", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_DRAGON_DAMAGES_ALLIES = BUILDER.define("controlledDragonDamagesAllies", false);
    public static final ModConfigSpec.DoubleValue CONTROLLED_DRAGON_FOLLOW_DISTANCE = BUILDER.defineInRange("controlledDragonFollowDistance", 24.0D, 8.0D, 128.0D);
    public static final ModConfigSpec.DoubleValue CONTROLLED_DRAGON_ATTACK_RADIUS = BUILDER.defineInRange("controlledDragonAttackRadius", 64.0D, 16.0D, 256.0D);
    public static final ModConfigSpec.BooleanValue CONTROLLED_BOSS_CROSS_DIMENSION_TELEPORT = BUILDER.define("controlledBossCrossDimensionTeleport", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_BOSS_FRIENDLY_FIRE = BUILDER.define("controlledBossFriendlyFire", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_BOSS_BLOCK_GRIEFING = BUILDER.define("controlledBossBlockGriefing", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_BOSS_PROTECT_OWNER_PETS = BUILDER.define("controlledBossProtectOwnerPets", true);
    public static final ModConfigSpec.BooleanValue CONTROLLED_BOSS_PROTECT_TEAM_MEMBERS = BUILDER.define("controlledBossProtectTeamMembers", true);
    public static final ModConfigSpec.BooleanValue ADVANCED_CHIP_DROPS_ON_BOSS_DEATH = BUILDER.define("advancedChipDropsOnBossDeath", false);
    public static final ModConfigSpec.BooleanValue CONTROLLED_BOSS_KEEPS_NORMAL_LOOT_ON_DEATH = BUILDER.define("controlledBossKeepsNormalLootOnDeath", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADVANCED_CONTROLLER_BOSS_ALLOWLIST = BUILDER
            .comment("Full entity IDs that are treated as advanced-controller bosses.")
            .defineListAllowEmpty("advancedControllerBossAllowlist", List.of(), Config::validateEntityName);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADVANCED_CONTROLLER_BOSS_DENYLIST = BUILDER
            .comment("Full entity IDs that advanced controller chips can never control.")
            .defineListAllowEmpty("advancedControllerBossDenylist", List.of(), Config::validateEntityName);

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
    public static boolean controllerChipDebug;
    public static boolean controllerChipAllowCreeperExplosion;
    public static boolean controllerChipAllowVillagers;
    public static int controllerChipThreatMemoryTicks;
    public static int controlledTargetCommandDuration;
    public static double controlledTargetMaximumDistance;
    public static boolean enablePassiveMobBuffs;
    public static double passiveBuffRadius;
    public static int passiveBuffRefreshInterval;
    public static boolean allowBuffStacking;
    public static int maxSupportMobsAffectingPlayer;
    public static boolean chickenBuffEnabled;
    public static boolean pigBuffEnabled;
    public static boolean cowBuffEnabled;
    public static boolean sheepBuffEnabled;
    public static boolean rabbitBuffEnabled;
    public static boolean miniSlimeBuffEnabled;
    public static boolean beeBuffEnabled;
    public static boolean horseBuffEnabled;
    public static boolean turtleBuffEnabled;
    public static boolean axolotlBuffEnabled;
    public static boolean batBuffEnabled;
    public static boolean foxBuffEnabled;
    public static Set<ResourceLocation> controllerChipEntityAllowlist = Set.of();
    public static Set<ResourceLocation> controllerChipEntityDenylist = Set.of();
    public static Set<ResourceLocation> controllerChipRoleDenylist = Set.of();
    public static Map<ResourceLocation, Infinitygroup.microtech.entity.control.ControlledMobCombatRole> controllerChipForcedRoles = Map.of();
    public static boolean advancedControllerChipEnabled;
    public static boolean advancedChipAllowBosses;
    public static double advancedChipBossHealthThreshold;
    public static boolean advancedChipCreativeBypassesHealthRequirement;
    public static int advancedChipMaxBossesPerPlayer;
    public static boolean advancedChipAllowNormalMobs;
    public static boolean advancedChipAllowModdedBosses;
    public static boolean advancedChipAllowEnderDragon;
    public static boolean advancedChipAllowWither;
    public static boolean advancedChipAllowWarden;
    public static boolean advancedChipAllowElderGuardian;
    public static boolean controlledWitherBlockGriefing;
    public static boolean controlledWitherFriendlyFire;
    public static boolean controlledWitherDamagesAllies;
    public static boolean controlledDragonBlockGriefing;
    public static boolean controlledDragonDamagesAllies;
    public static double controlledDragonFollowDistance;
    public static double controlledDragonAttackRadius;
    public static boolean controlledBossCrossDimensionTeleport;
    public static boolean controlledBossFriendlyFire;
    public static boolean controlledBossBlockGriefing;
    public static boolean controlledBossProtectOwnerPets;
    public static boolean controlledBossProtectTeamMembers;
    public static boolean advancedChipDropsOnBossDeath;
    public static boolean controlledBossKeepsNormalLootOnDeath;
    public static Set<ResourceLocation> advancedControllerBossAllowlist = Set.of();
    public static Set<ResourceLocation> advancedControllerBossDenylist = Set.of();
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

    private static boolean validateForcedRole(final Object obj) {
        if (!(obj instanceof String value)) {
            return false;
        }
        String[] parts = value.split("=", 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            ResourceLocation id = ResourceLocation.parse(parts[0]);
            Infinitygroup.microtech.entity.control.ControlledMobCombatRole.valueOf(parts[1].trim().toUpperCase(java.util.Locale.ROOT));
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
        controllerChipDebug = CONTROLLER_CHIP_DEBUG.get();
        controllerChipAllowCreeperExplosion = CONTROLLER_CHIP_ALLOW_CREEPER_EXPLOSION.get();
        controllerChipAllowVillagers = CONTROLLER_CHIP_ALLOW_VILLAGERS.get();
        controllerChipThreatMemoryTicks = CONTROLLER_CHIP_THREAT_MEMORY_TICKS.get();
        controlledTargetCommandDuration = CONTROLLED_TARGET_COMMAND_DURATION.get();
        controlledTargetMaximumDistance = CONTROLLED_TARGET_MAXIMUM_DISTANCE.get();
        enablePassiveMobBuffs = ENABLE_PASSIVE_MOB_BUFFS.get();
        passiveBuffRadius = PASSIVE_BUFF_RADIUS.get();
        passiveBuffRefreshInterval = PASSIVE_BUFF_REFRESH_INTERVAL.get();
        allowBuffStacking = ALLOW_BUFF_STACKING.get();
        maxSupportMobsAffectingPlayer = MAX_SUPPORT_MOBS_AFFECTING_PLAYER.get();
        chickenBuffEnabled = CHICKEN_BUFF_ENABLED.get();
        pigBuffEnabled = PIG_BUFF_ENABLED.get();
        cowBuffEnabled = COW_BUFF_ENABLED.get();
        sheepBuffEnabled = SHEEP_BUFF_ENABLED.get();
        rabbitBuffEnabled = RABBIT_BUFF_ENABLED.get();
        miniSlimeBuffEnabled = MINI_SLIME_BUFF_ENABLED.get();
        beeBuffEnabled = BEE_BUFF_ENABLED.get();
        horseBuffEnabled = HORSE_BUFF_ENABLED.get();
        turtleBuffEnabled = TURTLE_BUFF_ENABLED.get();
        axolotlBuffEnabled = AXOLOTL_BUFF_ENABLED.get();
        batBuffEnabled = BAT_BUFF_ENABLED.get();
        foxBuffEnabled = FOX_BUFF_ENABLED.get();
        controllerChipEntityAllowlist = parseEntitySet(CONTROLLER_CHIP_ENTITY_ALLOWLIST.get());
        controllerChipEntityDenylist = parseEntitySet(CONTROLLER_CHIP_ENTITY_DENYLIST.get());
        controllerChipRoleDenylist = parseEntitySet(CONTROLLER_CHIP_ROLE_DENYLIST.get());
        controllerChipForcedRoles = parseForcedRoles(CONTROLLER_CHIP_FORCED_ROLES.get());
        advancedControllerChipEnabled = ADVANCED_CONTROLLER_CHIP_ENABLED.get();
        advancedChipAllowBosses = ADVANCED_CHIP_ALLOW_BOSSES.get();
        advancedChipBossHealthThreshold = ADVANCED_CHIP_BOSS_HEALTH_THRESHOLD.get();
        advancedChipCreativeBypassesHealthRequirement = ADVANCED_CHIP_CREATIVE_BYPASSES_HEALTH_REQUIREMENT.get();
        advancedChipMaxBossesPerPlayer = ADVANCED_CHIP_MAX_BOSSES_PER_PLAYER.get();
        advancedChipAllowNormalMobs = ADVANCED_CHIP_ALLOW_NORMAL_MOBS.get();
        advancedChipAllowModdedBosses = ADVANCED_CHIP_ALLOW_MODDED_BOSSES.get();
        advancedChipAllowEnderDragon = ADVANCED_CHIP_ALLOW_ENDER_DRAGON.get();
        advancedChipAllowWither = ADVANCED_CHIP_ALLOW_WITHER.get();
        advancedChipAllowWarden = ADVANCED_CHIP_ALLOW_WARDEN.get();
        advancedChipAllowElderGuardian = ADVANCED_CHIP_ALLOW_ELDER_GUARDIAN.get();
        controlledWitherBlockGriefing = CONTROLLED_WITHER_BLOCK_GRIEFING.get();
        controlledWitherFriendlyFire = CONTROLLED_WITHER_FRIENDLY_FIRE.get();
        controlledWitherDamagesAllies = CONTROLLED_WITHER_DAMAGES_ALLIES.get();
        controlledDragonBlockGriefing = CONTROLLED_DRAGON_BLOCK_GRIEFING.get();
        controlledDragonDamagesAllies = CONTROLLED_DRAGON_DAMAGES_ALLIES.get();
        controlledDragonFollowDistance = CONTROLLED_DRAGON_FOLLOW_DISTANCE.get();
        controlledDragonAttackRadius = CONTROLLED_DRAGON_ATTACK_RADIUS.get();
        controlledBossCrossDimensionTeleport = CONTROLLED_BOSS_CROSS_DIMENSION_TELEPORT.get();
        controlledBossFriendlyFire = CONTROLLED_BOSS_FRIENDLY_FIRE.get();
        controlledBossBlockGriefing = CONTROLLED_BOSS_BLOCK_GRIEFING.get();
        controlledBossProtectOwnerPets = CONTROLLED_BOSS_PROTECT_OWNER_PETS.get();
        controlledBossProtectTeamMembers = CONTROLLED_BOSS_PROTECT_TEAM_MEMBERS.get();
        advancedChipDropsOnBossDeath = ADVANCED_CHIP_DROPS_ON_BOSS_DEATH.get();
        controlledBossKeepsNormalLootOnDeath = CONTROLLED_BOSS_KEEPS_NORMAL_LOOT_ON_DEATH.get();
        advancedControllerBossAllowlist = parseEntitySet(ADVANCED_CONTROLLER_BOSS_ALLOWLIST.get());
        advancedControllerBossDenylist = parseEntitySet(ADVANCED_CONTROLLER_BOSS_DENYLIST.get());
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

    private static Map<ResourceLocation, Infinitygroup.microtech.entity.control.ControlledMobCombatRole> parseForcedRoles(List<? extends String> values) {
        Map<ResourceLocation, Infinitygroup.microtech.entity.control.ControlledMobCombatRole> parsed = new HashMap<>();
        for (String value : values) {
            String[] parts = value.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                parsed.put(ResourceLocation.parse(parts[0]), Infinitygroup.microtech.entity.control.ControlledMobCombatRole.valueOf(parts[1].trim().toUpperCase(java.util.Locale.ROOT)));
            } catch (Exception ignored) {
            }
        }
        return parsed;
    }
}
