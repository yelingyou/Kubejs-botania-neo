package dev.kubejsbotania.neo.kubejs.schemas;

import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

/**
 * Schema for {@code botania:marimorphosis} ({@code MarimorphosisRecipe}).
 *
 * <p>Botania 1.21.1 uses the same codec as {@code botania:orechid}
 * ({@code OrechidRecipe.SERIALIZER.codec().xmap(...)} — see {@link OrechidSchema} for the field
 * list). {@code MarimorphosisRecipe} forces {@code cooldown} to {@code 0}; the field is still part of
 * the shared codec but is not written by Botania's datagen.</p>
 *
 * <p>Real JSON:</p>
 * <pre>{@code
 * {
 *   "type": "botania:marimorphosis",
 *   "biome_bonus_tag": "botania:marimorphosis_swamp_bonus",
 *   "biome_bonus_weight": 11,
 *   "input": {"type": "botania:tag", "tag": "#botania:marimorphosis_convertable"},
 *   "mana": 12,
 *   "output": {"type": "botania:block", "block": "botania:cataclasite"},
 *   "weight": 1
 * }
 * }</pre>
 */
public interface MarimorphosisSchema {
	RecipeSchema SCHEMA = OrechidSchema.create();
}
