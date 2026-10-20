package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

// Server -> client: the player's blueprint selection changed (null corners = not set), so the client can outline it
public record SelectionSyncPacket(@Nullable BlockPos first, @Nullable BlockPos second)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeNullable(first, FriendlyByteBuf::writeBlockPos);
        buf.writeNullable(second, FriendlyByteBuf::writeBlockPos);
    }

    public static SelectionSyncPacket decode(FriendlyByteBuf buf)
    {
        return new SelectionSyncPacket(buf.readNullable(FriendlyByteBuf::readBlockPos), buf.readNullable(FriendlyByteBuf::readBlockPos));
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        // Keep client-only classes out of the dedicated server
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.updateSelection(first, second));
    }
}
