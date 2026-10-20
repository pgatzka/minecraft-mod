package com.philgatzka.minecraftmod.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// A settlement member as the settlement sees it; kept even while the settler's chunk is unloaded
public record SettlerRecord(UUID entityId, String firstName, String lastName, @Nullable SettlerJob job, @Nullable BlockPos workplace)
{
    public String fullName()
    {
        return firstName + " " + lastName;
    }

    public SettlerRecord withJob(@Nullable SettlerJob job, @Nullable BlockPos workplace)
    {
        return new SettlerRecord(entityId, firstName, lastName, job, workplace);
    }

    public boolean worksAt(BlockPos pos)
    {
        return pos.equals(workplace);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Entity", entityId);
        tag.putString("FirstName", firstName);
        tag.putString("LastName", lastName);
        if (job != null && workplace != null)
        {
            tag.putString("Job", job.name());
            tag.put("Workplace", NbtUtils.writeBlockPos(workplace));
        }
        return tag;
    }

    public static SettlerRecord load(CompoundTag tag)
    {
        SettlerJob job = tag.contains("Job") ? SettlerJob.valueOf(tag.getString("Job")) : null;
        BlockPos workplace = tag.contains("Workplace") ? NbtUtils.readBlockPos(tag.getCompound("Workplace")) : null;
        return new SettlerRecord(tag.getUUID("Entity"), tag.getString("FirstName"), tag.getString("LastName"), job, workplace);
    }
}
