package dev.kubejsbotania.neo.kubejs;

import dev.kubejsbotania.neo.KubeJSBotaniaNeo;
import dev.kubejsbotania.neo.kubejs.component.FlexibleIngredientComponent;
import dev.kubejsbotania.neo.kubejs.component.FlexibleIngredientListComponent;
import dev.kubejsbotania.neo.kubejs.component.FlexibleItemStackComponent;
import dev.kubejsbotania.neo.kubejs.component.TerraPlatePositionComponent;
import dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe;
import dev.kubejsbotania.neo.recipe.TerraPlatePosition;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * KubeJS recipe schema for the extended Terra Plate recipe type.
 *
 * <p>Script usage (object form, compatible with the original terra-plate-extension mod):</p>
 * <pre>{@code
 * ServerEvents.recipes(event => {
 *     event.recipes.kubejs_botania_neo.terra_plate({
 *         ingredients: ['minecraft:diamond', 'minecraft:diamond', 'minecraft:emerald'],
 *         mana: 50000,
 *         output: { item: 'minecraft:netherite_ingot' },
 *         below: { positions: [
 *             { x: -1, z: -1, block: 'minecraft:stone' },
 *             { x: 0, z: 0, fluid: 'minecraft:water' }
 *         ] },
 *         result_blocks: [
 *             { x: 0, z: 0, block: 'minecraft:gold_block' }
 *         ]
 *     });
 * });
 * }</pre>
 *
 * <p>The ingredient/output components use script-friendly codecs and force a re-encode when read from
 * object-style JSON, so plain strings like {@code 'minecraft:diamond'} are accepted and written to the recipe
 * JSON in the canonical form. The positional form
 * ({@code event.recipes.kubejs_botania_neo.terra_plate(output, ingredients, mana, below, resultBlocks)})
 * keeps working as well.</p>
 *
 * <p>Keys:</p>
 * <ul>
 *     <li>{@code ingredients} - required, list of ingredients</li>
 *     <li>{@code output} - required, result item stack</li>
 *     <li>{@code mana} - optional, integer, defaults to 500000</li>
 *     <li>{@code below} - optional, list of positions or {@code {positions:[...]}}, relative x/z (-1..1 usually),
 *     one block below the plate. Each position is a block id, a {@code #tag}, or a fluid id.</li>
 *     <li>{@code result_blocks} - optional, list of positions that are replaced after a successful craft.</li>
 * </ul>
 *
 * @author KubeJS Botania Neo
 */
public interface TerraPlateExtensionSchema {
	RecipeComponent<ItemStack> OUTPUT_COMPONENT = new FlexibleItemStackComponent(ItemStackComponent.ITEM_STACK);
	RecipeComponent<Ingredient> INGREDIENT_COMPONENT = new FlexibleIngredientComponent(IngredientComponent.INGREDIENT);
	RecipeComponent<List<Ingredient>> INGREDIENT_LIST_COMPONENT = FlexibleIngredientListComponent.INSTANCE;

	RecipeKey<ItemStack> OUTPUT = OUTPUT_COMPONENT.outputKey("output");
	RecipeKey<List<Ingredient>> INGREDIENTS = INGREDIENT_LIST_COMPONENT.inputKey("ingredients");
	RecipeKey<Integer> MANA = NumberComponent.INT.otherKey("mana").optional(TerraPlateExtensionRecipe.DEFAULT_MANA);
	RecipeKey<List<TerraPlatePosition>> BELOW = TerraPlatePositionComponent.INSTANCE.otherKey("below").optional(List.of());
	RecipeKey<List<TerraPlatePosition>> RESULT_BLOCKS = TerraPlatePositionComponent.INSTANCE.otherKey("result_blocks").optional(List.of());

	RecipeSchema SCHEMA = new RecipeSchema(OUTPUT, INGREDIENTS, MANA, BELOW, RESULT_BLOCKS);

	static void register(RecipeSchemaRegistry registry) {
		// The JSON type stays kubejs_botania_neo:terra_plate (serializer registered by ModRecipes),
		// while the recipe instance itself is indexed under Botania's terra plate recipe type.
		registry.namespace(KubeJSBotaniaNeo.MOD_ID).register("terra_plate", SCHEMA);
	}
}
