package com.philgatzka.minecraftmod.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

// Where a blueprint ends up in the world: rotated around its anchor (the city block) so that the anchor lands on cityBlockPos.
// Used by both the client preview and the server, so what you see is what gets marked.
public record BlueprintPlacement(BlockPos cityBlockPos, BlockPos anchor, Vec3i size, Rotation rotation)
{
    // World position of a block at position `relative` inside the (unrotated) blueprint
    public BlockPos worldPos(BlockPos relative)
    {
        return cityBlockPos.offset(StructureTemplate.transform(relative.subtract(anchor), Mirror.NONE, rotation, BlockPos.ZERO));
    }

    // The area the rotated blueprint covers
    public BlueprintSelection area()
    {
        BlockPos far = new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        return BlueprintSelection.of(worldPos(BlockPos.ZERO), worldPos(far));
    }
}
