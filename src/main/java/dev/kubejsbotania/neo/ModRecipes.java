package dev.kubejsbotania.neo;

import dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe related registrations.
 * <p>
 * Only the serializer is registered: the extended Terra Plate recipes deliberately reuse Botania's
 * {@code botania:terrestrial_agglomeration_plate} recipe type (their {@code getType()} is inherited from
 * {@link vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe}), so Botania's recipe lookup and recipe
 * viewer integrations pick them up automatically.
 */
public final class ModRecipes {
	private ModRecipes() {
	}

	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
			DeferredRegister.create(Registries.RECIPE_SERIALIZER, KubeJSBotaniaNeo.MOD_ID);

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TerraPlateExtensionRecipe>> TERRA_PLATE_EXTENSION =
			RECIPE_SERIALIZERS.register("terra_plate", () -> TerraPlateExtensionRecipe.SERIALIZER);
}
