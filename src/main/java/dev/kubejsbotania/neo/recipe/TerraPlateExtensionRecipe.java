package dev.kubejsbotania.neo.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import vazkii.botania.api.recipe.ProcessingRecipeInput;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.common.crafting.recipe.RecipeUtils;
import vazkii.botania.xplat.XplatAbstractions;

import java.util.Arrays;
import java.util.List;

/**
 * Extended Terra Plate (Terrestrial Agglomeration Plate) recipe.
 * <p>
 * In addition to the usual {@code ingredients}/{@code mana}/{@code output} it supports a custom
 * {@code below} pattern and per-position {@code result_blocks} transformation after crafting.
 * <p>
 * This class intentionally implements Botania's {@link TerrestrialAgglomerationRecipe} interface (and therefore
 * inherits the default {@code getType()} pointing at {@code botania:terrestrial_agglomeration_plate}) so that
 * Botania itself indexes these recipes in its own recipe type. That makes them work with Botania's cached recipe
 * lookup, Botania's terra plate input item cache and Botania's recipe viewers without extra patches; only the
 * platform validation and the block transformation are injected with Mixin.
 * <p>
 * {@link #matches(ProcessingRecipeInput, Level)} is gated on {@link TerraPlateExtensionEngine#isActive} when this
 * recipe has a custom pattern, so it can never steal recipes from a normal terra plate platform.
 *
 * @author KubeJS Botania Neo
 */
public class TerraPlateExtensionRecipe implements TerrestrialAgglomerationRecipe {
	public static final int DEFAULT_MANA = 500000;

	public static final RecipeSerializer<TerraPlateExtensionRecipe> SERIALIZER = new Serializer();

	private final NonNullList<Ingredient> ingredients;
	private final int mana;
	private final ItemStack output;
	private final List<TerraPlatePosition> below;
	private final List<TerraPlatePosition> resultBlocks;
	private final boolean requireSpecialMatching;

	public TerraPlateExtensionRecipe(List<Ingredient> ingredients, int mana, ItemStack output,
			List<TerraPlatePosition> below, List<TerraPlatePosition> resultBlocks) {
		this.ingredients = NonNullList.of(Ingredient.EMPTY, ingredients.toArray(Ingredient[]::new));
		this.mana = mana;
		this.output = output;
		this.below = List.copyOf(below);
		this.resultBlocks = List.copyOf(resultBlocks);
		this.requireSpecialMatching = Arrays.stream(this.ingredients.toArray(Ingredient[]::new))
				.anyMatch(XplatAbstractions.INSTANCE::requiresCustomTesting);
	}

	@Override
	public int getMana() {
		return mana;
	}

	public ItemStack getOutput() {
		return output;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return ingredients;
	}

	public List<TerraPlatePosition> getBelow() {
		return below;
	}

	public List<TerraPlatePosition> getResultBlocks() {
		return resultBlocks;
	}

	/**
	 * Whether this recipe defines its own platform pattern. Recipes without a pattern fall back to
	 * Botania's default multiblock platform.
	 */
	public boolean hasCustomPattern() {
		return !below.isEmpty();
	}

	/**
	 * Pure item matching, not gated on the active terra plate context. Used by the platform validation.
	 */
	public boolean matchesInput(ProcessingRecipeInput input) {
		return RecipeUtils.matches(this, input, requireSpecialMatching);
	}

	@Override
	public boolean matches(ProcessingRecipeInput input, Level level) {
		if (hasCustomPattern() && !TerraPlateExtensionEngine.isActive(this)) {
			// A custom pattern recipe may only be selected while Botania validates the plate that matched it.
			return false;
		}

		return matchesInput(input);
	}

	@Override
	public ItemStack assemble(ProcessingRecipeInput input, HolderLookup.Provider registries) {
		return output.copy();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return output;
	}

	@Override
	public RecipeSerializer<? extends TerraPlateExtensionRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public String toString() {
		return "TerraPlateExtensionRecipe[" + ingredients.size() + " ingredients, " + mana + " mana, " + output + "]";
	}

	public static class Serializer implements RecipeSerializer<TerraPlateExtensionRecipe> {
		public static final MapCodec<TerraPlateExtensionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
						.forGetter(recipe -> recipe.ingredients),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("mana", DEFAULT_MANA)
						.forGetter(TerraPlateExtensionRecipe::getMana),
				ItemStack.STRICT_CODEC.fieldOf("output")
						.forGetter(TerraPlateExtensionRecipe::getOutput),
				TerraPlatePosition.LIST_CODEC.optionalFieldOf("below", List.of())
						.forGetter(TerraPlateExtensionRecipe::getBelow),
				TerraPlatePosition.LIST_CODEC.optionalFieldOf("result_blocks", List.of())
						.forGetter(TerraPlateExtensionRecipe::getResultBlocks)
		).apply(instance, TerraPlateExtensionRecipe::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, TerraPlateExtensionRecipe> STREAM_CODEC = StreamCodec.composite(
				Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.ingredients,
				ByteBufCodecs.VAR_INT, TerraPlateExtensionRecipe::getMana,
				ItemStack.STREAM_CODEC, TerraPlateExtensionRecipe::getOutput,
				TerraPlatePosition.STREAM_CODEC.apply(ByteBufCodecs.list()), TerraPlateExtensionRecipe::getBelow,
				TerraPlatePosition.STREAM_CODEC.apply(ByteBufCodecs.list()), TerraPlateExtensionRecipe::getResultBlocks,
				TerraPlateExtensionRecipe::new);

		@Override
		public MapCodec<TerraPlateExtensionRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, TerraPlateExtensionRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
