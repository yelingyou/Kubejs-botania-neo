package dev.kubejsbotania.neo.kubejs;

import dev.kubejsbotania.neo.kubejs.component.FlexibleIngredientListComponent;
import dev.kubejsbotania.neo.kubejs.component.FlexibleItemStackListComponent;
import dev.kubejsbotania.neo.kubejs.component.TerraPlatePositionComponent;
import dev.kubejsbotania.neo.kubejs.components.BotaniaComponents;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/**
 * The single KubeJS 2101 plugin entry point of KubeJS Botania Neo.
 *
 * <p>KubeJS 2101 does <b>not</b> use {@code ServiceLoader} for plugins; it reads the mod's
 * {@code kubejs.plugins.txt} resource and instantiates every class listed there (see
 * {@code KubeJSPlugins#loadMod}). The mod jar therefore ships a
 * {@code src/main/resources/kubejs.plugins.txt} containing this fully qualified class name.</p>
 *
 * <p>What is registered:</p>
 * <ul>
 *     <li>custom recipe components ({@link BotaniaComponents} - StateIngredient / Brew /
 *     CacheableFunction), the Terra Plate extension position component, and the script-friendly
 *     item/ingredient list components,</li>
 *     <li>the 10 Botania recipe schemas ({@link BotaniaRecipeSchemas}) plus the extended Terra Plate
 *     schema ({@link TerraPlateExtensionSchema}),</li>
 *     <li>the {@code ManaHandler} / {@code CorporeaHelper} script bindings ({@link BotaniaBindings}).</li>
 * </ul>
 */
public class BotaniaKubeJSPlugin implements KubeJSPlugin {
	@Override
	public void registerRecipeComponents(RecipeComponentTypeRegistry registry) {
		BotaniaComponents.register(registry);

		// Component type used by the extended Terra Plate schema (dev.kubejsbotania.neo.kubejs.TerraPlateExtensionSchema).
		registry.register(TerraPlatePositionComponent.TYPE);

		// Script-friendly list components (object-form values are re-encoded into canonical JSON).
		registry.register(FlexibleIngredientListComponent.TYPE);
		registry.register(FlexibleItemStackListComponent.TYPE);
	}

	@Override
	public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
		BotaniaRecipeSchemas.register(registry);

		// The mod's own Terra Plate extension recipe type (kubejs_botania_neo:terra_plate), which
		// produces items through Botania's real botania:terrestrial_agglomeration_plate recipe type.
		TerraPlateExtensionSchema.register(registry);
	}

	@Override
	public void registerBindings(BindingRegistry bindings) {
		BotaniaBindings.register(bindings);
	}
}
