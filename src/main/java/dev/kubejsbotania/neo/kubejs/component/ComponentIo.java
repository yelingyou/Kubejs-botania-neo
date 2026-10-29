package dev.kubejsbotania.neo.kubejs.component;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentValue;

/**
 * Shared helper for reading a component value from an object-style recipe JSON and marking it for
 * re-serialisation.
 *
 * <p>KubeJS 2101's object-style recipe call (a single JS/JSON object argument) is decoded component-per-key via
 * {@link RecipeComponent#readFromJson(KubeRecipe, RecipeComponentValue, JsonObject)}, but the decoded value is
 * <b>not</b> marked as dirty, so on serialisation KubeJS would simply pass the original JSON through. That is a
 * problem for script-friendly values that are not valid serializer JSON (e.g. a plain string
 * {@code "minecraft:diamond"} instead of {@code {"item":"minecraft:diamond"}}). Marking the value as written
 * forces the component's codec to re-encode it into the canonical JSON form.</p>
 *
 * @author KubeJS Botania Neo
 */
public final class ComponentIo {
	private ComponentIo() {
	}

	public static <T> void readAndMarkForRewrite(RecipeComponent<T> component, KubeRecipe recipe,
			RecipeComponentValue<T> value, JsonObject json) {
		var ops = recipe.type.event.ops.json();
		var element = json.get(value.key.name);

		if (element != null) {
			value.value = component.codec().parse(ops, element).getOrThrow();
			value.write();
			return;
		}

		if (value.key.names.size() >= 2) {
			for (String alternative : value.key.names) {
				element = json.get(alternative);

				if (element != null) {
					value.value = component.codec().parse(ops, element).getOrThrow();
					value.write();
					return;
				}
			}
		}
	}
}
