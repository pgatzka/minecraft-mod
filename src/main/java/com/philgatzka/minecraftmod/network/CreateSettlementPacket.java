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

// Client -> server: "found a settlement with this name at the Town Hall I have open"
public record CreateSettlementPacket(String name)
{
    // Generous upper bound so a modified client can't send huge strings; the real limit is checked in Settlements
    private static final int MAX_WIRE_LENGTH = 256;

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(name, MAX_WIRE_LENGTH);
    }

    public static CreateSettlementPacket decode(FriendlyByteBuf buf)
    {
        return new CreateSettlementPacket(buf.readUtf(MAX_WIRE_LENGTH));
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

        Settlements.CreateResult result = Settlements.tryCreate(player, townHall, name);
        if (result.isSuccess())
        {
            player.displayClientMessage(Component.translatable("settlement.minecraftmod.created", result.settlement().name()), false);
            TownHallMenu.open(player, pos); // reopen so the screen shows the new settlement
        }
        else
        {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScreenErrorPacket(result.error()));
        }
    }
}
