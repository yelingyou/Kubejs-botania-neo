package dev.kubejsbotania.neo.kubejs.component;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentValue;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Ingredient component with:
 * <ul>
 *     <li>a script-friendly codec ({@code 'minecraft:diamond'}, {@code '#tag'}, vanilla ingredient JSON);</li>
 *     <li>object-form support: decoded values are marked for re-serialisation so KubeJS rewrites them into the
 *     canonical ingredient JSON that recipe serializers expect.</li>
 * </ul>
 *
 * @author KubeJS Botania Neo
 */
public record FlexibleIngredientComponent(RecipeComponentType<?> type) implements RecipeComponent<Ingredient> {
	public static final TypeInfo TYPE_INFO = IngredientWrapper.TYPE_INFO;

	@Override
	public RecipeComponentType<?> type() {
		return type;
	}

	@Override
	public Codec<Ingredient> codec() {
		return KubeJSCodecs.INGREDIENT;
	}

	@Override
	public TypeInfo typeInfo() {
		return TYPE_INFO;
	}

	@Override
	public void readFromJson(KubeRecipe recipe, RecipeComponentValue<Ingredient> value, JsonObject json) {
		ComponentIo.readAndMarkForRewrite(this, recipe, value, json);
	}

	@Override
	public String toString() {
		return type.toString();
	}
}
