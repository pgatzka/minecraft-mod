package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Server -> client: an action from a screen was rejected, show why in that screen
public record ScreenErrorPacket(Component message)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeComponent(message);
    }

    public static ScreenErrorPacket decode(FriendlyByteBuf buf)
    {
        return new ScreenErrorPacket(buf.readComponent());
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        // Keep client-only classes out of the dedicated server
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.showScreenError(message));
    }
}
