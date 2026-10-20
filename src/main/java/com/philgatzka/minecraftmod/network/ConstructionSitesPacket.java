package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.client.ClientPacketHandlers;
import com.philgatzka.minecraftmod.construction.ConstructionSite;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

// Server -> client: all construction sites in the player's dimension, so the client can outline them
public record ConstructionSitesPacket(List<ConstructionSite.Outline> sites)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeCollection(sites, (b, site) -> site.write(b));
    }

    public static ConstructionSitesPacket decode(FriendlyByteBuf buf)
    {
        return new ConstructionSitesPacket(buf.readList(ConstructionSite.Outline::read));
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        // Keep client-only classes out of the dedicated server
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.updateConstructionSites(sites));
    }
}
