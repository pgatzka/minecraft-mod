package com.philgatzka.minecraftmod.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// All construction sites on the server, saved with the overworld in data/minecraftmod_construction_sites.dat
public class ConstructionSiteData extends SavedData
{
    private static final String FILE_NAME = "minecraftmod_construction_sites";

    private final Map<UUID, ConstructionSite> sites = new LinkedHashMap<>();

    public static ConstructionSiteData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(ConstructionSiteData::load, ConstructionSiteData::new, FILE_NAME);
    }

    public List<ConstructionSite> all()
    {
        return List.copyOf(sites.values());
    }

    public List<ConstructionSite> in(ResourceKey<Level> dimension)
    {
        return sites.values().stream().filter(site -> site.dimension().equals(dimension)).toList();
    }

    public Optional<ConstructionSite> atCityBlock(ResourceKey<Level> dimension, BlockPos pos)
    {
        return sites.values().stream().filter(site -> site.dimension().equals(dimension) && site.cityBlockPos().equals(pos)).findFirst();
    }

    public void add(ConstructionSite site)
    {
        sites.put(site.id(), site);
        setDirty();
    }

    public void remove(UUID id)
    {
        if (sites.remove(id) != null)
            setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        sites.values().forEach(site -> list.add(site.save()));
        tag.put("Sites", list);
        return tag;
    }

    private static ConstructionSiteData load(CompoundTag tag)
    {
        ConstructionSiteData data = new ConstructionSiteData();
        ListTag list = tag.getList("Sites", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            ConstructionSite site = ConstructionSite.load(list.getCompound(i));
            data.sites.put(site.id(), site);
        }
        return data;
    }
}
