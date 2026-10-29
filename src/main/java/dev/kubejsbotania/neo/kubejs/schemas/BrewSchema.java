package dev.kubejsbotania.neo.kubejs.schemas;

import dev.kubejsbotania.neo.kubejs.components.BotaniaComponents;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.crafting.Ingredient;
import vazkii.botania.api.brew.Brew;

import java.util.List;

/**
 * Schema for {@code botania:brew} ({@code BotanicalBreweryRecipe}, the Botanical Brewery).
 *
 * <p>Botania 1.21.1 codec ({@code BotanicalBreweryRecipe.Serializer.CODEC}):</p>
 * <ul>
 *     <li>{@code brew} - {@code BotaniaAPI.instance().getBrewRegistry().byNameCodec()}, required.
 *     This is the id of a Botania brew registry entry, e.g. {@code "botania:absolution"}.</li>
 *     <li>{@code ingredients} - {@code ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf())}, required</li>
 * </ul>
 *
 * <p><b>There is no {@code mana} field.</b> In 1.21.1 the mana cost comes from the brew itself
 * ({@code BotanicalBreweryRecipe#getManaUsage()} delegates to {@code Brew#getManaCost()}); the old
 * 1.20.1 schema did not have a mana key either, so nothing is lost. Brews are datapack registry
 * entries, not recipe JSON, so scripts can only reference existing brews here.</p>
 *
 * <p>Real JSON:</p>
 * <pre>{@code
 * {
 *   "type": "botania:brew",
 *   "brew": "botania:absolution",
 *   "ingredients": [
 *     {"item": "minecraft:nether_wart"},
 *     {"item": "minecraft:quartz"},
 *     {"item": "minecraft:emerald"},
 *     {"item": "minecraft:melon_slice"}
 *   ]
 * }
 * }</pre>
 */
public interface BrewSchema {
	RecipeKey<Brew> BREW = BotaniaComponents.BREW.outputKey("brew");
	RecipeKey<List<Ingredient>> INGREDIENTS = BotaniaSchemaComponents.INGREDIENT_LIST.inputKey("ingredients");

	RecipeSchema SCHEMA = new RecipeSchema(BREW, INGREDIENTS);
}
