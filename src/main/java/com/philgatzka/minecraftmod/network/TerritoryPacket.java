package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

// Server -> client: the chunks of the settlement this player leads (empty = none here), so the client can show the territory
public record TerritoryPacket(Set<ChunkPos> chunks)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeCollection(chunks, (b, chunk) -> b.writeLong(chunk.toLong()));
    }

    public static TerritoryPacket decode(FriendlyByteBuf buf)
    {
        return new TerritoryPacket(buf.readCollection(HashSet::new, b -> new ChunkPos(b.readLong())));
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        // Keep client-only classes out of the dedicated server
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.updateTerritory(chunks));
    }
}
