package com.philgatzka.minecraftmod.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// A settlement led by exactly one player. Change it only through SettlementData so changes get saved.
public final class Settlement
{
    private final UUID id;
    private final String name;
    private final UUID leaderId;
    private final String leaderName;
    private final ResourceKey<Level> dimension;
    // Null while the settlement has no Town Hall (e.g. it was destroyed)
    @Nullable
    private BlockPos townHallPos;
    private final Map<UUID, SettlerRecord> settlers = new LinkedHashMap<>();
    // City blocks of the settlement's other buildings (Builder's Huts, ...); each one extends the territory
    private final Set<BlockPos> buildings = new LinkedHashSet<>();

    Settlement(UUID id, String name, UUID leaderId, String leaderName, ResourceKey<Level> dimension, @Nullable BlockPos townHallPos)
    {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.leaderName = leaderName;
        this.dimension = dimension;
        this.townHallPos = townHallPos;
    }

    public UUID id()
    {
        return id;
    }

    public String name()
    {
        return name;
    }

    public UUID leaderId()
    {
        return leaderId;
    }

    public String leaderName()
    {
        return leaderName;
    }

    public ResourceKey<Level> dimension()
    {
        return dimension;
    }

    @Nullable
    public BlockPos townHallPos()
    {
        return townHallPos;
    }

    public Collection<SettlerRecord> settlers()
    {
        return Collections.unmodifiableCollection(settlers.values());
    }

    public Set<BlockPos> buildings()
    {
        return Collections.unmodifiableSet(buildings);
    }

    public Optional<SettlerRecord> settler(UUID entityId)
    {
        return Optional.ofNullable(settlers.get(entityId));
    }

    public Optional<SettlerRecord> workerAt(BlockPos workplace)
    {
        return settlers.values().stream().filter(settler -> settler.worksAt(workplace)).findFirst();
    }

    void setTownHallPos(@Nullable BlockPos townHallPos)
    {
        this.townHallPos = townHallPos;
    }

    boolean addBuilding(BlockPos pos)
    {
        return buildings.add(pos.immutable());
    }

    boolean removeBuilding(BlockPos pos)
    {
        return buildings.remove(pos);
    }

    void putSettler(SettlerRecord settler)
    {
        settlers.put(settler.entityId(), settler);
    }

    boolean removeSettler(UUID entityId)
    {
        return settlers.remove(entityId) != null;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putUUID("Leader", leaderId);
        tag.putString("LeaderName", leaderName);
        tag.putString("Dimension", dimension.location().toString());
        if (townHallPos != null)
            tag.put("TownHall", NbtUtils.writeBlockPos(townHallPos));
        ListTag settlerList = new ListTag();
        settlers.values().forEach(settler -> settlerList.add(settler.save()));
        tag.put("Settlers", settlerList);
        ListTag buildingList = new ListTag();
        buildings.forEach(pos -> buildingList.add(NbtUtils.writeBlockPos(pos)));
        tag.put("Buildings", buildingList);
        return tag;
    }

    public static Settlement load(CompoundTag tag)
    {
        Settlement settlement = new Settlement(
                tag.getUUID("Id"),
                tag.getString("Name"),
                tag.getUUID("Leader"),
                tag.getString("LeaderName"),
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension"))),
                tag.contains("TownHall") ? NbtUtils.readBlockPos(tag.getCompound("TownHall")) : null);
        ListTag settlerList = tag.getList("Settlers", Tag.TAG_COMPOUND);
        for (int i = 0; i < settlerList.size(); i++)
            settlement.putSettler(SettlerRecord.load(settlerList.getCompound(i)));
        ListTag buildingList = tag.getList("Buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < buildingList.size(); i++)
            settlement.addBuilding(NbtUtils.readBlockPos(buildingList.getCompound(i)));
        return settlement;
    }
}
