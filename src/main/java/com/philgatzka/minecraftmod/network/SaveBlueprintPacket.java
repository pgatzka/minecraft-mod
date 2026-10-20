package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.blueprint.BlueprintCommands;
import com.philgatzka.minecraftmod.blueprint.Blueprints;
import com.philgatzka.minecraftmod.menu.BlueprintMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// Client -> server: "save the area from my open Save Blueprint screen under this name"
public record SaveBlueprintPacket(String name)
{
    // Generous upper bound so a modified client can't send huge strings; the real limit is checked in Blueprints
    private static final int MAX_WIRE_LENGTH = 256;

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(name, MAX_WIRE_LENGTH);
    }

    public static SaveBlueprintPacket decode(FriendlyByteBuf buf)
    {
        return new SaveBlueprintPacket(buf.readUtf(MAX_WIRE_LENGTH));
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        ServerPlayer player = context.get().getSender();
        if (player == null || !(player.containerMenu instanceof BlueprintMenu menu) || !Blueprints.canUse(player))
            return;

        Blueprints.SaveResult result = Blueprints.save(player.serverLevel(), menu.getInfo().selection(), name, player.getGameProfile().getName());
        if (result.isSuccess())
        {
            player.closeContainer();
            player.displayClientMessage(BlueprintCommands.savedMessage(result), false);
        }
        else
        {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScreenErrorPacket(result.error()));
        }
    }
}
