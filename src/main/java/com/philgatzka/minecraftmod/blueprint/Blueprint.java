package com.philgatzka.minecraftmod.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

// A saved structure built around one city block. anchor is the city block's position inside the template.
public record Blueprint(ResourceLocation id, StructureTemplate template, Block cityBlock, BlockPos anchor, BlueprintBlocks blocks)
{
}
