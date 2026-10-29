package dev.kubejsbotania.neo.kubejs.schemas;

import dev.kubejsbotania.neo.kubejs.components.BotaniaComponents;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.TagKeyComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.commands.CacheableFunction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import vazkii.botania.api.recipe.StateIngredient;

/**
 * Shared schema of the three {@code OrechidRecipe} subclasses:
 * {@code botania:orechid}, {@code botania:orechid_ignem} and {@code botania:marimorphosis}.
 *
 * <p>All three use the same codec ({@code OrechidRecipe.Serializer.RAW_CODEC};
 * {@code OrechidIgnemRecipe} and {@code MarimorphosisRecipe} merely {@code xmap} it), so the JSON
 * fields are identical:</p>
 * <ul>
 *     <li>{@code input} - {@code StateIngredients.TYPED_CODEC}, required</li>
 *     <li>{@code output} - {@code StateIngredients.TYPED_CODEC}, required
 *     (both are world block states, not items)</li>
 *     <li>{@code mana} - {@code ExtraCodecs.POSITIVE_INT}, required</li>
 *     <li>{@code cooldown} - {@code ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cooldown", 0)}, optional</li>
 *     <li>{@code weight} - {@code ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("weight", 0)}, optional</li>
 *     <li>{@code biome_bonus_weight} - {@code Codec.INT.optionalFieldOf("biome_bonus_weight", 0)}, optional
 *     (plain {@code INT}, negative values are accepted by the codec)</li>
 *     <li>{@code biome_bonus_tag} - {@code TagKey.codec(Registries.BIOME)}, optional, no default</li>
 *     <li>{@code pre_update_function} / {@code success_function} - {@code CacheableFunction.CODEC}, optional, no default</li>
 * </ul>
 *
 * <p>Note that Botania's own datagen omits {@code cooldown} for {@code marimorphosis} (its
 * constructor hardcodes cooldown {@code 0}); the field is still accepted by the shared codec.</p>
 *
 * <p>Real JSON ({@code botania:orechid} with biome bonus):</p>
 * <pre>{@code
 * {
 *   "type": "botania:orechid",
 *   "biome_bonus_tag": "botania:orechid_stone_copper_bonus",
 *   "biome_bonus_weight": 2500,
 *   "cooldown": 100,
 *   "input": {"type": "botania:block", "block": "minecraft:stone"},
 *   "mana": 17500,
 *   "output": {"type": "botania:block", "block": "minecraft:copper_ore"},
 *   "weight": 2724
 * }
 * }</pre>
 */
public interface OrechidSchema {
	RecipeKey<StateIngredient> OUTPUT = BotaniaSchemaComponents.STATE_INGREDIENT.outputKey("output");
	RecipeKey<StateIngredient> INPUT = BotaniaSchemaComponents.STATE_INGREDIENT.inputKey("input");
	RecipeKey<Integer> MANA = NumberComponent.POSITIVE_INT.otherKey("mana");
	RecipeKey<Integer> WEIGHT = NumberComponent.NON_NEGATIVE_INT.otherKey("weight").optional(0);
	RecipeKey<Integer> COOLDOWN = NumberComponent.NON_NEGATIVE_INT.otherKey("cooldown").optional(0);
	RecipeKey<Integer> BIOME_BONUS_WEIGHT = NumberComponent.INT.otherKey("biome_bonus_weight").optional(0);
	RecipeKey<TagKey<Biome>> BIOME_BONUS_TAG = TagKeyComponent.BIOME.instance()
			.otherKey("biome_bonus_tag")
			.defaultOptional();
	RecipeKey<CacheableFunction> PRE_UPDATE_FUNCTION = BotaniaComponents.CACHEABLE_FUNCTION
			.otherKey("pre_update_function")
			.defaultOptional();
	RecipeKey<CacheableFunction> SUCCESS_FUNCTION = BotaniaComponents.CACHEABLE_FUNCTION
			.otherKey("success_function")
			.defaultOptional();

	/**
	 * Creates a fresh schema instance for one of the three orechid-like recipe types. Every type gets
	 * its own instance so that later changes (e.g. a type override) cannot leak between them.
	 */
	static RecipeSchema create() {
		return new RecipeSchema(OUTPUT, INPUT, MANA, WEIGHT, COOLDOWN, BIOME_BONUS_WEIGHT, BIOME_BONUS_TAG,
				PRE_UPDATE_FUNCTION, SUCCESS_FUNCTION);
	}

	RecipeSchema SCHEMA = create();
}
