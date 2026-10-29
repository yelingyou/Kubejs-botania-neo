package dev.kubejsbotania.neo.kubejs.schemas;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * Schema for {@code botania:terrestrial_agglomeration_plate} (the Terrestrial Agglomeration Plate /
 * Terra Plate recipe).
 *
 * <p><b>Renamed in 1.21.1:</b> the recipe type used to be {@code botania:terra_plate} in 1.20.1,
 * but Botania 1.21.1 registers it as {@code botania:terrestrial_agglomeration_plate}
 * ({@code vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe.TYPE_ID}). The real id is what
 * datapack JSONs contain, so {@link #SCHEMA} is registered under the real id;
 * {@link #ALIAS_SCHEMA} additionally exposes the friendlier legacy {@code event.recipes.botania.terra_plate}
 * name and uses {@link RecipeSchema#typeOverride(ResourceLocation)} so that KubeJS still writes the
 * correct {@code "type"} into generated recipe JSON.</p>
 *
 * <p>Botania 1.21.1 codec ({@code TerrestrialAgglomerationRecipe.Serializer.CODEC}):</p>
 * <ul>
 *     <li>{@code ingredients} - {@code ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf())}, required</li>
 *     <li>{@code mana} - {@code ExtraCodecs.POSITIVE_INT}, required</li>
 *     <li>{@code result} - {@code ItemStack.STRICT_CODEC}, required (field name is {@code result}, not {@code output}!)</li>
 * </ul>
 *
 * <p>Real JSON:</p>
 * <pre>{@code
 * {
 *   "type": "botania:terrestrial_agglomeration_plate",
 *   "ingredients": [{"tag": "c:ingots/manasteel"}, {"tag": "c:gems/mana_pearl"}, {"tag": "c:gems/mana_diamond"}],
 *   "mana": 500000,
 *   "result": {"count": 1, "id": "botania:terrasteel_ingot"}
 * }
 * }</pre>
 */
public interface TerraPlateSchema {
	/**
	 * The real 1.21.1 recipe serializer id.
	 */
	ResourceLocation TYPE_ID = ResourceLocation.fromNamespaceAndPath("botania", "terrestrial_agglomeration_plate");

	RecipeKey<ItemStack> RESULT = BotaniaSchemaComponents.ITEM_STACK.outputKey("result");
	RecipeKey<List<Ingredient>> INGREDIENTS = BotaniaSchemaComponents.INGREDIENT_LIST.inputKey("ingredients");
	RecipeKey<Integer> MANA = NumberComponent.POSITIVE_INT.otherKey("mana");

	/**
	 * Creates a fresh schema instance; the alias needs its own instance because
	 * {@link RecipeSchema#typeOverride(ResourceLocation)} mutates it.
	 */
	static RecipeSchema create() {
		return new RecipeSchema(RESULT, INGREDIENTS, MANA);
	}

	RecipeSchema SCHEMA = create();
	RecipeSchema ALIAS_SCHEMA = create().typeOverride(TYPE_ID);
}
