package dev.kubejsbotania.neo.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe;
import dev.kubejsbotania.neo.recipe.TerraPlatePosition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.client.integration.emi.ManaWidget;

import java.util.List;

/**
 * Native EMI representation of an extended Terra Plate recipe.
 *
 * <p>Layout is the same as the JEI page of this mod (176x172):</p>
 * <ul>
 *     <li>left: input ingredients (3x4 grid) and the output slot;</li>
 *     <li>centre: the Terrestrial Agglomeration Plate catalyst slot (the workstation);</li>
 *     <li>bottom-left: the {@code below} block/fluid pattern (3x3 grid);</li>
 *     <li>bottom-right: the {@code result_blocks} of a successful craft (3x3 grid);</li>
 *     <li>bottom: Botania's own mana bar, rendered by Botania's public EMI
 *     {@link ManaWidget} (which internally calls {@code HUDHandler.renderManaBar}).</li>
 * </ul>
 *
 * @author KubeJS Botania Neo
 */
public class TerraPlateExtensionEmiRecipe implements EmiRecipe {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 172;
	private static final int CELL = 18;

	private static final int INPUT_X = 10;
	private static final int INPUT_Y = 22;
	private static final int INPUT_COLUMNS = 3;
	private static final int INPUT_ROWS = 4;

	private static final int OUTPUT_X = 142;
	private static final int OUTPUT_Y = 40;

	private static final int PLATE_X = 96;
	private static final int PLATE_Y = 40;

	private static final int BELOW_X = 10;
	private static final int RESULT_X = 94;
	private static final int GRID_Y = 108;
	private static final int LABEL_Y = 96;

	private static final int MANA_WIDTH = 102;
	private static final int MANA_HEIGHT = 5;
	private static final int MANA_X = (WIDTH - MANA_WIDTH) / 2;
	private static final int MANA_Y = HEIGHT - MANA_HEIGHT - 1;
	/**
	 * Same reference value the JEI page of this mod uses (Botania's own JEI agglomeration page also uses
	 * 100000 for its mana bar).
	 */
	private static final int MANA_DISPLAY_MAX = 100000;

	private final EmiRecipeCategory category;
	private final RecipeHolder<TerrestrialAgglomerationRecipe> holder;
	private final TerraPlateExtensionRecipe recipe;
	private final List<EmiIngredient> inputs;
	private final List<EmiStack> outputs;

	public TerraPlateExtensionEmiRecipe(EmiRecipeCategory category,
			RecipeHolder<TerrestrialAgglomerationRecipe> holder) {
		this.category = category;
		this.holder = holder;
		this.recipe = (TerraPlateExtensionRecipe) holder.value();
		this.inputs = recipe.getIngredients().stream().map(EmiIngredient::of).toList();
		this.outputs = List.of(EmiStack.of(recipe.getOutput()));
	}

	/**
	 * The Terrestrial Agglomeration Plate item stack, used as category icon / workstation / catalyst slot.
	 */
	public static ItemStack plateStack() {
		Item plate = BuiltInRegistries.ITEM.get(
				ResourceLocation.fromNamespaceAndPath("botania", "terrestrial_agglomeration_plate"));
		return new ItemStack(plate == null ? Items.AIR : plate);
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return category;
	}

	@Override
	public ResourceLocation getId() {
		return holder.id();
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return inputs;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return outputs;
	}

	@Override
	public int getDisplayWidth() {
		return WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return HEIGHT;
	}

	@Override
	public RecipeHolder<?> getBackingRecipe() {
		return holder;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		List<Ingredient> ingredients = recipe.getIngredients();
		int columns = Math.min(INPUT_COLUMNS, Math.max(1, ingredients.size()));

		for (int i = 0; i < Math.min(ingredients.size(), INPUT_COLUMNS * INPUT_ROWS); i++) {
			int column = i % columns;
			int row = i / columns;
			widgets.addSlot(inputs.get(i), INPUT_X + column * CELL, INPUT_Y + row * CELL);
		}

		widgets.addSlot(outputs.getFirst(), OUTPUT_X, OUTPUT_Y).recipeContext(this);

		// The workstation: Terrestrial Agglomeration Plate.
		widgets.addSlot(EmiStack.of(plateStack()), PLATE_X, PLATE_Y).catalyst(true);
		widgets.addText(Component.translatable("jei.kubejs_botania_neo.workstation"),
				PLATE_X - 2, PLATE_Y - 10, 0xFF404040, false);

		addPatternWidgets(widgets, recipe.getBelow(), BELOW_X);
		addPatternWidgets(widgets, recipe.getResultBlocks(), RESULT_X);

		if (!recipe.getBelow().isEmpty()) {
			widgets.addText(Component.translatable("jei.kubejs_botania_neo.below"),
					BELOW_X, LABEL_Y, 0xFF404040, false);
		}

		if (!recipe.getResultBlocks().isEmpty()) {
			widgets.addText(Component.translatable("jei.kubejs_botania_neo.result_blocks"),
					RESULT_X, LABEL_Y, 0xFF404040, false);
			widgets.addText(Component.literal("=>"),
					BELOW_X + 3 * CELL + 6, GRID_Y + CELL + 5, 0xFF404040, false);
		}

		// Botania's own mana bar (same widget Botania's own EMI agglomeration page uses).
		widgets.add(new ManaWidget(MANA_X, MANA_Y, recipe.getMana(), MANA_DISPLAY_MAX));
	}

	private static void addPatternWidgets(WidgetHolder widgets, List<TerraPlatePosition> positions, int gridX) {
		for (TerraPlatePosition position : positions) {
			if (position.x() < -1 || position.x() > 1 || position.z() < -1 || position.z() > 1) {
				continue;
			}

			EmiStack stack = stackOf(position);

			if (!stack.isEmpty()) {
				widgets.addSlot(stack, gridX + (position.x() + 1) * CELL, GRID_Y + (position.z() + 1) * CELL);
			}
		}
	}

	private static EmiStack stackOf(TerraPlatePosition position) {
		if (position.fluid() != null) {
			ResourceLocation id = ResourceLocation.tryParse(position.fluid());

			if (id == null) {
				return EmiStack.EMPTY;
			}

			Fluid fluid = BuiltInRegistries.FLUID.get(id);
			return fluid == null || fluid == Fluids.EMPTY ? EmiStack.EMPTY : EmiStack.of(fluid);
		}

		if (position.block() == null) {
			return EmiStack.EMPTY;
		}

		ItemStack display = position.block().startsWith("#")
				? firstBlockOfTag(position.block().substring(1))
				: itemOfBlock(position.block());

		return display.isEmpty() ? EmiStack.EMPTY : EmiStack.of(display);
	}

	private static ItemStack itemOfBlock(String blockId) {
		ResourceLocation id = ResourceLocation.tryParse(blockId);

		if (id == null) {
			return ItemStack.EMPTY;
		}

		Block block = BuiltInRegistries.BLOCK.get(id);
		return block == Blocks.AIR ? ItemStack.EMPTY : new ItemStack(block.asItem());
	}

	private static ItemStack firstBlockOfTag(String tagId) {
		ResourceLocation id = ResourceLocation.tryParse(tagId);

		if (id == null) {
			return ItemStack.EMPTY;
		}

		return BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, id))
				.flatMap(holders -> holders.stream().findFirst())
				.map(holder -> new ItemStack(holder.value().asItem()))
				.orElse(ItemStack.EMPTY);
	}
}
