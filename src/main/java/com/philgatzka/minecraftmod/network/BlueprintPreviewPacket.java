package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.blueprint.BlueprintBlocks;
import com.philgatzka.minecraftmod.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Server -> client: a blueprint's blocks, so the Builder's Wand can preview it
public record BlueprintPreviewPacket(ResourceLocation id, BlueprintBlocks blocks)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(id);
        blocks.write(buf);
    }

    public static BlueprintPreviewPacket decode(FriendlyByteBuf buf)
    {
        return new BlueprintPreviewPacket(buf.readResourceLocation(), BlueprintBlocks.read(buf));
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        // Keep client-only classes out of the dedicated server
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.cacheBlueprint(id, blocks));
    }
}
