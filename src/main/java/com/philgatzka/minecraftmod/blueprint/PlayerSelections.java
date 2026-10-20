package com.philgatzka.minecraftmod.blueprint;

import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.network.SelectionSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Each player's blueprint selection (two corners), shared by the Blueprint Wand and the /blueprint commands.
// Kept in memory only, like WorldEdit's; every change is sent to the player's client so it can draw the box.
public final class PlayerSelections
{
    public record Corners(ResourceKey<Level> dimension, @Nullable BlockPos first, @Nullable BlockPos second)
    {
        public Optional<BlueprintSelection> complete()
        {
            return first == null || second == null ? Optional.empty() : Optional.of(BlueprintSelection.of(first, second));
        }
    }

    private static final Map<UUID, Corners> SELECTIONS = new HashMap<>();

    private PlayerSelections()
    {
    }

    // Corners from another dimension than the player's current one are ignored
    public static Corners get(ServerPlayer player)
    {
        Corners corners = SELECTIONS.get(player.getUUID());
        return corners != null && corners.dimension().equals(player.level().dimension()) ? corners : new Corners(player.level().dimension(), null, null);
    }

    public static Optional<BlueprintSelection> current(ServerPlayer player)
    {
        return get(player).complete();
    }

    public static Corners setFirst(ServerPlayer player, BlockPos pos)
    {
        return update(player, new Corners(player.level().dimension(), pos.immutable(), get(player).second()));
    }

    public static Corners setSecond(ServerPlayer player, BlockPos pos)
    {
        return update(player, new Corners(player.level().dimension(), get(player).first(), pos.immutable()));
    }

    public static Corners set(ServerPlayer player, BlueprintSelection selection)
    {
        return update(player, new Corners(player.level().dimension(), selection.min(), selection.max()));
    }

    public static void clear(ServerPlayer player)
    {
        SELECTIONS.remove(player.getUUID());
        sync(player, null, null);
    }

    private static Corners update(ServerPlayer player, Corners corners)
    {
        SELECTIONS.put(player.getUUID(), corners);
        sync(player, corners.first(), corners.second());
        return corners;
    }

    private static void sync(ServerPlayer player, @Nullable BlockPos first, @Nullable BlockPos second)
    {
        // GameTest mock players have no connection to send to
        if (player.connection != null)
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SelectionSyncPacket(first, second));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        SELECTIONS.remove(event.getEntity().getUUID());
    }

    // The integrated server can be restarted in the same game session; don't carry selections over
    public static void onServerStopped(ServerStoppedEvent event)
    {
        SELECTIONS.clear();
    }
}
