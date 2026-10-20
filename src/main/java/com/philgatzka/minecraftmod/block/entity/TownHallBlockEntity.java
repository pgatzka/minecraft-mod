package com.philgatzka.minecraftmod.block.entity;

import com.philgatzka.minecraftmod.MinecraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TownHallBlockEntity extends BlockEntity
{
    // The settlement founded at this Town Hall, see SettlementData
    @Nullable
    private UUID settlementId;

    public TownHallBlockEntity(BlockPos pos, BlockState state)
    {
        super(MinecraftMod.TOWN_HALL_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public UUID getSettlementId()
    {
        return settlementId;
    }

    public void setSettlementId(@Nullable UUID settlementId)
    {
        this.settlementId = settlementId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        if (settlementId != null)
            tag.putUUID("Settlement", settlementId);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        settlementId = tag.hasUUID("Settlement") ? tag.getUUID("Settlement") : null;
    }
}
