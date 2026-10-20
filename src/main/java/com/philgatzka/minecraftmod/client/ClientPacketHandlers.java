package com.philgatzka.minecraftmod.client;

import com.philgatzka.minecraftmod.blueprint.BlueprintBlocks;
import com.philgatzka.minecraftmod.client.ui.UiScreen;
import com.philgatzka.minecraftmod.construction.ConstructionSite;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class ClientPacketHandlers
{
    private ClientPacketHandlers()
    {
    }

    public static void showScreenError(Component message)
    {
        if (Minecraft.getInstance().screen instanceof UiScreen<?> screen)
            screen.showError(message);
    }

    public static void updateSelection(@Nullable BlockPos first, @Nullable BlockPos second)
    {
        ClientSelection.set(first, second);
    }

    public static void cacheBlueprint(ResourceLocation id, BlueprintBlocks blocks)
    {
        ClientBlueprints.cache(id, blocks);
    }

    public static void updateConstructionSites(List<ConstructionSite.Outline> sites)
    {
        ClientBlueprints.setSites(sites);
    }

    public static void updateTerritory(Set<ChunkPos> chunks)
    {
        ClientBlueprints.setTerritory(chunks);
    }
}
