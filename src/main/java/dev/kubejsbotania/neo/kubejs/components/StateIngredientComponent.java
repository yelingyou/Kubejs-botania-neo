package dev.kubejsbotania.neo.kubejs.components;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import dev.kubejsbotania.neo.kubejs.component.ComponentIo;
import dev.kubejsbotania.neo.kubejs.component.KubeJSCodecs;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.BlockWrapper;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentValue;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.api.recipe.StateIngredient;
import vazkii.botania.common.crafting.StateIngredients;

import java.util.Map;

/**
 * KubeJS 2101 recipe component for Botania's {@link StateIngredient}.
 *
 * <p>Serialisation is delegated to Botania's typed codec, extended by {@link KubeJSCodecs#STATE_INGREDIENT} so
 * that script strings ({@code "minecraft:stone"}, {@code "#block_tag"}) are accepted as well. Values read from
 * object-style recipe JSON are marked for re-serialisation so KubeJS writes the canonical Botania JSON instead
 * of the raw script string.</p>
 *
 * <pre>{@code
 * {"type": "botania:block", "block": "minecraft:stone"}
 * {"type": "botania:tag",   "tag": "#minecraft:logs"}
 * {"type": "botania:state", "state": {"Name": "minecraft:cobblestone"}}
 * {"type": "botania:any_of", "ingredients": [...]}
 * {"type": "botania:all_of_excluding", "ingredients": [...], "excluding": [...]}
 * }</pre>
 *
 * <p>Script input is deliberately permissive: {@link StateIngredient}, {@link BlockState},
 * {@link Block}, {@link TagKey}, block state strings ({@code "minecraft:oak_log"}), block tags
 * ({@code "#minecraft:logs"}) and raw JSON objects are accepted.</p>
 */
public record StateIngredientComponent(RecipeComponentType<?> type) implements RecipeComponent<StateIngredient> {
	/**
	 * Rhino type description used for script typing hints; strings and {@link StateIngredient}
	 * instances are both valid inputs.
	 */
	public static final TypeInfo TYPE_INFO = TypeInfo.of(StateIngredient.class);

	@Override
	public Codec<StateIngredient> codec() {
		return KubeJSCodecs.STATE_INGREDIENT;
	}

	@Override
	public TypeInfo typeInfo() {
		return TypeInfo.STRING.or(TYPE_INFO);
	}

	@Override
	public void readFromJson(KubeRecipe recipe, RecipeComponentValue<StateIngredient> value, JsonObject json) {
		ComponentIo.readAndMarkForRewrite(this, recipe, value, json);
	}

	@Override
	public StateIngredient wrap(RecipeScriptContext cx, Object from) {
		if (from instanceof StateIngredient ingredient) {
			return ingredient;
		}

		if (from instanceof BlockState state) {
			return StateIngredients.of(state);
		}

		if (from instanceof Block block) {
			return StateIngredients.of(block);
		}

		if (from instanceof TagKey<?> tag) {
			if (!Registries.BLOCK.equals(tag.registry())) {
				throw new KubeRuntimeException("Expected a block tag for a botania StateIngredient, got " + tag);
			}

			@SuppressWarnings("unchecked")
			var blockTag = (TagKey<Block>) tag;
			return StateIngredients.of(blockTag);
		}

		if (from instanceof JsonPrimitive primitive) {
			return fromString(cx, primitive.getAsString());
		}

		if (from instanceof CharSequence string) {
			return fromString(cx, string.toString());
		}

		// Raw JSON (from other recipes/data packs) as well as JS object literals such as
		// { type: 'botania:block', block: 'minecraft:stone' } are parsed with Botania's own codec.
		if (from instanceof JsonElement || from instanceof Map<?, ?> || cx.cx().isMapLike(from)) {
			var json = from instanceof JsonElement element ? element : JsonUtils.of(cx.cx(), from);

			return StateIngredients.TYPED_CODEC.parse(cx.ops().json(), json).getOrThrow(
					message -> new KubeRuntimeException(
							"Failed to read a botania StateIngredient from " + json + ": " + message));
		}

		throw new KubeRuntimeException("Can't convert '" + from + "' to a botania StateIngredient");
	}

	/**
	 * Parses the short string forms used in KubeJS scripts: {@code "#tag"} for block tags,
	 * {@code "namespace:block"} for block type ingredients and
	 * {@code "namespace:block[property=value]"} for exact block state ingredients.
	 */
	private static StateIngredient fromString(RecipeScriptContext cx, String string) {
		if (string.startsWith("#")) {
			var id = ResourceLocation.tryParse(string.substring(1));

			if (id == null) {
				throw new KubeRuntimeException("Invalid block tag: " + string);
			}

			return StateIngredients.of(TagKey.create(Registries.BLOCK, id));
		}

		BlockState state;

		try {
			state = BlockWrapper.parseBlockState(cx.registries(), string);
		} catch (Exception ex) {
			throw new KubeRuntimeException("Invalid block or block state: " + string, ex);
		}

		// A plain block id serialises as {"type":"botania:block"}, an exact state as
		// {"type":"botania:state"}, which mirrors what Botania itself generates.
		return string.indexOf('[') == -1 ? StateIngredients.of(state.getBlock()) : StateIngredients.of(state);
	}

	@Override
	public String toString() {
		return type.toString();
	}
}
