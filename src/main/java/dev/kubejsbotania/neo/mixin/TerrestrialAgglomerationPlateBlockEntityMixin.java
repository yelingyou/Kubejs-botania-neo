package dev.kubejsbotania.neo.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.kubejsbotania.neo.recipe.TerraPlateExtensionEngine;
import dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.recipe.ProcessingRecipeInput;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.common.block.block_entity.TerrestrialAgglomerationPlateBlockEntity;

import java.util.List;

/**
 * Adds custom platform patterns and post-craft block transformation to Botania's Terra Plate.
 *
 * <p>Verified against the real Botania {@code botania-neoforge-1.21.1-455} bytecode:</p>
 * <ul>
 *     <li>{@code hasValidPlatform()} (private, {@code ()Z}) runs before the item entities are collected by
 *     {@code tryStartProcessing} / {@code serverCollectingTick};</li>
 *     <li>{@code finishCrafting(...)} (private) runs after the output item was spawned, inputs discarded, mana
 *     cleared, block event sent and the block state set to DONE.</li>
 * </ul>
 *
 * <p>The extended recipe itself reuses Botania's real {@code botania:terrestrial_agglomeration_plate} recipe
 * type (its {@code getType()} is inherited from {@link TerrestrialAgglomerationRecipe}), so Botania's cached
 * recipe lookup and its terra plate input item cache handle it automatically. The only thing needed here is
 * to accept the custom platform and to record the matched rotation for the item lookup and post-craft
 * transformation.</p>
 *
 * @author KubeJS Botania Neo
 */
@Mixin(TerrestrialAgglomerationPlateBlockEntity.class)
public abstract class TerrestrialAgglomerationPlateBlockEntityMixin extends BlockEntity {
	private TerrestrialAgglomerationPlateBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Inject(method = "hasValidPlatform", at = @At("HEAD"), cancellable = true)
	private void kubejs_botania_neo$hasValidPlatform(CallbackInfoReturnable<Boolean> cir) {
		Level level = getLevel();

		if (level == null || level.isClientSide()) {
			return;
		}

		BlockPos pos = getBlockPos();
		TerraPlateExtensionEngine.Match match = TerraPlateExtensionEngine.findMatch(level, pos);

		if (match != null) {
			// Make the extended recipe's matches() accept it during the item lookup that Botania performs
			// right after this platform check.
			TerraPlateExtensionEngine.setActive(match.recipe(), match.rotation(), pos);
			cir.setReturnValue(true);
		} else {
			TerraPlateExtensionEngine.clearActive();
		}
	}

	@Inject(
			method = "finishCrafting(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/List;Lvazkii/botania/api/recipe/ProcessingRecipeInput;Lnet/minecraft/world/item/crafting/RecipeHolder;)V",
			at = @At("TAIL")
	)
	private void kubejs_botania_neo$finishCrafting(Level level, BlockPos pos, BlockState state,
			List<ItemEntity> itemEntities, ProcessingRecipeInput recipeInput,
			RecipeHolder<TerrestrialAgglomerationRecipe> recipeHolder, CallbackInfo ci) {
		TerraPlateExtensionEngine.ActiveMatch active = TerraPlateExtensionEngine.getActive();
		Object crafted = recipeHolder.value();

		if (crafted instanceof TerraPlateExtensionRecipe extended) {
			if (extended.hasCustomPattern()) {
				// Custom pattern recipe: only transform when the crafted recipe is exactly the one whose
				// platform matched this tick (never transform for a vanilla recipe).
				if (active != null && active.recipe() == extended) {
					TerraPlateExtensionEngine.applyResultBlocks(level, pos, extended, active.rotation());
				}
			} else {
				// Pattern-less extension recipe crafted on Botania's default platform: no rotation context,
				// apply result_blocks with rotation 0.
				TerraPlateExtensionEngine.applyResultBlocks(level, pos, extended, 0);
			}
		}

		TerraPlateExtensionEngine.clearActive();
	}
}
