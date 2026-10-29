package dev.kubejsbotania.neo.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe;
import dev.kubejsbotania.neo.recipe.TerraPlatePosition;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import vazkii.botania.client.gui.HUDHandler;

import java.util.List;

/**
 * The JEI category of the extended Terra Plate.
 *
 * <p>Layout (176 x 172):</p>
 * <ul>
 *     <li>left: the input ingredients (up to 3x4) and the output item;</li>
 *     <li>bottom-left: the {@code below} block pattern rendered as a 3x3 grid (positions with x/z in -1..1);
 *     blocks are shown as their item, block tags as a representative block of the tag, fluids as fluid slots;</li>
 *     <li>bottom-right: the {@code result_blocks} of a successful craft, rendered the same way;</li>
 *     <li>bottom: Botania's own mana bar (same texture and {@code mana / 100000} ratio as Botania's
 *     terrestrial agglomeration page), drawn by calling Botania's public
 *     {@link HUDHandler#renderManaBar(GuiGraphics, int, int, int, float, int, int)}.</li>
 * </ul>
 *
 * @author KubeJS Botania Neo
 */
public class TerraPlateExtensionRecipeCategory implements IRecipeCategory<TerraPlateExtensionRecipe> {
	private static final int WIDTH = 176;
	private static final int HEIGHT = 172;
	private static final int CELL = 18;

	private static final int INPUT_X = 10;
	private static final int INPUT_Y = 22;
	private static final int INPUT_COLUMNS = 3;
	private static final int INPUT_ROWS = 4;

	private static final int OUTPUT_X = 142;
	private static final int OUTPUT_Y = 40;

	private static final int BELOW_X = 10;
	private static final int RESULT_X = 94;
	private static final int GRID_Y = 108;
	private static final int LABEL_Y = 96;

	/**
	 * Botania's mana bar is 102x5 pixels; {@code renderManaBar} uses exactly that size.
	 */
	private static final int MANA_BAR_WIDTH = 102;
	private static final int MANA_BAR_HEIGHT = 5;
	private static final int MANA_BAR_X = (WIDTH - MANA_BAR_WIDTH) / 2;
	private static final int MANA_BAR_Y = HEIGHT - MANA_BAR_HEIGHT - 1;
	private static final int MANA_COLOR = 0x0000FF;
	private static final float MANA_ALPHA = 0.75F;
	/**
	 * Same reference value Botania's own terrestrial agglomeration JEI category passes to
	 * {@code HUDHandler.renderManaBar}: the fill ratio is {@code recipe mana / 100000}.
	 */
	private static final int MANA_DISPLAY_MAX = 100000;

	private final IDrawable background;
	private final IDrawable icon;
	private final Component title;

	public TerraPlateExtensionRecipeCategory(IGuiHelper guiHelper) {
		this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
		Item plate = BuiltInRegistries.ITEM.get(
				ResourceLocation.fromNamespaceAndPath("botania", "terrestrial_agglomeration_plate"));
		this.icon = guiHelper.createDrawableItemStack(new ItemStack(plate == null ? Items.AIR : plate));
		this.title = Component.translatable("jei.kubejs_botania_neo.terra_plate");
	}

	@Override
	public RecipeType<TerraPlateExtensionRecipe> getRecipeType() {
		return TerraPlateExtensionJeiPlugin.RECIPE_TYPE;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public IDrawable getBackground() {
		return background;
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, TerraPlateExtensionRecipe recipe, IFocusGroup focuses) {
		List<Ingredient> ingredients = recipe.getIngredients();
		int columns = Math.min(INPUT_COLUMNS, Math.max(1, ingredients.size()));

		for (int i = 0; i < Math.min(ingredients.size(), INPUT_COLUMNS * INPUT_ROWS); i++) {
			int column = i % columns;
			int row = i / columns;
			int x = INPUT_X + column * CELL;
			int y = INPUT_Y + row * CELL;

			builder.addSlot(RecipeIngredientRole.INPUT, x, y)
					.setStandardSlotBackground()
					.addIngredients(ingredients.get(i));
		}

		builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
				.setOutputSlotBackground()
				.addItemStack(recipe.getOutput());

		addPatternSlots(builder, recipe.getBelow(), BELOW_X);
		addPatternSlots(builder, recipe.getResultBlocks(), RESULT_X);
	}

	/**
	 * Adds a slot for every in-grid (x/z in -1..1) position. Positions outside of the 3x3 preview grid are
	 * skipped in the UI (they still work in game).
	 */
	private static void addPatternSlots(IRecipeLayoutBuilder builder, List<TerraPlatePosition> positions, int gridX) {
		for (TerraPlatePosition position : positions) {
			if (position.x() < -1 || position.x() > 1 || position.z() < -1 || position.z() > 1) {
				continue;
			}

			int x = gridX + (position.x() + 1) * CELL;
			int y = GRID_Y + (position.z() + 1) * CELL;

			IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, y)
					.setStandardSlotBackground();

			fillSlot(slot, position);
		}
	}

	private static void fillSlot(IRecipeSlotBuilder slot, TerraPlatePosition position) {
		if (position.fluid() != null) {
			ResourceLocation id = ResourceLocation.tryParse(position.fluid());

			if (id != null) {
				Fluid fluid = BuiltInRegistries.FLUID.get(id);
				slot.addFluidStack(fluid, 1000);
				slot.setFluidRenderer(1000, false, 16, 16);
			}

			return;
		}

		if (position.block() == null) {
			return;
		}

		ItemStack display = position.block().startsWith("#")
				? firstBlockOfTag(position.block().substring(1))
				: itemOfBlock(position.block());

		if (!display.isEmpty()) {
			slot.addItemStack(display);
		}
	}

	private static ItemStack itemOfBlock(String blockId) {
		ResourceLocation id = ResourceLocation.tryParse(blockId);

		if (id == null) {
			return ItemStack.EMPTY;
		}

		Block block = BuiltInRegistries.BLOCK.get(id);

		if (block == Blocks.AIR) {
			return ItemStack.EMPTY;
		}

		return new ItemStack(block.asItem());
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

	@Override
	public void draw(TerraPlateExtensionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
			double mouseX, double mouseY) {
		var font = Minecraft.getInstance().font;

		if (!recipe.getBelow().isEmpty()) {
			guiGraphics.drawString(font, Component.translatable("jei.kubejs_botania_neo.below"),
					BELOW_X, LABEL_Y, 0xFF404040, false);
		}

		if (!recipe.getResultBlocks().isEmpty()) {
			guiGraphics.drawString(font, Component.translatable("jei.kubejs_botania_neo.result_blocks"),
					RESULT_X, LABEL_Y, 0xFF404040, false);

			guiGraphics.drawString(font, Component.literal("=>"),
					BELOW_X + 3 * CELL + 6, GRID_Y + CELL + 5, 0xFF404040, false);
		}

		// Botania's own mana bar - same texture, colours, alpha and fill ratio as Botania's
		// terrestrial agglomeration JEI page (see TerrestrialAgglomerationRecipeCategory#draw).
		RenderSystem.enableBlend();

		try {
			HUDHandler.renderManaBar(guiGraphics, MANA_BAR_X, MANA_BAR_Y, MANA_COLOR, MANA_ALPHA,
					recipe.getMana(), MANA_DISPLAY_MAX);
		} finally {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
		}
	}
}
