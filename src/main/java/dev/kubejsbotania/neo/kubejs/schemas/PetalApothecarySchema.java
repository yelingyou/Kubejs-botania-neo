package dev.kubejsbotania.neo.kubejs.schemas;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.IntBounds;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * Schema for {@code botania:petal_apothecary} ({@code PetalApothecaryRecipe}).
 *
 * <p>Botania 1.21.1 codec ({@code PetalApothecaryRecipe.Serializer.CODEC}):</p>
 * <ul>
 *     <li>{@code output} - {@code ItemStack.STRICT_CODEC}, required</li>
 *     <li>{@code reagent} - {@code Ingredient.CODEC_NONEMPTY}, required.
 *     In 1.21.1 the field is required (all shipped recipes use
 *     {@code {"tag": "botania:seed_apothecary_reagent"}}); the 1.20.1 KubeJS-Botania schema used to
 *     default it, so scripts have to pass it explicitly now.</li>
 *     <li>{@code ingredients} - {@code Ingredient.CODEC_NONEMPTY.listOf(1, 16)}, required</li>
 * </ul>
 *
 * <p>Real JSON:</p>
 * <pre>{@code
 * {
 *   "type": "botania:petal_apothecary",
 *   "ingredients": [{"tag": "botania:petals/light_gray"}, {"item": "botania:rune_of_earth"}],
 *   "output": {"count": 1, "id": "botania:clayconia"},
 *   "reagent": {"tag": "botania:seed_apothecary_reagent"}
 * }
 * }</pre>
 */
public interface PetalApothecarySchema {
	RecipeKey<ItemStack> OUTPUT = BotaniaSchemaComponents.ITEM_STACK.outputKey("output");
	RecipeKey<List<Ingredient>> INGREDIENTS = BotaniaSchemaComponents.INGREDIENT_LIST
			.withBounds(IntBounds.of(1, 16))
			.inputKey("ingredients");
	RecipeKey<Ingredient> REAGENT = BotaniaSchemaComponents.INGREDIENT.inputKey("reagent");

	RecipeSchema SCHEMA = new RecipeSchema(OUTPUT, INGREDIENTS, REAGENT);
}
