package dev.kubejsbotania.neo.kubejs.schemas;

import dev.kubejsbotania.neo.kubejs.component.FlexibleIngredientComponent;
import dev.kubejsbotania.neo.kubejs.component.FlexibleIngredientListComponent;
import dev.kubejsbotania.neo.kubejs.component.FlexibleItemStackComponent;
import dev.kubejsbotania.neo.kubejs.component.FlexibleItemStackListComponent;
import dev.kubejsbotania.neo.kubejs.components.BotaniaComponents;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import vazkii.botania.api.recipe.StateIngredient;

import java.util.List;

/**
 * Reusable recipe components for the Botania schemas with script-friendly codecs.
 *
 * <p>KubeJS 2101 parses the single-object form of a recipe call by converting the JS object to JSON and then
 * running each recipe key's codec over it, but by default it does <b>not</b> re-encode the decoded values back
 * into the recipe JSON. The vanilla item/ingredient codecs and Botania's StateIngredient codec only accept JSON
 * objects, so plain script strings like {@code 'minecraft:diamond'} or {@code '#minecraft:logs'} would either
 * fail to parse or be written to the recipe JSON verbatim (and then rejected by the recipe serializers).</p>
 *
 * <p>These components combine the flexible codecs from {@code KubeJSCodecs} with a {@code readFromJson}
 * override that marks the value for re-serialisation, so object form writes canonical JSON while positional
 * form keeps working through the JS value wrappers. Both usages work:</p>
 *
 * <pre>{@code
 * // object form
 * event.recipes.botania.mana_infusion({ output: 'minecraft:gold_ingot', input: 'minecraft:iron_ingot', mana: 5000 })
 * // positional form
 * event.recipes.botania.mana_infusion('minecraft:gold_ingot', 'minecraft:iron_ingot', 5000)
 * }</pre>
 *
 * @author KubeJS Botania Neo
 */
public interface BotaniaSchemaComponents {
	RecipeComponent<ItemStack> ITEM_STACK = new FlexibleItemStackComponent(ItemStackComponent.ITEM_STACK);

	FlexibleItemStackListComponent ITEM_STACK_LIST = FlexibleItemStackListComponent.INSTANCE;

	RecipeComponent<Ingredient> INGREDIENT = new FlexibleIngredientComponent(IngredientComponent.INGREDIENT);

	FlexibleIngredientListComponent INGREDIENT_LIST = FlexibleIngredientListComponent.INSTANCE;

	RecipeComponent<StateIngredient> STATE_INGREDIENT = BotaniaComponents.STATE_INGREDIENT.instance();
}
