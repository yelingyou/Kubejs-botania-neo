package dev.kubejsbotania.neo.kubejs.schemas;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * Schema for {@code botania:elven_trade} ({@code ElvenTradeRecipe}).
 *
 * <p>Botania 1.21.1 codec ({@code ElvenTradeRecipe.Serializer.CODEC}):</p>
 * <ul>
 *     <li>{@code output} - {@code ExtraCodecs.nonEmptyList(ITEM_STACK_COMPACT_CODEC.listOf())}, required.
 *     This is a <b>list</b> of item stacks; a count-1 stack may be written compactly but KubeJS always
 *     writes the regular strict item stack object, which Botania accepts.</li>
 *     <li>{@code ingredients} - {@code ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf())}, required</li>
 * </ul>
 *
 * <p>Real JSON:</p>
 * <pre>{@code
 * {
 *   "type": "botania:elven_trade",
 *   "ingredients": [{"item": "botania:livingwood"}],
 *   "output": [{"id": "botania:dreamwood"}]
 * }
 * }</pre>
 */
public interface ElvenTradeSchema {
	RecipeKey<List<ItemStack>> OUTPUT = BotaniaSchemaComponents.ITEM_STACK_LIST.outputKey("output");
	RecipeKey<List<Ingredient>> INGREDIENTS = BotaniaSchemaComponents.INGREDIENT_LIST.inputKey("ingredients");

	RecipeSchema SCHEMA = new RecipeSchema(OUTPUT, INGREDIENTS);
}
