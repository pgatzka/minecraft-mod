package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.block.entity.TownHallBlockEntity;
import com.philgatzka.minecraftmod.menu.TownHallMenu;
import com.philgatzka.minecraftmod.settlement.Settlements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// Client -> server: "make the Town Hall I have open the new Town Hall of the settlement I lead"
public record MoveSettlementPacket()
{
    public void encode(FriendlyByteBuf buf)
    {
    }

    public static MoveSettlementPacket decode(FriendlyByteBuf buf)
    {
        return new MoveSettlementPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        ServerPlayer player = context.get().getSender();
        // Only accept requests for the Town Hall the player actually has open and is still near
        if (player == null || !(player.containerMenu instanceof TownHallMenu menu) || !menu.stillValid(player))
            return;
        BlockPos pos = menu.getInfo().pos();
        if (!(player.level().getBlockEntity(pos) instanceof TownHallBlockEntity townHall))
            return;

        Settlements.CreateResult result = Settlements.tryMoveTo(player, townHall);
        if (result.isSuccess())
        {
            player.displayClientMessage(Component.translatable("settlement.minecraftmod.moved", result.settlement().name()), false);
            TownHallMenu.open(player, pos); // reopen so the screen shows the settlement
        }
        else
        {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScreenErrorPacket(result.error()));
        }
    }
}
