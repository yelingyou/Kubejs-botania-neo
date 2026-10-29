package dev.kubejsbotania.neo.kubejs.component;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import dev.kubejsbotania.neo.KubeJSBotaniaNeo;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentValue;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * List of item stacks with script-friendly codec and object-form rewrite support.
 *
 * <p>Used by {@code botania:elven_trade}'s output list. Same rationale as
 * {@link FlexibleIngredientListComponent}: object-form values must be re-encoded into strict item stack JSON.</p>
 *
 * @author KubeJS Botania Neo
 */
public record FlexibleItemStackListComponent(RecipeComponentType<?> type) implements RecipeComponent<List<ItemStack>> {
	public static final RecipeComponentType<List<ItemStack>> TYPE = RecipeComponentType.unit(
			ResourceLocation.fromNamespaceAndPath(KubeJSBotaniaNeo.MOD_ID, "item_stack_list"),
			FlexibleItemStackListComponent::new);

	public static final FlexibleItemStackListComponent INSTANCE = new FlexibleItemStackListComponent(TYPE);

	private static final FlexibleItemStackComponent INNER = new FlexibleItemStackComponent(ItemStackComponent.ITEM_STACK);

	@Override
	public RecipeComponentType<?> type() {
		return type;
	}

	@Override
	public Codec<List<ItemStack>> codec() {
		return KubeJSCodecs.ITEM_STACK.listOf();
	}

	@Override
	public TypeInfo typeInfo() {
		return TypeInfo.RAW_LIST.withParams(ItemWrapper.TYPE_INFO);
	}

	@Override
	public List<ItemStack> wrap(RecipeScriptContext cx, Object from) {
		return ListRecipeComponent.wrap0(cx, INNER, from);
	}

	@Override
	public boolean hasPriority(dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext cx, Object from) {
		return from instanceof Iterable<?> || from != null && from.getClass().isArray();
	}

	@Override
	public void readFromJson(KubeRecipe recipe, RecipeComponentValue<List<ItemStack>> value, JsonObject json) {
		ComponentIo.readAndMarkForRewrite(this, recipe, value, json);
	}

	/**
	 * Bounds API compatibility with KubeJS's {@code ListRecipeComponent} (the original schemas call
	 * {@code withBounds(...)}); list sizes are validated by the Botania recipe serializers themselves.
	 */
	public RecipeComponent<List<ItemStack>> withBounds(dev.latvian.mods.kubejs.util.IntBounds bounds) {
		return this;
	}

	@Override
	public boolean isEmpty(List<ItemStack> value) {
		// Empty lists are legal for Botania's optional ingredient lists (e.g. runic altar catalysts).
		return false;
	}

	@Override
	public String toString() {
		return type.toString();
	}
}
