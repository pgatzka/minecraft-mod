package com.philgatzka.minecraftmod.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;

// A box of blocks between two corners (both inclusive), with min <= max on every axis
public record BlueprintSelection(BlockPos min, BlockPos max)
{
    public static BlueprintSelection of(BlockPos a, BlockPos b)
    {
        return new BlueprintSelection(
                new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
                new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
    }

    public Vec3i size()
    {
        return new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1);
    }

    public boolean fitsMaxSize()
    {
        Vec3i size = size();
        return size.getX() <= Blueprints.MAX_SIZE && size.getY() <= Blueprints.MAX_SIZE && size.getZ() <= Blueprints.MAX_SIZE;
    }

    public boolean intersects(BlueprintSelection other)
    {
        return min.getX() <= other.max.getX() && other.min.getX() <= max.getX()
                && min.getY() <= other.max.getY() && other.min.getY() <= max.getY()
                && min.getZ() <= other.max.getZ() && other.min.getZ() <= max.getZ();
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(min);
        buf.writeBlockPos(max);
    }

    public static BlueprintSelection read(FriendlyByteBuf buf)
    {
        return new BlueprintSelection(buf.readBlockPos(), buf.readBlockPos());
    }
}
