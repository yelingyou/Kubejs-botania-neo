package dev.kubejsbotania.neo.kubejs.schemas;

import dev.kubejsbotania.neo.kubejs.components.BotaniaComponents;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import vazkii.botania.api.recipe.StateIngredient;
import vazkii.botania.common.block.mana.ManaPoolBlock;
import vazkii.botania.common.crafting.StateIngredients;

/**
 * Schema for {@code botania:mana_infusion} ({@code ManaInfusionRecipe}).
 *
 * <p>Botania 1.21.1 codec ({@code ManaInfusionRecipe.Serializer.CODEC}):</p>
 * <ul>
 *     <li>{@code output} - {@code ItemStack.STRICT_CODEC}, required</li>
 *     <li>{@code input} - {@code Ingredient.CODEC_NONEMPTY}, required</li>
 *     <li>{@code mana} - {@code ExtraCodecs.intRange(1, ManaPoolBlock.MAX_MANA + 1)}, required</li>
 *     <li>{@code group} - {@code Codec.STRING.optionalFieldOf("group", "")}, optional</li>
 *     <li>{@code catalyst} - {@code StateIngredients.TYPED_CODEC.optionalFieldOf("catalyst", StateIngredients.NONE)}, optional</li>
 * </ul>
 *
 * <p>Real JSON (with catalyst/group):</p>
 * <pre>{@code
 * {
 *   "type": "botania:mana_infusion",
 *   "catalyst": {"type": "botania:block", "block": "botania:alchemy_catalyst"},
 *   "group": "botania:flower_shrinking",
 *   "input": {"item": "botania:agricarnation"},
 *   "mana": 2500,
 *   "output": {"count": 1, "id": "botania:agricarnation_petite"}
 * }
 * }</pre>
 */
public interface ManaInfusionSchema {
	RecipeKey<ItemStack> OUTPUT = BotaniaSchemaComponents.ITEM_STACK.outputKey("output");
	RecipeKey<Ingredient> INPUT = BotaniaSchemaComponents.INGREDIENT.inputKey("input");
	RecipeKey<Integer> MANA = NumberComponent.intRange(1, ManaPoolBlock.MAX_MANA + 1).otherKey("mana");
	RecipeKey<StateIngredient> CATALYST = BotaniaSchemaComponents.STATE_INGREDIENT
			.otherKey("catalyst")
			.optional(StateIngredients.NONE);
	RecipeKey<String> GROUP = StringComponent.OPTIONAL_STRING.otherKey("group").optional("");

	RecipeSchema SCHEMA = new RecipeSchema(OUTPUT, INPUT, MANA, CATALYST, GROUP);
}
