package dev.kubejsbotania.neo.kubejs.component;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import dev.kubejsbotania.neo.KubeJSBotaniaNeo;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentValue;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * List of ingredients with script-friendly codec and object-form rewrite support.
 *
 * <p>{@link ListRecipeComponent} does not mark values read from object-style JSON for re-serialisation, so a
 * script list like {@code ['minecraft:diamond', '#minecraft:logs']} would otherwise be written to the recipe
 * JSON verbatim and rejected by the recipe serializers. This component wraps the flexible ingredient codec and
 * marks the list as written, so KubeJS re-encodes it into canonical ingredient JSON.</p>
 *
 * @author KubeJS Botania Neo
 */
public record FlexibleIngredientListComponent(RecipeComponentType<?> type) implements RecipeComponent<List<Ingredient>> {
	public static final RecipeComponentType<List<Ingredient>> TYPE = RecipeComponentType.unit(
			ResourceLocation.fromNamespaceAndPath(KubeJSBotaniaNeo.MOD_ID, "ingredient_list"),
			FlexibleIngredientListComponent::new);

	public static final FlexibleIngredientListComponent INSTANCE = new FlexibleIngredientListComponent(TYPE);

	private static final FlexibleIngredientComponent INNER = new FlexibleIngredientComponent(IngredientComponent.INGREDIENT);

	@Override
	public RecipeComponentType<?> type() {
		return type;
	}

	@Override
	public Codec<List<Ingredient>> codec() {
		return KubeJSCodecs.INGREDIENT.listOf();
	}

	@Override
	public TypeInfo typeInfo() {
		return TypeInfo.RAW_LIST.withParams(IngredientWrapper.TYPE_INFO);
	}

	@Override
	public List<Ingredient> wrap(RecipeScriptContext cx, Object from) {
		return ListRecipeComponent.wrap0(cx, INNER, from);
	}

	@Override
	public boolean hasPriority(dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext cx, Object from) {
		return from instanceof Iterable<?> || from != null && from.getClass().isArray();
	}

	@Override
	public void readFromJson(KubeRecipe recipe, RecipeComponentValue<List<Ingredient>> value, JsonObject json) {
		ComponentIo.readAndMarkForRewrite(this, recipe, value, json);
	}

	/**
	 * Bounds API compatibility with KubeJS's {@code ListRecipeComponent} (the original schemas call
	 * {@code withBounds(...)}); list sizes are validated by the Botania recipe serializers themselves.
	 */
	public RecipeComponent<List<Ingredient>> withBounds(dev.latvian.mods.kubejs.util.IntBounds bounds) {
		return this;
	}

	@Override
	public boolean isEmpty(List<Ingredient> value) {
		// Empty lists are legal for Botania's optional ingredient lists (e.g. runic altar catalysts).
		return false;
	}

	@Override
	public String toString() {
		return type.toString();
	}
}
