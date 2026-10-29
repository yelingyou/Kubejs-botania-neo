package dev.kubejsbotania.neo.kubejs;

import dev.kubejsbotania.neo.kubejs.schemas.BrewSchema;
import dev.kubejsbotania.neo.kubejs.schemas.ElvenTradeSchema;
import dev.kubejsbotania.neo.kubejs.schemas.ManaInfusionSchema;
import dev.kubejsbotania.neo.kubejs.schemas.MarimorphosisSchema;
import dev.kubejsbotania.neo.kubejs.schemas.OrechidIgnemSchema;
import dev.kubejsbotania.neo.kubejs.schemas.OrechidSchema;
import dev.kubejsbotania.neo.kubejs.schemas.PetalApothecarySchema;
import dev.kubejsbotania.neo.kubejs.schemas.PureDaisySchema;
import dev.kubejsbotania.neo.kubejs.schemas.RunicAltarSchema;
import dev.kubejsbotania.neo.kubejs.schemas.TerraPlateSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeNamespace;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;

/**
 * Registers the KubeJS 2101 recipe schemas for Botania's recipe types.
 *
 * <p>The namespace is always {@code botania} and every path is the real Botania 1.21.1 recipe
 * serializer id, because KubeJS resolves the serializer through
 * {@code RecipeSchemaType#getSerializer()} using exactly this id. Datapack recipe JSONs are also
 * matched back to these schemas by their {@code "type"} string, so using the real ids means KubeJS
 * can read/modify Botania's own recipes instead of falling back to
 * {@code UnknownRecipeSchemaType}.</p>
 *
 * <p>Script counterpart: {@code event.recipes.botania.<name>(...)}.</p>
 */
public final class BotaniaRecipeSchemas {
	/**
	 * The namespace all Botania recipes live in.
	 */
	public static final String NAMESPACE = "botania";

	private BotaniaRecipeSchemas() {
	}

	/**
	 * Registers all Botania recipe schemas. Called from
	 * {@link dev.latvian.mods.kubejs.plugin.KubeJSPlugin#registerRecipeSchemas}.
	 *
	 * @param registry the KubeJS recipe schema registry
	 */
	public static void register(RecipeSchemaRegistry registry) {
		RecipeNamespace botania = registry.namespace(NAMESPACE);

		// The 10 recipe types Botania 1.21.1 actually registers.
		botania.register("mana_infusion", ManaInfusionSchema.SCHEMA);
		botania.register("pure_daisy", PureDaisySchema.SCHEMA);
		botania.register("elven_trade", ElvenTradeSchema.SCHEMA);
		botania.register("runic_altar", RunicAltarSchema.SCHEMA);
		botania.register("brew", BrewSchema.SCHEMA);
		botania.register("petal_apothecary", PetalApothecarySchema.SCHEMA);
		botania.register("orechid", OrechidSchema.SCHEMA);
		botania.register("orechid_ignem", OrechidIgnemSchema.SCHEMA);
		botania.register("marimorphosis", MarimorphosisSchema.SCHEMA);

		// 1.21.1 renamed the Terra Plate recipe type from botania:terra_plate to
		// botania:terrestrial_agglomeration_plate. Register the real id first (required for datapack
		// JSON parsing), then a compatibility alias for the friendlier 1.20.1 script name. The alias
		// uses RecipeSchema#typeOverride, so KubeJS still writes the real type id into recipe JSON.
		botania.register("terrestrial_agglomeration_plate", TerraPlateSchema.SCHEMA);
		botania.register("terra_plate", TerraPlateSchema.ALIAS_SCHEMA);
	}
}
