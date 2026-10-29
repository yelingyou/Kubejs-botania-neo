package dev.kubejsbotania.neo.kubejs.schemas;

import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

/**
 * Schema for {@code botania:orechid_ignem} ({@code OrechidIgnemRecipe}).
 *
 * <p>Botania 1.21.1 uses the exact same codec as {@code botania:orechid}
 * ({@code OrechidRecipe.SERIALIZER.codec().xmap(...)} — see {@link OrechidSchema} for the field
 * list), so this schema simply reuses {@link OrechidSchema#create()}.</p>
 *
 * <p>Real JSON:</p>
 * <pre>{@code
 * {
 *   "type": "botania:orechid_ignem",
 *   "cooldown": 100,
 *   "input": {"type": "botania:block", "block": "minecraft:netherrack"},
 *   "mana": 20000,
 *   "output": {"type": "botania:block", "block": "minecraft:nether_quartz_ore"},
 *   "weight": 7387
 * }
 * }</pre>
 */
public interface OrechidIgnemSchema {
	RecipeSchema SCHEMA = OrechidSchema.create();
}
