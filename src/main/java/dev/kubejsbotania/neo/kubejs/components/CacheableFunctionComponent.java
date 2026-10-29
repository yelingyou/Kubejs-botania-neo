package dev.kubejsbotania.neo.kubejs.components;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.commands.CacheableFunction;
import net.minecraft.resources.ResourceLocation;

/**
 * KubeJS 2101 recipe component for a {@link CacheableFunction}, i.e. a datapack
 * {@code .mcfunction} reference.
 *
 * <p>Botania 1.21.1 added {@code pre_update_function} / {@code success_function} fields to
 * {@code pure_daisy}, {@code orechid}, {@code orechid_ignem} and {@code marimorphosis} recipes. They
 * are serialised as a plain function id string, for example
 * {@code "success_function": "botania:ender_air_release"}.</p>
 */
public record CacheableFunctionComponent(RecipeComponentType<?> type) implements RecipeComponent<CacheableFunction> {
	/**
	 * Rhino type description used for script typing hints; function ids and
	 * {@link CacheableFunction} instances are both valid inputs.
	 */
	public static final TypeInfo TYPE_INFO = TypeInfo.of(CacheableFunction.class);

	@Override
	public Codec<CacheableFunction> codec() {
		return CacheableFunction.CODEC;
	}

	@Override
	public TypeInfo typeInfo() {
		return TypeInfo.STRING.or(TYPE_INFO);
	}

	@Override
	public CacheableFunction wrap(RecipeScriptContext cx, Object from) {
		if (from instanceof CacheableFunction function) {
			return function;
		}

		if (from == null) {
			throw new KubeRuntimeException("Function id can't be null!");
		}

		var string = switch (from) {
			case JsonPrimitive primitive -> primitive.getAsString();
			case CharSequence text -> text.toString();
			case ResourceLocation id -> id.toString();
			default -> String.valueOf(from);
		};

		return CacheableFunction.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(string)).getOrThrow(
				message -> new KubeRuntimeException("Invalid .mcfunction id '" + string + "': " + message));
	}

	@Override
	public String toString() {
		return type.toString();
	}
}
