package dev.kubejsbotania.neo.kubejs.components;

import dev.kubejsbotania.neo.KubeJSBotaniaNeo;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import net.minecraft.commands.CacheableFunction;
import net.minecraft.resources.ResourceLocation;
import vazkii.botania.api.brew.Brew;
import vazkii.botania.api.recipe.StateIngredient;

/**
 * Registry-independent holders for the custom KubeJS 2101 recipe components needed by Botania's
 * recipe schemas.
 *
 * <p>KubeJS 2101 looks recipe components up by {@link RecipeComponentType} when it serialises the
 * loaded recipe schemas (data generation / web API). Because of that every custom component has to be
 * registered from {@link dev.latvian.mods.kubejs.plugin.KubeJSPlugin#registerRecipeComponents}, which
 * is what {@link #register(RecipeComponentTypeRegistry)} is for. The
 * {@link dev.latvian.mods.kubejs.plugin.KubeJSPlugin} exposed by this mod
 * ({@code dev.kubejsbotania.neo.kubejs.BotaniaKubeJSPlugin}) already calls it.</p>
 *
 * <p>Only three components are custom, everything else is reused from KubeJS 2101 built-ins
 * ({@code ItemStackComponent}, {@code IngredientComponent}, {@code TagKeyComponent},
 * {@code NumberComponent}, {@code StringComponent}, {@code BooleanComponent}, ...).</p>
 */
public interface BotaniaComponents {
	/**
	 * A Botania {@link StateIngredient} (block, block state, block tag, {@code any_of},
	 * {@code all_of_excluding}). Serialised with Botania's own typed codec, so the JSON written by
	 * KubeJS is byte-for-byte compatible with Botania's datapack format.
	 */
	RecipeComponentType<StateIngredient> STATE_INGREDIENT = RecipeComponentType.unit(
			id("state_ingredient"), StateIngredientComponent::new);

	/**
	 * A {@link Brew} reference (Botania's {@code botania:brews} registry). Scripts may pass a brew id
	 * such as {@code 'botania:absorption'} or a {@link Brew} instance.
	 */
	RecipeComponentType<Brew> BREW = RecipeComponentType.unit(id("brew"), BrewComponent::new);

	/**
	 * A {@link CacheableFunction} reference ({@code .mcfunction} id, e.g. {@code 'botania:ender_air_release'}).
	 */
	RecipeComponentType<CacheableFunction> CACHEABLE_FUNCTION = RecipeComponentType.unit(
			id("cacheable_function"), CacheableFunctionComponent::new);

	/**
	 * {@return a resource location in this mod's namespace}
	 *
	 * @param path the path of the id
	 */
	static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(KubeJSBotaniaNeo.MOD_ID, path);
	}

	/**
	 * Registers every custom component type, called from
	 * {@link dev.latvian.mods.kubejs.plugin.KubeJSPlugin#registerRecipeComponents}.
	 *
	 * @param registry the KubeJS component type registry
	 */
	static void register(RecipeComponentTypeRegistry registry) {
		registry.register(STATE_INGREDIENT);
		registry.register(BREW);
		registry.register(CACHEABLE_FUNCTION);
	}
}
