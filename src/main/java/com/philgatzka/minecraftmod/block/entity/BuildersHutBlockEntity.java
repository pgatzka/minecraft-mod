package com.philgatzka.minecraftmod.block.entity;

import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.settlement.SettlerJob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BuildersHutBlockEntity extends WorkplaceBlockEntity
{
    public BuildersHutBlockEntity(BlockPos pos, BlockState state)
    {
        super(MinecraftMod.BUILDERS_HUT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public SettlerJob getJob()
    {
        return SettlerJob.BUILDER;
    }
}
