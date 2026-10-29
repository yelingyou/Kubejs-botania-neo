package dev.kubejsbotania.neo.kubejs.schemas;

import dev.kubejsbotania.neo.kubejs.components.BotaniaComponents;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.commands.CacheableFunction;
import vazkii.botania.api.recipe.StateIngredient;

/**
 * Schema for {@code botania:pure_daisy} ({@code PureDaisyRecipe}).
 *
 * <p>Botania 1.21.1 codec ({@code PureDaisyRecipe.Serializer.CODEC}):</p>
 * <ul>
 *     <li>{@code input} - {@code StateIngredients.TYPED_CODEC}, required</li>
 *     <li>{@code output} - {@code StateIngredients.TYPED_CODEC}, required
 *     (in 1.20.1 this was a plain block state; in 1.21.1 it is a full StateIngredient, so
 *     {@code botania:block}, {@code botania:state} and {@code botania:tag} outputs all work)</li>
 *     <li>{@code time} - {@code ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 0)}, optional</li>
 *     <li>{@code copy_properties} - {@code Codec.BOOL.optionalFieldOf("copy_properties", false)}, optional</li>
 *     <li>{@code pre_update_function} - {@code CacheableFunction.CODEC} optional, no default</li>
 *     <li>{@code success_function} - {@code CacheableFunction.CODEC} optional, no default</li>
 * </ul>
 *
 * <p><b>Note:</b> the raw codec default for {@code time} is {@code 0} in Botania 1.21.1, but every
 * recipe shipped by Botania uses {@code 150} ({@link PureDaisyRecipe#DEFAULT_TIME}, also the default
 * used by the original KubeJS-Botania 1.20.1 schema). This schema therefore defaults to
 * {@code 150} and always writes it, so an omitted argument behaves like Botania's own recipes.</p>
 *
 * <p>Real JSON:</p>
 * <pre>{@code
 * {
 *   "type": "botania:pure_daisy",
 *   "input":  {"type": "botania:block", "block": "minecraft:netherrack"},
 *   "output": {"type": "botania:state", "state": {"Name": "minecraft:cobblestone"}},
 *   "time": 150
 * }
 * }</pre>
 */
public interface PureDaisySchema {
	RecipeKey<StateIngredient> OUTPUT = BotaniaSchemaComponents.STATE_INGREDIENT.outputKey("output");
	RecipeKey<StateIngredient> INPUT = BotaniaSchemaComponents.STATE_INGREDIENT.inputKey("input");
	RecipeKey<Integer> TIME = NumberComponent.intRange(0, Integer.MAX_VALUE).otherKey("time").optional(150).alwaysWrite();
	RecipeKey<Boolean> COPY_PROPERTIES = BooleanComponent.BOOLEAN.otherKey("copy_properties").optional(false);
	RecipeKey<CacheableFunction> PRE_UPDATE_FUNCTION = BotaniaComponents.CACHEABLE_FUNCTION
			.otherKey("pre_update_function")
			.defaultOptional();
	RecipeKey<CacheableFunction> SUCCESS_FUNCTION = BotaniaComponents.CACHEABLE_FUNCTION
			.otherKey("success_function")
			.defaultOptional();

	RecipeSchema SCHEMA = new RecipeSchema(OUTPUT, INPUT, TIME, COPY_PROPERTIES, PRE_UPDATE_FUNCTION, SUCCESS_FUNCTION);
}
