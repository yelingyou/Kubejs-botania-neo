package dev.kubejsbotania.neo.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.kubejsbotania.neo.KubeJSBotaniaNeo;
import dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;

import java.util.List;

/**
 * Native EMI integration for the extended Terra Plate recipes.
 *
 * <p>EMI is optional: this class is only discovered and loaded by EMI when EMI is installed. It provides a
 * fully native EMI category (instead of relying on EMI's JEMI bridge importing the JEI page), including:</p>
 * <ul>
 *     <li>the custom {@code below} block/fluid pattern and the {@code result_blocks} preview,</li>
 *     <li>Botania's own mana bar (Botania's EMI {@code ManaWidget}),</li>
 *     <li>the Terrestrial Agglomeration Plate as the category <b>workstation</b>, so using/right-clicking the
 *     plate in EMI shows these recipes, plus a catalyst slot inside each recipe page.</li>
 * </ul>
 *
 * <p>The extended recipes are also filtered out of Botania's own EMI
 * {@code botania:terrestrial_agglomeration} page, so they only appear on the page of this mod.</p>
 *
 * @author KubeJS Botania Neo
 */
@EmiEntrypoint
public class TerraPlateExtensionEmiPlugin implements EmiPlugin {
	/**
	 * EMI category id of the extended Terra Plate recipes.
	 */
	public static final ResourceLocation CATEGORY_ID =
			ResourceLocation.fromNamespaceAndPath(KubeJSBotaniaNeo.MOD_ID, "terra_plate");

	private static final ResourceLocation BOTANIA_TERRESTRIAL_AGGLOMERATION =
			ResourceLocation.fromNamespaceAndPath("botania", "terrestrial_agglomeration");

	@Override
	public void register(EmiRegistry registry) {
		try {
			EmiStack plate = EmiStack.of(TerraPlateExtensionEmiRecipe.plateStack());
			EmiRecipeCategory category = new EmiRecipeCategory(CATEGORY_ID, plate);

			registry.addCategory(category);

			// The Terrestrial Agglomeration Plate is the workstation of this category: using the plate item
			// in EMI ("U" / recipe lookup) lists the extended recipes.
			registry.addWorkstation(category, plate);

			List<RecipeHolder<TerrestrialAgglomerationRecipe>> all =
					registry.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.TERRA_PLATE_TYPE);

			int added = 0;

			for (RecipeHolder<TerrestrialAgglomerationRecipe> holder : all) {
				if (holder.value() instanceof TerraPlateExtensionRecipe) {
					registry.addRecipe(new TerraPlateExtensionEmiRecipe(category, holder));
					added++;
				}
			}

			// Remove the same recipes from Botania's own EMI page (they are shown by the category above).
			registry.removeRecipes(recipe -> {
				if (recipe == null || recipe.getCategory() == null) {
					return false;
				}

				if (!BOTANIA_TERRESTRIAL_AGGLOMERATION.equals(recipe.getCategory().getId())) {
					return false;
				}

				ResourceLocation id = recipe.getId();
				return id != null && KubeJSBotaniaNeo.MOD_ID.equals(id.getNamespace());
			});

			KubeJSBotaniaNeo.LOGGER.info(
					"[{}] EMI: registered {} extended terra plate recipes (workstation = terrestrial agglomeration plate)",
					KubeJSBotaniaNeo.MOD_ID, added);
		} catch (Throwable t) {
			KubeJSBotaniaNeo.LOGGER.error("[{}] EMI: failed to register the terra plate extension category",
					KubeJSBotaniaNeo.MOD_ID, t);
		}
	}
}
