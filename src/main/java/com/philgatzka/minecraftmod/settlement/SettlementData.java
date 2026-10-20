package com.philgatzka.minecraftmod.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// All settlements on the server, saved with the overworld in data/minecraftmod_settlements.dat
public class SettlementData extends SavedData
{
    private static final String FILE_NAME = "minecraftmod_settlements";

    private final Map<UUID, Settlement> settlements = new LinkedHashMap<>();

    public static SettlementData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(SettlementData::load, SettlementData::new, FILE_NAME);
    }

    public Optional<Settlement> get(UUID id)
    {
        return Optional.ofNullable(settlements.get(id));
    }

    public Collection<Settlement> all()
    {
        return Collections.unmodifiableCollection(settlements.values());
    }

    public Optional<Settlement> getByLeader(UUID leaderId)
    {
        return settlements.values().stream().filter(settlement -> settlement.leaderId().equals(leaderId)).findFirst();
    }

    public Settlement create(String name, UUID leaderId, String leaderName, ResourceKey<Level> dimension, BlockPos townHallPos)
    {
        Settlement settlement = new Settlement(UUID.randomUUID(), name, leaderId, leaderName, dimension, townHallPos);
        settlements.put(settlement.id(), settlement);
        setDirty();
        return settlement;
    }

    // Deletes a settlement record (its blocks and settlers stay in the world). Used by GameTests to start from a clean slate.
    public void remove(UUID id)
    {
        if (settlements.remove(id) != null)
            setDirty();
    }

    public void setTownHall(Settlement settlement, @Nullable BlockPos townHallPos)
    {
        settlement.setTownHallPos(townHallPos);
        setDirty();
    }

    public void addBuilding(Settlement settlement, BlockPos pos)
    {
        if (settlement.addBuilding(pos))
            setDirty();
    }

    public void removeBuilding(Settlement settlement, BlockPos pos)
    {
        if (settlement.removeBuilding(pos))
            setDirty();
    }

    public void addSettler(Settlement settlement, SettlerRecord settler)
    {
        settlement.putSettler(settler);
        setDirty();
    }

    public void removeSettler(Settlement settlement, UUID entityId)
    {
        if (settlement.removeSettler(entityId))
            setDirty();
    }

    public void setJob(Settlement settlement, UUID entityId, @Nullable SettlerJob job, @Nullable BlockPos workplace)
    {
        settlement.settler(entityId).ifPresent(settler -> {
            settlement.putSettler(settler.withJob(job, workplace));
            setDirty();
        });
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        settlements.values().forEach(settlement -> list.add(settlement.save()));
        tag.put("Settlements", list);
        return tag;
    }

    private static SettlementData load(CompoundTag tag)
    {
        SettlementData data = new SettlementData();
        ListTag list = tag.getList("Settlements", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            Settlement settlement = Settlement.load(list.getCompound(i));
            data.settlements.put(settlement.id(), settlement);
        }
        return data;
    }
}
