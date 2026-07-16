package Infinitygroup.microtech.machine;

import Infinitygroup.microtech.Microtech;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TechTableRecipeHelper {
    private static final Map<Item, TechTableRecipe> RECIPES = Map.of(
            Items.IRON_INGOT, new TechTableRecipe(Items.IRON_INGOT, Microtech.IRON_PLATE.get(), 4, 0.20D, 0.18D, 0.08D),
            Items.COPPER_INGOT, new TechTableRecipe(Items.COPPER_INGOT, Microtech.COPPER_PLATE.get(), 4, 0.20D, 0.18D, 0.08D),
            Items.GOLD_INGOT, new TechTableRecipe(Items.GOLD_INGOT, Microtech.GOLD_PLATE.get(), 5, 0.24D, 0.16D, 0.06D)
    );

    private TechTableRecipeHelper() {
    }

    public static Optional<TechTableRecipe> getRecipe(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(RECIPES.get(stack.getItem()));
    }

    public static boolean isValidInput(ItemStack stack) {
        return getRecipe(stack).isPresent();
    }

    public record TechTableRecipe(Item inputItem, Item outputItem, int requiredHits, double cursorSpeed, double goodWindow, double perfectWindow) {
        public ItemStack createOutputStack() {
            return new ItemStack(this.outputItem);
        }

        public Component getInputDisplayName() {
            return Component.translatable(this.inputItem.getDescriptionId());
        }

        public Component getOutputDisplayName() {
            return Component.translatable(this.outputItem.getDescriptionId());
        }
    }
}
