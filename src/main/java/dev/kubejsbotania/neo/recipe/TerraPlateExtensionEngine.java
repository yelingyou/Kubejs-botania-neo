package dev.kubejsbotania.neo.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.recipe.ProcessingRecipeInput;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;
import vazkii.botania.common.crafting.recipe.RecipeUtils;

import java.util.List;

/**
 * Matching/transformation logic for extended Terra Plate recipes. Kept free of Mixin code so it can be
 * unit tested and reused by the recipe viewer integration.
 *
 * <h2>Why the thread local?</h2>
 * Botania's terra plate only gives {@code hasValidPlatform()} a chance to inspect the world before it looks
 * up the recipe by items. Our extended recipe may only match when its custom block pattern matched as well,
 * so {@code hasValidPlatform()} records the current match in {@link #ACTIVE} and
 * {@link TerraPlateExtensionRecipe#matches} only accepts the recipe while that context is active.
 * {@code RecipeManager.CachedCheck} does not cache input matches, so this is safe (verified against the
 * real Botania 455 bytecode).
 *
 * @author KubeJS Botania Neo
 */
public final class TerraPlateExtensionEngine {
	private TerraPlateExtensionEngine() {
	}

	public record Match(TerraPlateExtensionRecipe recipe, int rotation) {
	}

	/**
	 * Context of the terra plate that is currently being validated by Botania, on the current thread.
	 */
	public record ActiveMatch(TerraPlateExtensionRecipe recipe, int rotation, BlockPos platePos) {
	}

	private static final ThreadLocal<ActiveMatch> ACTIVE = new ThreadLocal<>();

	public static List<ItemEntity> getInputItemEntities(Level level, BlockPos platePos) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(platePos), EntitySelector.ENTITY_STILL_ALIVE);
	}

	/**
	 * Finds the extended recipe matching both the current input item entities and the current block pattern
	 * below the plate. Returns {@code null} if no extended recipe matches.
	 */
	@Nullable
	public static Match findMatch(Level level, BlockPos platePos) {
		List<ItemEntity> items = getInputItemEntities(level, platePos);

		if (items.isEmpty()) {
			return null;
		}

		ProcessingRecipeInput input = RecipeUtils.getInputFromEntities(items);

		if (input.size() <= 0) {
			return null;
		}

		for (RecipeHolder<TerrestrialAgglomerationRecipe> holder : level.getRecipeManager()
				.getAllRecipesFor(BotaniaRecipeTypes.TERRA_PLATE_TYPE)) {
			if (!(holder.value() instanceof TerraPlateExtensionRecipe recipe)) {
				continue;
			}

			if (!recipe.hasCustomPattern()) {
				// Recipes without a custom pattern must keep using Botania's default 3x3 platform
				// validation. Their item-only matches() is accepted by the vanilla recipe lookup after
				// the original hasValidPlatform() passed, so they must not be returned here (otherwise
				// they would bypass the platform check entirely).
				continue;
			}

			int rotation = matchRotation(level, platePos, recipe.getBelow());

			if (rotation < 0) {
				continue;
			}

			if (recipe.matchesInput(input)) {
				return new Match(recipe, rotation);
			}
		}

		return null;
	}

	/**
	 * @return the first rotation (0-3, clockwise 90 degrees each) that matches the pattern, or -1.
	 */
	public static int matchRotation(Level level, BlockPos platePos, List<TerraPlatePosition> pattern) {
		for (int rotation = 0; rotation < 4; rotation++) {
			if (matchesPattern(level, platePos, pattern, rotation)) {
				return rotation;
			}
		}

		return -1;
	}

	public static boolean matchesPattern(Level level, BlockPos platePos, List<TerraPlatePosition> pattern, int rotation) {
		for (TerraPlatePosition position : pattern) {
			if (!position.matches(level, position.rotatedPos(platePos, rotation))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Applies the {@code result_blocks} transformation after a successful craft, using the same rotation that
	 * matched the input pattern.
	 */
	public static void applyResultBlocks(Level level, BlockPos platePos, TerraPlateExtensionRecipe recipe, int rotation) {
		for (TerraPlatePosition position : recipe.getResultBlocks()) {
			position.apply(level, position.rotatedPos(platePos, rotation));
		}
	}

	public static void setActive(TerraPlateExtensionRecipe recipe, int rotation, BlockPos platePos) {
		ACTIVE.set(new ActiveMatch(recipe, rotation, platePos.immutable()));
	}

	public static void clearActive() {
		ACTIVE.remove();
	}

	@Nullable
	public static ActiveMatch getActive() {
		return ACTIVE.get();
	}

	/**
	 * @return {@code true} if {@code recipe} is the recipe currently being validated by Botania on this thread.
	 */
	public static boolean isActive(TerraPlateExtensionRecipe recipe) {
		ActiveMatch active = ACTIVE.get();
		return active != null && active.recipe() == recipe;
	}
}
