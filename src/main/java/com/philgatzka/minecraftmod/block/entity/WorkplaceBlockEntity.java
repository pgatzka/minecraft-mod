package com.philgatzka.minecraftmod.block.entity;

import com.philgatzka.minecraftmod.settlement.SettlerJob;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// A settlement building that employs one settler in a job. Who works here is stored in the settlement (SettlerRecord.workplace).
public abstract class WorkplaceBlockEntity extends BlockEntity
{
    @Nullable
    private UUID settlementId;

    protected WorkplaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    public abstract SettlerJob getJob();

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
