package dev.kubejsbotania.neo.kubejs.component;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import dev.kubejsbotania.neo.KubeJSBotaniaNeo;
import dev.kubejsbotania.neo.recipe.TerraPlatePosition;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * KubeJS recipe component for a list of {@link TerraPlatePosition}.
 * <p>
 * Accepts the page-style {@code {positions:[...]}} object as well as a plain list. Each position is an object
 * with {@code x}/{@code z} plus exactly one of {@code block} (block id or {@code #tag}) or {@code fluid}
 * (fluid id). Serializes as a plain JSON list, matching the recipe serializer.
 *
 * @author KubeJS Botania Neo
 */
public class TerraPlatePositionComponent implements RecipeComponent<List<TerraPlatePosition>> {
	public static final RecipeComponentType<List<TerraPlatePosition>> TYPE = RecipeComponentType.unit(
			ResourceLocation.fromNamespaceAndPath(KubeJSBotaniaNeo.MOD_ID, "terra_plate_positions"),
			TerraPlatePositionComponent::new);

	public static final TerraPlatePositionComponent INSTANCE = new TerraPlatePositionComponent(TYPE);

	private final RecipeComponentType<?> type;

	public TerraPlatePositionComponent(RecipeComponentType<?> type) {
		this.type = type;
	}

	@Override
	public RecipeComponentType<?> type() {
		return type;
	}

	@Override
	public Codec<List<TerraPlatePosition>> codec() {
		return TerraPlatePosition.LIST_CODEC;
	}

	@Override
	public TypeInfo typeInfo() {
		return TypeInfo.RAW_LIST;
	}

	@Override
	public boolean hasPriority(RecipeMatchContext cx, Object from) {
		return from instanceof Iterable<?> || from instanceof Map<?, ?> || from instanceof JsonObject
				|| from != null && from.getClass().isArray();
	}

	@Override
	public List<TerraPlatePosition> wrap(RecipeScriptContext rcx, Object from) {
		var cx = rcx.cx();

		if (from == null) {
			return List.of();
		}

		if (cx.isMapLike(from)) {
			Map<String, Object> map = normalize(cx.optionalMapOf(from, TypeInfo.NONE, TypeInfo.NONE));

			if (map != null && map.containsKey("positions")) {
				return wrap(rcx, map.get("positions"));
			}

			return List.of(readPosition(map));
		}

		List<?> raw = cx.optionalListOf(from, TypeInfo.NONE);

		if (raw == null) {
			throw new IllegalArgumentException("Expected a list of terra plate positions, got: " + from);
		}

		List<TerraPlatePosition> list = new ArrayList<>(raw.size());

		for (Object element : raw) {
			if (!cx.isMapLike(element)) {
				throw new IllegalArgumentException("Expected a terra plate position object, got: " + element);
			}

			Map<String, Object> map = normalize(cx.optionalMapOf(element, TypeInfo.NONE, TypeInfo.NONE));

			if (map == null) {
				throw new IllegalArgumentException("Expected a terra plate position object, got: " + element);
			}

			list.add(readPosition(map));
		}

		return list;
	}

	@Override
	public boolean isEmpty(List<TerraPlatePosition> value) {
		// An empty list is valid: it means "use Botania's default platform".
		return false;
	}

	@Override
	public String toString() {
		return type.toString();
	}

	private static Map<String, Object> normalize(@Nullable Map<?, ?> map) {
		if (map == null) {
			return Map.of();
		}

		Map<String, Object> result = new LinkedHashMap<>(map.size());

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			result.put(Objects.toString(entry.getKey()), entry.getValue());
		}

		return result;
	}

	private static TerraPlatePosition readPosition(Map<String, Object> map) {
		int x = asInt(map.get("x"));
		int z = asInt(map.get("z"));
		String block = asString(map.get("block"));
		String fluid = asString(map.get("fluid"));

		return new TerraPlatePosition(x, z, block, fluid);
	}

	private static int asInt(@Nullable Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}

		if (value instanceof CharSequence text) {
			return Integer.parseInt(text.toString());
		}

		throw new IllegalArgumentException("Expected a number, got: " + value);
	}

	@Nullable
	private static String asString(@Nullable Object value) {
		return value == null ? null : value.toString();
	}
}
