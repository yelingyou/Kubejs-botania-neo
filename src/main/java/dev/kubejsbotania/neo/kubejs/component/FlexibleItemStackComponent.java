package dev.kubejsbotania.neo.kubejs.component;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentValue;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.world.item.ItemStack;

/**
 * Item stack component with:
 * <ul>
 *     <li>a script-friendly codec ({@code 'minecraft:diamond'}, page-style {@code {item,count}}, strict JSON);</li>
 *     <li>object-form support: decoded values are marked for re-serialisation so KubeJS rewrites them into the
 *     canonical {@code {"id":...}} JSON that recipe serializers expect.</li>
 * </ul>
 *
 * @author KubeJS Botania Neo
 */
public record FlexibleItemStackComponent(RecipeComponentType<?> type) implements RecipeComponent<ItemStack> {
	public static final TypeInfo TYPE_INFO = ItemWrapper.TYPE_INFO;

	@Override
	public RecipeComponentType<?> type() {
		return type;
	}

	@Override
	public Codec<ItemStack> codec() {
		return KubeJSCodecs.ITEM_STACK;
	}

	@Override
	public TypeInfo typeInfo() {
		return TYPE_INFO;
	}

	@Override
	public void readFromJson(KubeRecipe recipe, RecipeComponentValue<ItemStack> value, JsonObject json) {
		ComponentIo.readAndMarkForRewrite(this, recipe, value, json);
	}

	@Override
	public String toString() {
		return type.toString();
	}
}
