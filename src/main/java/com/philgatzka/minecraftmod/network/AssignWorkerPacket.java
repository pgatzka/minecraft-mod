package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.block.entity.WorkplaceBlockEntity;
import com.philgatzka.minecraftmod.menu.WorkplaceMenu;
import com.philgatzka.minecraftmod.settlement.Settlements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

// Client -> server: "give the workplace I have open to this settler" (null = unassign its worker)
public record AssignWorkerPacket(@Nullable UUID settlerId)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeNullable(settlerId, FriendlyByteBuf::writeUUID);
    }

    public static AssignWorkerPacket decode(FriendlyByteBuf buf)
    {
        return new AssignWorkerPacket(buf.readNullable(FriendlyByteBuf::readUUID));
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        ServerPlayer player = context.get().getSender();
        // Only accept requests for the workplace the player actually has open and is still near
        if (player == null || !(player.containerMenu instanceof WorkplaceMenu menu) || !menu.stillValid(player))
            return;
        BlockPos pos = menu.getInfo().pos();
        if (!(player.level().getBlockEntity(pos) instanceof WorkplaceBlockEntity workplace))
            return;

        Settlements.Result result = Settlements.assignWorker(player, workplace, settlerId);
        if (result.isSuccess())
            WorkplaceMenu.open(player, pos); // reopen so the screen shows the new assignment
        else
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScreenErrorPacket(result.error()));
    }
}
