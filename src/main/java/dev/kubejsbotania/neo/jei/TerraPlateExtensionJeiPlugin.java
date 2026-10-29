package dev.kubejsbotania.neo.jei;

import dev.kubejsbotania.neo.KubeJSBotaniaNeo;
import dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.client.integration.jei.TerrestrialAgglomerationRecipeCategory;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;

import java.util.List;

/**
 * JEI integration for the extended Terra Plate recipes.
 *
 * <p>The recipes themselves are indexed by Minecraft under Botania's real
 * {@code botania:terrestrial_agglomeration_plate} recipe type (see
 * {@link TerraPlateExtensionRecipe}), so Botania's own JEI plugin would list them on its
 * "Terrestrial Agglomeration" page as well. This plugin:</p>
 * <ul>
 *     <li>registers a dedicated JEI category (with Botania's own mana bar and the custom
 *     {@code below} pattern / {@code result_blocks} preview), and</li>
 *     <li>hides the extended recipes from Botania's category via JEI's
 *     {@link mezz.jei.api.recipe.IRecipeManager#hideRecipes} - hiding is stored per JEI
 *     {@link RecipeType}, so the dedicated category keeps showing them.</li>
 * </ul>
 *
 * <p>Hiding is defensive: if Botania's JEI plugin is not loaded for some reason (its recipe category was never
 * registered), JEI throws {@link IllegalStateException}; in that case the hide attempt is skipped with a
 * warning instead of failing. That situation also means there is no Botania JEI page to hide recipes from.</p>
 *
 * @author KubeJS Botania Neo
 */
@JeiPlugin
public class TerraPlateExtensionJeiPlugin implements IModPlugin {
	/**
	 * JEI recipe type of the extended terra plate recipes.
	 */
	public static final RecipeType<TerraPlateExtensionRecipe> RECIPE_TYPE =
			RecipeType.create(KubeJSBotaniaNeo.MOD_ID, "terra_plate", TerraPlateExtensionRecipe.class);

	private static final ResourceLocation PLUGIN_UID =
			ResourceLocation.fromNamespaceAndPath(KubeJSBotaniaNeo.MOD_ID, "jei_plugin");

	/**
	 * EMI is installed: the native EMI category is used instead of the JEI page, so that EMI's JEMI bridge
	 * does not import a duplicate of this page. Detection is class based to keep EMI a soft dependency.
	 */
	private static Boolean emiLoaded;

	private static boolean isEmiLoaded() {
		if (emiLoaded == null) {
			try {
				Class.forName("dev.emi.emi.api.EmiPlugin", false,
						TerraPlateExtensionJeiPlugin.class.getClassLoader());
				emiLoaded = Boolean.TRUE;
			} catch (ClassNotFoundException e) {
				emiLoaded = Boolean.FALSE;
			}
		}

		return emiLoaded;
	}

	@Nullable
	private static IJeiRuntime jeiRuntime;
	private static boolean listenersRegistered;
	@Nullable
	private static RecipeManager lastHideAttemptManager;

	@Override
	public ResourceLocation getPluginUid() {
		return PLUGIN_UID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		if (isEmiLoaded()) {
			KubeJSBotaniaNeo.LOGGER.info(
					"[{}] JEI: EMI detected, skipping JEI category registration (native EMI category is used)",
					KubeJSBotaniaNeo.MOD_ID);
			return;
		}

		KubeJSBotaniaNeo.LOGGER.info("[{}] JEI: registering terra plate extension category", KubeJSBotaniaNeo.MOD_ID);
		registration.addRecipeCategories(
				new TerraPlateExtensionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		if (isEmiLoaded()) {
			return;
		}

		var client = Minecraft.getInstance();
		var level = client.level;

		if (level == null) {
			KubeJSBotaniaNeo.LOGGER.warn("[{}] JEI: no client level yet, skipping terra plate recipe registration",
					KubeJSBotaniaNeo.MOD_ID);
			return;
		}

		List<RecipeHolder<TerrestrialAgglomerationRecipe>> all =
				level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.TERRA_PLATE_TYPE);

		List<TerraPlateExtensionRecipe> recipes = all.stream()
				.filter(holder -> holder.value() instanceof TerraPlateExtensionRecipe)
				.map(RecipeHolder::value)
				.map(TerraPlateExtensionRecipe.class::cast)
				.toList();

		KubeJSBotaniaNeo.LOGGER.info("[{}] JEI: {} terra plate recipes total, {} extended recipes",
				KubeJSBotaniaNeo.MOD_ID, all.size(), recipes.size());

		registration.addRecipes(RECIPE_TYPE, recipes);
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		if (isEmiLoaded()) {
			return;
		}

		ResourceLocation plateId = ResourceLocation.fromNamespaceAndPath("botania", "terrestrial_agglomeration_plate");
		Item plate = BuiltInRegistries.ITEM.get(plateId);

		if (plate != null) {
			registration.addRecipeCatalyst(new ItemStack(plate), RECIPE_TYPE);
		}
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		jeiRuntime = runtime;
		ensureHideListeners();
		hideExtensionRecipes(recipeManagerOf(Minecraft.getInstance()));
	}

	@Override
	public void onRuntimeUnavailable() {
		jeiRuntime = null;
		lastHideAttemptManager = null;
	}

	/**
	 * Registers listeners once. Recipes are available only after a world has been joined (and are rebuilt on
	 * every data pack reload), so hiding is re-applied whenever the client {@link RecipeManager} instance changes.
	 */
	private static void ensureHideListeners() {
		if (listenersRegistered) {
			return;
		}

		listenersRegistered = true;

		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
			var client = Minecraft.getInstance();

			if (client.level == null && client.getConnection() == null) {
				return;
			}

			hideExtensionRecipes(recipeManagerOf(client));
		});

		NeoForge.EVENT_BUS.addListener((RecipesUpdatedEvent event) -> hideExtensionRecipes(event.getRecipeManager()));
	}

	@Nullable
	private static RecipeManager recipeManagerOf(Minecraft client) {
		if (client.level != null) {
			return client.level.getRecipeManager();
		}

		if (client.getConnection() != null) {
			return client.getConnection().getRecipeManager();
		}

		return null;
	}

	/**
	 * Hides the extended recipes from Botania's own terrestrial agglomeration JEI page. The dedicated
	 * {@link #RECIPE_TYPE} category is a different JEI recipe type and therefore keeps showing them.
	 */
	private static void hideExtensionRecipes(@Nullable RecipeManager manager) {
		IJeiRuntime runtime = jeiRuntime;

		if (runtime == null || manager == null || manager == lastHideAttemptManager) {
			return;
		}

		// Mark before attempting: if Botania's JEI plugin is missing, the category is never registered and we
		// must not retry (and throw) every client tick.
		lastHideAttemptManager = manager;

		List<TerrestrialAgglomerationRecipe> extended = manager.getAllRecipesFor(BotaniaRecipeTypes.TERRA_PLATE_TYPE)
				.stream()
				.filter(holder -> holder.value() instanceof TerraPlateExtensionRecipe)
				.map(RecipeHolder::value)
				.map(TerrestrialAgglomerationRecipe.class::cast)
				.toList();

		if (extended.isEmpty()) {
			return;
		}

		try {
			// TerrestrialAgglomerationRecipeCategory.TYPE is Botania's own JEI recipe type for its
			// "Terrestrial Agglomeration" page (botania:terra_plate).
			runtime.getRecipeManager().hideRecipes(TerrestrialAgglomerationRecipeCategory.TYPE, extended);

			KubeJSBotaniaNeo.LOGGER.info(
					"[{}] JEI: hid {} extended terra plate recipes from Botania's agglomeration page",
					KubeJSBotaniaNeo.MOD_ID, extended.size());
		} catch (IllegalStateException e) {
			KubeJSBotaniaNeo.LOGGER.warn(
					"[{}] JEI: Botania's terrestrial agglomeration JEI category is not registered; skipping hide ({})",
					KubeJSBotaniaNeo.MOD_ID, e.getMessage());
		} catch (Throwable t) {
			KubeJSBotaniaNeo.LOGGER.warn("[{}] JEI: failed to hide extended terra plate recipes",
					KubeJSBotaniaNeo.MOD_ID, t);
		}
	}
}
