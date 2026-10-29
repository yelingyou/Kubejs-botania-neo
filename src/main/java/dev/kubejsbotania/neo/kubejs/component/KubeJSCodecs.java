package dev.kubejsbotania.neo.kubejs.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import vazkii.botania.api.recipe.StateIngredient;
import vazkii.botania.common.crafting.StateIngredients;

/**
 * JSON codecs that accept the "script friendly" forms used by KubeJS object-style recipe calls, in addition
 * to the standard vanilla/Botania JSON forms.
 *
 * <p>Why this is needed: KubeJS 2101 parses the single-object form of a recipe call by converting the JS
 * object to JSON and then running each recipe key's {@link Codec} over it (the JS value wrappers are only
 * used for positional arguments). Vanilla's {@code Ingredient.CODEC_NONEMPTY},
 * {@code ItemStack.STRICT_CODEC} and Botania's {@code StateIngredients.TYPED_CODEC} do not accept plain
 * JSON strings such as {@code "minecraft:diamond"}, so without these codecs the object form of the Botania
 * recipes ({@code event.recipes.botania.mana_infusion({ input: 'minecraft:coal', ... })}) and of the
 * extended terra plate recipe would fail to parse.</p>
 *
 * <p>Accepted forms:</p>
 * <ul>
 *     <li>ingredient: {@code "minecraft:diamond"}, {@code "#minecraft:logs"} (item tag), or any standard
 *     vanilla ingredient JSON (object / array);</li>
 *     <li>item stack: {@code "minecraft:diamond"}, {@code {"item":"minecraft:diamond","count":2}} (page
 *     style), or the strict item stack JSON ({@code {"id":...}});</li>
 *     <li>StateIngredient: {@code "minecraft:stone"}, {@code "#minecraft:base_stone_overworld"} (block tag)
 *     or the standard Botania JSON ({@code {"type":"botania:block",...}}, {@code {"type":"botania:state",...}},
 *     {@code any_of}, {@code all_of_excluding}).</li>
 * </ul>
 * Encoding always uses the vanilla/Botania JSON form so the recipe serializers can read it back.
 *
 * @author KubeJS Botania Neo
 */
public final class KubeJSCodecs {
	private KubeJSCodecs() {
	}

	/**
	 * Page-style item stack object: {@code {"item": "minecraft:diamond", "count": 2}}.
	 */
	public static final MapCodec<ItemStack> PAGE_STYLE_ITEM_STACK = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.fieldOf("item").forGetter(stack -> {
				ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
				return id == null ? "minecraft:air" : id.toString();
			}),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(ItemStack::getCount)
	).apply(instance, (item, count) -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)), count)));

	/**
	 * Item stack codec: script string / page-style {@code {item,count}} / strict vanilla JSON.
	 */
	public static final Codec<ItemStack> ITEM_STACK = new Codec<>() {
		@Override
		public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
			var stringResult = ops.getStringValue(input);

			if (stringResult.result().isPresent()) {
				String value = stringResult.result().get();
				ResourceLocation id = ResourceLocation.tryParse(value);

				if (id == null) {
					return DataResult.error(() -> "Invalid item id: " + value);
				}

				Item item = BuiltInRegistries.ITEM.get(id);

				if (item == Items.AIR && !id.equals(ResourceLocation.withDefaultNamespace("air"))) {
					return DataResult.error(() -> "Unknown item: " + value);
				}

				return DataResult.success(Pair.of(new ItemStack(item), ops.empty()));
			}

			var pageStyle = PAGE_STYLE_ITEM_STACK.codec().decode(ops, input);

			if (pageStyle.result().isPresent()) {
				return pageStyle;
			}

			return ItemStack.STRICT_CODEC.decode(ops, input);
		}

		@Override
		public <T> DataResult<T> encode(ItemStack input, DynamicOps<T> ops, T prefix) {
			return ItemStack.STRICT_CODEC.encode(input, ops, prefix);
		}
	};

	/**
	 * Ingredient codec: script string ({@code "minecraft:diamond"} / {@code "#tag"}) or vanilla ingredient JSON.
	 */
	public static final Codec<Ingredient> INGREDIENT = new Codec<>() {
		@Override
		public <T> DataResult<Pair<Ingredient, T>> decode(DynamicOps<T> ops, T input) {
			var stringResult = ops.getStringValue(input);

			if (stringResult.result().isPresent()) {
				DataResult<Ingredient> ingredient = ingredientFromString(stringResult.result().get());

				if (ingredient.result().isPresent()) {
					return DataResult.success(Pair.of(ingredient.result().get(), ops.empty()));
				}

				return ingredient.map(ing -> Pair.of(ing, ops.empty()));
			}

			return Ingredient.CODEC_NONEMPTY.decode(ops, input);
		}

		@Override
		public <T> DataResult<T> encode(Ingredient input, DynamicOps<T> ops, T prefix) {
			return Ingredient.CODEC_NONEMPTY.encode(input, ops, prefix);
		}
	};

	/**
	 * StateIngredient codec: script string ({@code "minecraft:stone"} / {@code "#block_tag"}) or Botania JSON.
	 */
	public static final Codec<StateIngredient> STATE_INGREDIENT = new Codec<>() {
		@Override
		public <T> DataResult<Pair<StateIngredient, T>> decode(DynamicOps<T> ops, T input) {
			var stringResult = ops.getStringValue(input);

			if (stringResult.result().isPresent()) {
				String value = stringResult.result().get();
				DataResult<StateIngredient> ingredient = stateIngredientFromString(value);

				if (ingredient.result().isPresent()) {
					return DataResult.success(Pair.of(ingredient.result().get(), ops.empty()));
				}

				return ingredient.map(ing -> Pair.of(ing, ops.empty()));
			}

			return StateIngredients.TYPED_CODEC.decode(ops, input);
		}

		@Override
		public <T> DataResult<T> encode(StateIngredient input, DynamicOps<T> ops, T prefix) {
			return StateIngredients.TYPED_CODEC.encode(input, ops, prefix);
		}
	};

	private static DataResult<Ingredient> ingredientFromString(String value) {
		if (value.startsWith("#")) {
			ResourceLocation tagId = ResourceLocation.tryParse(value.substring(1));

			if (tagId == null) {
				return DataResult.error(() -> "Invalid item tag: " + value);
			}

			return DataResult.success(Ingredient.of(TagKey.create(Registries.ITEM, tagId)));
		}

		ResourceLocation id = ResourceLocation.tryParse(value);

		if (id == null) {
			return DataResult.error(() -> "Invalid item id: " + value);
		}

		Item item = BuiltInRegistries.ITEM.get(id);

		if (item == Items.AIR && !id.equals(ResourceLocation.withDefaultNamespace("air"))) {
			return DataResult.error(() -> "Unknown item: " + value);
		}

		return DataResult.success(Ingredient.of(item));
	}

	private static DataResult<StateIngredient> stateIngredientFromString(String value) {
		if (value.startsWith("#")) {
			ResourceLocation tagId = ResourceLocation.tryParse(value.substring(1));

			if (tagId == null) {
				return DataResult.error(() -> "Invalid block tag: " + value);
			}

			return DataResult.success(StateIngredients.of(TagKey.create(Registries.BLOCK, tagId)));
		}

		if (value.indexOf('[') >= 0) {
			return DataResult.error(() -> "Block state strings with properties are not supported in object form; "
					+ "use {\"type\":\"botania:state\",\"state\":{...}} or the positional argument form: " + value);
		}

		ResourceLocation id = ResourceLocation.tryParse(value);

		if (id == null) {
			return DataResult.error(() -> "Invalid block id: " + value);
		}

		Block block = BuiltInRegistries.BLOCK.get(id);

		if (block == Blocks.AIR && !id.equals(ResourceLocation.withDefaultNamespace("air"))) {
			return DataResult.error(() -> "Unknown block: " + value);
		}

		return DataResult.success(StateIngredients.of(block));
	}
}
