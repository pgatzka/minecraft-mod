package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.blueprint.Blueprints;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// Client -> server: "I hold a Builder's Wand with this blueprint but don't have its blocks yet" (e.g. after rejoining)
public record RequestBlueprintPreviewPacket(ResourceLocation id)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(id);
    }

    public static RequestBlueprintPreviewPacket decode(FriendlyByteBuf buf)
    {
        return new RequestBlueprintPreviewPacket(buf.readResourceLocation());
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        ServerPlayer player = context.get().getSender();
        if (player == null)
            return;
        Blueprints.load(player.server, id).ifPresent(blueprint ->
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlueprintPreviewPacket(id, blueprint.blocks())));
    }
}
