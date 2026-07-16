package Infinitygroup.microtech.item;

import Infinitygroup.microtech.Microtech;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TechCrusherRecipeHelper {
    private static final Map<Item, CrusherRecipeDefinition> RECIPES = createRecipes();

    private TechCrusherRecipeHelper() {
    }

    public static Optional<CrusherRecipe> getRecipe(ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }

        CrusherRecipeDefinition definition = RECIPES.get(input.getItem());
        if (definition == null) {
            return Optional.empty();
        }

        return Optional.of(new CrusherRecipe(definition.id(), definition.outputSupplier().get()));
    }

    public static boolean isValidInput(ItemStack stack) {
        return getRecipe(stack).isPresent();
    }

    public static Optional<ItemStack> getOutput(ItemStack input) {
        return getRecipe(input).map(CrusherRecipe::output);
    }

    private static Map<Item, CrusherRecipeDefinition> createRecipes() {
        Map<Item, CrusherRecipeDefinition> map = new LinkedHashMap<>();

        register(map, "raw_iron_to_iron_dust", Items.RAW_IRON, () -> new ItemStack(Microtech.IRON_DUST.get()));
        register(map, "raw_copper_to_copper_dust", Items.RAW_COPPER, () -> new ItemStack(Microtech.COPPER_DUST.get()));
        register(map, "raw_gold_to_gold_dust", Items.RAW_GOLD, () -> new ItemStack(Microtech.GOLD_DUST.get()));

        register(map, "iron_ore_to_iron_dust", Items.IRON_ORE, () -> new ItemStack(Microtech.IRON_DUST.get(), 2));
        register(map, "deepslate_iron_ore_to_iron_dust", Items.DEEPSLATE_IRON_ORE, () -> new ItemStack(Microtech.IRON_DUST.get(), 2));
        register(map, "copper_ore_to_copper_dust", Items.COPPER_ORE, () -> new ItemStack(Microtech.COPPER_DUST.get(), 2));
        register(map, "deepslate_copper_ore_to_copper_dust", Items.DEEPSLATE_COPPER_ORE, () -> new ItemStack(Microtech.COPPER_DUST.get(), 2));
        register(map, "gold_ore_to_gold_dust", Items.GOLD_ORE, () -> new ItemStack(Microtech.GOLD_DUST.get(), 2));
        register(map, "deepslate_gold_ore_to_gold_dust", Items.DEEPSLATE_GOLD_ORE, () -> new ItemStack(Microtech.GOLD_DUST.get(), 2));

        register(map, "coal_ore_to_coal_dust", Items.COAL_ORE, () -> new ItemStack(Microtech.COAL_DUST.get(), 2));
        register(map, "deepslate_coal_ore_to_coal_dust", Items.DEEPSLATE_COAL_ORE, () -> new ItemStack(Microtech.COAL_DUST.get(), 2));

        register(map, "redstone_ore_to_redstone", Items.REDSTONE_ORE, () -> new ItemStack(Items.REDSTONE, 4));
        register(map, "deepslate_redstone_ore_to_redstone", Items.DEEPSLATE_REDSTONE_ORE, () -> new ItemStack(Items.REDSTONE, 4));

        register(map, "lapis_ore_to_lapis_dust", Items.LAPIS_ORE, () -> new ItemStack(Microtech.LAPIS_DUST.get(), 4));
        register(map, "deepslate_lapis_ore_to_lapis_dust", Items.DEEPSLATE_LAPIS_ORE, () -> new ItemStack(Microtech.LAPIS_DUST.get(), 4));

        register(map, "diamond_ore_to_diamond_dust", Items.DIAMOND_ORE, () -> new ItemStack(Microtech.DIAMOND_DUST.get()));
        register(map, "deepslate_diamond_ore_to_diamond_dust", Items.DEEPSLATE_DIAMOND_ORE, () -> new ItemStack(Microtech.DIAMOND_DUST.get()));

        register(map, "emerald_ore_to_emerald_dust", Items.EMERALD_ORE, () -> new ItemStack(Microtech.EMERALD_DUST.get()));
        register(map, "deepslate_emerald_ore_to_emerald_dust", Items.DEEPSLATE_EMERALD_ORE, () -> new ItemStack(Microtech.EMERALD_DUST.get()));

        register(map, "ancient_debris_to_netherite_dust", Items.ANCIENT_DEBRIS, () -> new ItemStack(Microtech.NETHERITE_DUST.get()));

        return Map.copyOf(map);
    }

    private static void register(Map<Item, CrusherRecipeDefinition> map, String id, Item input, Supplier<ItemStack> outputSupplier) {
        map.put(input, new CrusherRecipeDefinition(id, outputSupplier));
    }

    public record CrusherRecipe(String id, ItemStack output) {
    }

    private record CrusherRecipeDefinition(String id, Supplier<ItemStack> outputSupplier) {
    }
}
