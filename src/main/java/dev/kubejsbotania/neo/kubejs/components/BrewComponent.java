package dev.kubejsbotania.neo.kubejs.components;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.brew.Brew;

/**
 * KubeJS 2101 recipe component for a Botania {@link Brew}.
 *
 * <p>Botania 1.21.1 keeps brews in its own {@code botania:brews} registry and the
 * {@code botania:brew} recipe codec refers to them by id
 * ({@code "brew": "botania:absolution"}). The component therefore serialises a brew as its registry
 * id and accepts ids or {@link Brew} instances from scripts.</p>
 *
 * <p>Note: the registry is only available after Botania registered it (NeoForge
 * {@code NewRegistryEvent}), therefore the codec is created lazily instead of in a static
 * initialiser.</p>
 */
public record BrewComponent(RecipeComponentType<?> type) implements RecipeComponent<Brew> {
	private static final Codec<Brew> CODEC = Codec.lazyInitialized(
			() -> BotaniaAPI.instance().getBrewRegistry().byNameCodec());

	/**
	 * Rhino type description used for script typing hints; brew ids and {@link Brew} instances are
	 * both valid inputs.
	 */
	public static final TypeInfo TYPE_INFO = TypeInfo.of(Brew.class);

	@Override
	public Codec<Brew> codec() {
		return CODEC;
	}

	@Override
	public TypeInfo typeInfo() {
		return TypeInfo.STRING.or(TYPE_INFO);
	}

	@Override
	public Brew wrap(RecipeScriptContext cx, Object from) {
		if (from instanceof Brew brew) {
			return brew;
		}

		if (from == null) {
			throw new KubeRuntimeException("Botania brew can't be null!");
		}

		var string = switch (from) {
			case JsonPrimitive primitive -> primitive.getAsString();
			case CharSequence text -> text.toString();
			case ResourceLocation id -> id.toString();
			default -> String.valueOf(from);
		};

		var registry = BotaniaAPI.instance().getBrewRegistry();
		var id = ResourceLocation.tryParse(string);

		if (id == null) {
			throw new KubeRuntimeException("Invalid botania brew id: " + string);
		}

		var brew = registry.get(id);

		// DefaultedRegistry#get returns the default value for unknown ids, so also compare the
		// resolved registry key to give scripts a useful error message.
		if (brew == null || !id.equals(registry.getKey(brew))) {
			throw new KubeRuntimeException("Unknown botania brew: " + id);
		}

		return brew;
	}

	@Override
	public String toString() {
		return type.toString();
	}
}
