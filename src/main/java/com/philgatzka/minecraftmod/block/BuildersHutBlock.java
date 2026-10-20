package com.philgatzka.minecraftmod.block;

import com.philgatzka.minecraftmod.block.entity.BuildersHutBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BuildersHutBlock extends WorkplaceBlock
{
    public BuildersHutBlock(Properties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new BuildersHutBlockEntity(pos, state);
    }
}
