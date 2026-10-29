package dev.kubejsbotania.neo.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A single position in a Terra Plate pattern. Coordinates are relative to the Terra Plate block
 * (the position is always one block below the plate). Each entry must specify exactly one of
 * {@code block} (block id, or {@code #namespace:tag} for a block tag) or {@code fluid} (fluid id).
 *
 * @author KubeJS Botania Neo
 */
public record TerraPlatePosition(int x, int z, @Nullable String block, @Nullable String fluid) {
	public static final Codec<TerraPlatePosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("x").forGetter(TerraPlatePosition::x),
			Codec.INT.fieldOf("z").forGetter(TerraPlatePosition::z),
			Codec.STRING.optionalFieldOf("block").forGetter(p -> Optional.ofNullable(p.block())),
			Codec.STRING.optionalFieldOf("fluid").forGetter(p -> Optional.ofNullable(p.fluid()))
	).apply(instance, (x, z, block, fluid) -> new TerraPlatePosition(x, z, block.orElse(null), fluid.orElse(null))));

	public static final Codec<TerraPlatePosition> VALIDATED_CODEC = CODEC.validate(pos -> pos.isValid()
			? DataResult.success(pos)
			: DataResult.error(() -> "Terra plate position must have exactly one of 'block' or 'fluid': " + pos));

	/**
	 * Accepts both a plain list of positions and the {@code {"positions":[...]}} object form used by
	 * the original terra-plate-extension mod. Always serializes as a plain list.
	 */
	public static final Codec<List<TerraPlatePosition>> LIST_CODEC = Codec
			.either(VALIDATED_CODEC.listOf(), VALIDATED_CODEC.listOf().fieldOf("positions").codec())
			.xmap(either -> either.map(list -> list, list -> list), Either::left);

	public static final StreamCodec<RegistryFriendlyByteBuf, TerraPlatePosition> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TerraPlatePosition::x,
			ByteBufCodecs.VAR_INT, TerraPlatePosition::z,
			ByteBufCodecs.STRING_UTF8, pos -> pos.block() == null ? "" : pos.block(),
			ByteBufCodecs.STRING_UTF8, pos -> pos.fluid() == null ? "" : pos.fluid(),
			(x, z, block, fluid) -> new TerraPlatePosition(x, z, block.isEmpty() ? null : block, fluid.isEmpty() ? null : fluid));

	public boolean isValid() {
		return (block == null) != (fluid == null);
	}

	public boolean isFluid() {
		return fluid != null;
	}

	public boolean isBlockTag() {
		return block != null && block.startsWith("#");
	}

	/**
	 * Rotates this relative position clockwise by {@code rotation * 90} degrees around the plate.
	 */
	public BlockPos rotatedPos(BlockPos platePos, int rotation) {
		int rx;
		int rz;

		switch (rotation & 3) {
			case 1 -> {
				rx = -z;
				rz = x;
			}
			case 2 -> {
				rx = -x;
				rz = -z;
			}
			case 3 -> {
				rx = z;
				rz = -x;
			}
			default -> {
				rx = x;
				rz = z;
			}
		}

		return platePos.offset(rx, -1, rz);
	}

	/**
	 * Checks if the world at {@code pos} matches this pattern entry.
	 */
	public boolean matches(Level level, BlockPos pos) {
		if (block != null) {
			if (isBlockTag()) {
				ResourceLocation tagId = ResourceLocation.tryParse(block.substring(1));

				if (tagId == null) {
					return false;
				}

				BlockState state = level.getBlockState(pos);
				return state.is(TagKey.create(Registries.BLOCK, tagId));
			}

			ResourceLocation id = ResourceLocation.tryParse(block);

			if (id == null) {
				return false;
			}

			Block expected = BuiltInRegistries.BLOCK.get(id);
			return expected != Blocks.AIR && level.getBlockState(pos).is(expected);
		}

		if (fluid != null) {
			ResourceLocation id = ResourceLocation.tryParse(fluid);

			if (id == null) {
				return false;
			}

			Fluid expected = BuiltInRegistries.FLUID.get(id);
			return level.getFluidState(pos).getType() == expected;
		}

		return false;
	}

	/**
	 * Applies this entry to the world (used by {@code result_blocks}). Tags can not be placed and are skipped.
	 */
	public void apply(Level level, BlockPos pos) {
		if (fluid != null) {
			ResourceLocation id = ResourceLocation.tryParse(fluid);

			if (id == null) {
				return;
			}

			Fluid fluidType = BuiltInRegistries.FLUID.get(id);
			level.setBlock(pos, fluidType.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
			return;
		}

		if (block == null || isBlockTag()) {
			return;
		}

		ResourceLocation id = ResourceLocation.tryParse(block);

		if (id == null) {
			return;
		}

		Block result = BuiltInRegistries.BLOCK.get(id);

		if (result != Blocks.AIR) {
			level.setBlock(pos, result.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	public String toString() {
		return "(" + x + "," + z + ")" + (block != null ? " block=" + block : " fluid=" + fluid);
	}
}
