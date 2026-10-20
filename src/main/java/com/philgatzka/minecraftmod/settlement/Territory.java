package com.philgatzka.minecraftmod.settlement;

import com.philgatzka.minecraftmod.Config;
import com.philgatzka.minecraftmod.block.TownHallBlock;
import com.philgatzka.minecraftmod.block.WorkplaceBlock;
import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.network.TerritoryPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// A settlement's territory: the chunks it may build in. It is a square of chunks around the Town Hall's chunk
// (Config.townHallTerritoryRadius, 2 = 5x5) plus a square around each of its buildings' chunks (Config.buildingTerritoryRadius, 1 = 3x3).
public final class Territory
{
    private Territory()
    {
    }

    public static Set<ChunkPos> of(Settlement settlement)
    {
        Set<ChunkPos> chunks = new HashSet<>();
        if (settlement.townHallPos() != null)
            addSquare(chunks, new ChunkPos(settlement.townHallPos()), Config.townHallTerritoryRadius);
        for (BlockPos building : settlement.buildings())
            addSquare(chunks, new ChunkPos(building), Config.buildingTerritoryRadius);
        return chunks;
    }

    private static void addSquare(Set<ChunkPos> chunks, ChunkPos center, int radius)
    {
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
                chunks.add(new ChunkPos(center.x + x, center.z + z));
    }

    // The first settlement other than `except` (null = any) whose territory includes any chunk of the area
    public static Optional<Settlement> claimedBy(ServerLevel level, BlueprintSelection area, @Nullable UUID except)
    {
        return SettlementData.get(level.getServer()).all().stream()
                .filter(settlement -> settlement.dimension().equals(level.dimension()) && !settlement.id().equals(except))
                .filter(settlement -> touches(of(settlement), area))
                .findFirst();
    }

    // Any chunk of the area is in the territory
    public static boolean touches(Set<ChunkPos> territory, BlueprintSelection area)
    {
        for (int x = area.min().getX() >> 4; x <= area.max().getX() >> 4; x++)
            for (int z = area.min().getZ() >> 4; z <= area.max().getZ() >> 4; z++)
                if (territory.contains(new ChunkPos(x, z)))
                    return true;
        return false;
    }

    public static boolean contains(Settlement settlement, BlockPos pos)
    {
        return of(settlement).contains(new ChunkPos(pos));
    }

    // Every chunk the area touches is in the territory
    public static boolean covers(Set<ChunkPos> territory, BlueprintSelection area)
    {
        for (int x = area.min().getX() >> 4; x <= area.max().getX() >> 4; x++)
            for (int z = area.min().getZ() >> 4; z <= area.max().getZ() >> 4; z++)
                if (!territory.contains(new ChunkPos(x, z)))
                    return false;
        return true;
    }

    // Builder's Huts and other workplaces placed by hand must also be inside the placer's territory,
    // and a Town Hall must not be placed inside another settlement's territory
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level))
            return;
        if (event.getPlacedBlock().getBlock() instanceof TownHallBlock)
        {
            UUID own = Settlements.getLedBy(level, player).map(Settlement::id).orElse(null);
            Optional<Settlement> other = claimedBy(level, new BlueprintSelection(event.getPos(), event.getPos()), own);
            if (other.isPresent())
            {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("construction.minecraftmod.error.other_territory", other.get().name()), true);
            }
            return;
        }
        if (!(event.getPlacedBlock().getBlock() instanceof WorkplaceBlock))
            return;
        boolean inside = Settlements.getLedBy(level, player)
                .filter(settlement -> settlement.dimension().equals(level.dimension()))
                .map(settlement -> contains(settlement, event.getPos()))
                .orElse(false);
        if (!inside)
        {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("construction.minecraftmod.error.outside_territory"), true);
        }
    }

    // --- Telling a leader's client about their territory, so it can be shown ---

    public static void syncTo(ServerPlayer player)
    {
        // GameTest mock players have no connection to send to
        if (player.connection == null)
            return;
        Set<ChunkPos> chunks = Settlements.getLedBy(player.serverLevel(), player)
                .filter(settlement -> settlement.dimension().equals(player.level().dimension()))
                .map(Territory::of)
                .orElse(Set.of());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TerritoryPacket(chunks));
    }

    public static void syncLeader(MinecraftServer server, Settlement settlement)
    {
        ServerPlayer leader = server.getPlayerList().getPlayer(settlement.leaderId());
        if (leader != null)
            syncTo(leader);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            syncTo(player);
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            syncTo(player);
    }
}
