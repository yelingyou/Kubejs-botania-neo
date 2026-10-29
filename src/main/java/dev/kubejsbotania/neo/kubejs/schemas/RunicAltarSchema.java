package dev.kubejsbotania.neo.kubejs.schemas;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.IntBounds;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * Schema for {@code botania:runic_altar} ({@code RunicAltarRecipe}).
 *
 * <p>Botania 1.21.1 codec ({@code RunicAltarRecipe.Serializer.CODEC}):</p>
 * <ul>
 *     <li>{@code ingredients} - {@code Ingredient.CODEC_NONEMPTY.listOf()}, required field, may be empty</li>
 *     <li>{@code catalysts} - {@code Ingredient.CODEC_NONEMPTY.listOf()}, required field, may be empty.
 *     The codec has no default for it, but Botania's own JSON always contains {@code "catalysts": []},
 *     so this schema exposes it as an optional value that defaults to an empty list and is always
 *     written. That keeps the JSON technically identical to Botania's and still allows the short
 *     4-argument script constructor.</li>
 *     <li>{@code reagent} - {@code Ingredient.CODEC_NONEMPTY}, required (1.21.1 has no default reagent;
 *     Botania's own recipes use {@code botania:livingrock})</li>
 *     <li>{@code mana} - {@code ExtraCodecs.POSITIVE_INT}, required</li>
 *     <li>{@code output} - {@code ItemStack.STRICT_CODEC}, required</li>
 * </ul>
 *
 * <p>Botania additionally validates that {@code ingredients.size() + catalysts.size()} is between 1
 * and 16 and that ingredients/catalysts/reagent do not overlap; those checks happen at datapack load
 * and are intentionally not duplicated by KubeJS.</p>
 *
 * <p>Real JSON:</p>
 * <pre>{@code
 * {
 *   "type": "botania:runic_altar",
 *   "catalysts": [],
 *   "ingredients": [{"tag": "c:dusts/mana"}, {"item": "minecraft:stone"}],
 *   "mana": 5200,
 *   "output": {"count": 2, "id": "botania:rune_of_earth"},
 *   "reagent": {"item": "botania:livingrock"}
 * }
 * }</pre>
 */
public interface RunicAltarSchema {
	RecipeKey<ItemStack> OUTPUT = BotaniaSchemaComponents.ITEM_STACK.outputKey("output");
	RecipeKey<List<Ingredient>> INGREDIENTS = BotaniaSchemaComponents.INGREDIENT_LIST
			.withBounds(IntBounds.of(0, 16))
			.inputKey("ingredients");
	RecipeKey<Integer> MANA = NumberComponent.POSITIVE_INT.otherKey("mana");
	RecipeKey<Ingredient> REAGENT = BotaniaSchemaComponents.INGREDIENT.inputKey("reagent");
	RecipeKey<List<Ingredient>> CATALYSTS = BotaniaSchemaComponents.INGREDIENT_LIST
			.withBounds(IntBounds.of(0, 16))
			.inputKey("catalysts")
			.optional(List.of())
			.alwaysWrite();

	RecipeSchema SCHEMA = new RecipeSchema(OUTPUT, INGREDIENTS, MANA, REAGENT, CATALYSTS);
}
