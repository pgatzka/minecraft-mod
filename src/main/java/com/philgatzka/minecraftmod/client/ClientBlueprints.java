package com.philgatzka.minecraftmod.client;

import com.philgatzka.minecraftmod.blueprint.BlueprintBlocks;
import com.philgatzka.minecraftmod.construction.ConstructionSite;
import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.network.RequestBlueprintPreviewPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Client-side copies of what the server sent: blueprint blocks for the Builder's Wand preview, and the construction sites to outline
public final class ClientBlueprints
{
    public record Preview(BlueprintBlocks blocks, Map<BlockPos, BlockState> visibleBlocks)
    {
    }

    private static final Map<ResourceLocation, Preview> PREVIEWS = new HashMap<>();
    private static final Set<ResourceLocation> REQUESTED = new HashSet<>();
    private static List<ConstructionSite.Outline> sites = List.of();
    // Chunks of the settlement the local player leads (empty if none)
    private static Set<ChunkPos> territory = Set.of();
    // Chunks of all other settlements in this dimension
    private static Set<ChunkPos> otherTerritory = Set.of();

    private ClientBlueprints()
    {
    }

    // The blueprint's preview, asking the server for it (once) if we don't have it yet
    @Nullable
    static Preview preview(ResourceLocation id)
    {
        Preview preview = PREVIEWS.get(id);
        if (preview == null && REQUESTED.add(id))
            ModNetwork.CHANNEL.sendToServer(new RequestBlueprintPreviewPacket(id));
        return preview;
    }

    static void cache(ResourceLocation id, BlueprintBlocks blocks)
    {
        PREVIEWS.put(id, new Preview(blocks, blocks.visibleBlocks()));
    }

    static List<ConstructionSite.Outline> sites()
    {
        return sites;
    }

    static void setSites(List<ConstructionSite.Outline> newSites)
    {
        sites = List.copyOf(newSites);
    }

    static Set<ChunkPos> territory()
    {
        return territory;
    }

    static Set<ChunkPos> otherTerritory()
    {
        return otherTerritory;
    }

    static void setTerritory(Set<ChunkPos> chunks, Set<ChunkPos> others)
    {
        territory = Set.copyOf(chunks);
        otherTerritory = Set.copyOf(others);
    }

    // On disconnect: the next server may have different blueprints and sites
    static void clear()
    {
        PREVIEWS.clear();
        REQUESTED.clear();
        sites = List.of();
        territory = Set.of();
        otherTerritory = Set.of();
    }
}
