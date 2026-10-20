package com.philgatzka.minecraftmod.menu;

import com.philgatzka.minecraftmod.block.entity.TownHallBlockEntity;
import com.philgatzka.minecraftmod.settlement.Settlement;
import com.philgatzka.minecraftmod.settlement.Settlements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

// What the Town Hall screen shows, sent to the client when the menu opens
public record TownHallInfo(BlockPos pos, @Nullable String settlementName, @Nullable String leaderName, int settlerCount,
                           @Nullable String playerSettlementName)
{
    public static TownHallInfo of(ServerPlayer player, TownHallBlockEntity townHall)
    {
        Settlement settlement = Settlements.getSettlement(player.serverLevel(), townHall.getSettlementId()).orElse(null);
        Settlement led = Settlements.getLedBy(player.serverLevel(), player).orElse(null);
        return new TownHallInfo(townHall.getBlockPos(),
                settlement == null ? null : settlement.name(),
                settlement == null ? null : settlement.leaderName(),
                settlement == null ? 0 : settlement.settlers().size(),
                led == null ? null : led.name());
    }

    public boolean hasSettlement()
    {
        return settlementName != null;
    }

    // The viewing player can found a settlement here: the Town Hall is free and they don't lead one yet
    public boolean canCreate()
    {
        return !hasSettlement() && playerSettlementName == null;
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(pos);
        buf.writeNullable(settlementName, FriendlyByteBuf::writeUtf);
        buf.writeNullable(leaderName, FriendlyByteBuf::writeUtf);
        buf.writeVarInt(settlerCount);
        buf.writeNullable(playerSettlementName, FriendlyByteBuf::writeUtf);
    }

    public static TownHallInfo read(FriendlyByteBuf buf)
    {
        return new TownHallInfo(buf.readBlockPos(), buf.readNullable(FriendlyByteBuf::readUtf), buf.readNullable(FriendlyByteBuf::readUtf),
                buf.readVarInt(), buf.readNullable(FriendlyByteBuf::readUtf));
    }
}
