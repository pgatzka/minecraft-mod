package com.philgatzka.minecraftmod.construction;

import com.philgatzka.minecraftmod.blueprint.Blueprint;
import com.philgatzka.minecraftmod.blueprint.BlueprintPlacement;
import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import com.philgatzka.minecraftmod.block.TownHallBlock;
import com.philgatzka.minecraftmod.block.entity.WorkplaceBlockEntity;
import com.philgatzka.minecraftmod.network.ConstructionSitesPacket;
import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.settlement.Settlement;
import com.philgatzka.minecraftmod.settlement.Settlements;
import com.philgatzka.minecraftmod.settlement.Territory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Placing a blueprint as a construction site: only its city block is placed, the rest of its area is marked to be built later
public final class ConstructionSites
{
    private ConstructionSites()
    {
    }

    public record Result(@Nullable ConstructionSite site, @Nullable Component error)
    {
        static Result failure(Component error)
        {
            return new Result(null, error);
        }

        public boolean isSuccess()
        {
            return site != null;
        }
    }

    public static BlueprintPlacement placementOf(Blueprint blueprint, BlockPos cityBlockPos, Rotation rotation)
    {
        return new BlueprintPlacement(cityBlockPos, blueprint.anchor(), blueprint.template().getSize(), rotation);
    }

    // Checks that need only the area, also used by the client preview (with the sites it knows about)
    @Nullable
    public static Component checkArea(Level level, BlueprintPlacement placement, List<BlueprintSelection> otherSites)
    {
        BlueprintSelection area = placement.area();
        if (level.isOutsideBuildHeight(area.min()) || level.isOutsideBuildHeight(area.max()))
            return Component.translatable("construction.minecraftmod.error.outside_world");
        if (!level.getBlockState(placement.cityBlockPos()).canBeReplaced())
            return Component.translatable("construction.minecraftmod.error.blocked");
        if (otherSites.stream().anyMatch(area::intersects))
            return Component.translatable("construction.minecraftmod.error.overlap");
        return null;
    }

    // Places the blueprint's city block at cityBlockPos and marks the rest of its area as a construction site.
    // Costs one of the city block's items unless the player is in creative mode.
    public static Result place(Player player, ServerLevel level, Blueprint blueprint, BlockPos cityBlockPos, Rotation rotation)
    {
        ConstructionSiteData data = ConstructionSiteData.get(level.getServer());
        BlueprintPlacement placement = placementOf(blueprint, cityBlockPos, rotation);
        Component areaError = checkArea(level, placement, data.in(level.dimension()).stream().map(ConstructionSite::area).toList());
        if (areaError != null)
            return Result.failure(areaError);

        if (needsTerritory(blueprint.cityBlock()))
        {
            Optional<Settlement> settlement = Settlements.getLedBy(level, player).filter(led -> led.dimension().equals(level.dimension()));
            if (settlement.isEmpty())
                return Result.failure(Component.translatable("construction.minecraftmod.error.no_settlement", blueprint.cityBlock().getName()));
            if (!Territory.covers(Territory.of(settlement.get()), placement.area()))
                return Result.failure(Component.translatable("construction.minecraftmod.error.outside_territory"));
        }
        else
        {
            UUID own = Settlements.getLedBy(level, player).map(Settlement::id).orElse(null);
            Optional<Settlement> other = Territory.claimedBy(level, placement.area(), own);
            if (other.isPresent())
                return Result.failure(Component.translatable("construction.minecraftmod.error.other_territory", other.get().name()));
        }

        Item cost = blueprint.cityBlock().asItem();
        if (!player.isCreative())
        {
            Inventory inventory = player.getInventory();
            int slot = findSlot(inventory, cost);
            if (slot < 0)
                return Result.failure(Component.translatable("construction.minecraftmod.error.missing_item", blueprint.cityBlock().getName()));
            inventory.removeItem(slot, 1);
        }

        BlockState state = blueprint.blocks().blocks().getOrDefault(blueprint.anchor(), blueprint.cityBlock().defaultBlockState()).rotate(rotation);
        level.setBlock(cityBlockPos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(cityBlockPos) instanceof WorkplaceBlockEntity workplace)
            Settlements.claimWorkplace(level, player, workplace);

        UUID settlementId = Settlements.getLedBy(level, player)
                .filter(settlement -> settlement.dimension().equals(level.dimension()))
                .map(Settlement::id)
                .orElse(null);
        ConstructionSite site = new ConstructionSite(UUID.randomUUID(), blueprint.id(), level.dimension(), cityBlockPos, rotation, placement.area(), settlementId);
        data.add(site);
        syncDimension(level);
        return new Result(site, null);
    }

    // Every building except the Town Hall (which founds the territory) must be placed inside its settlement's territory
    public static boolean needsTerritory(Block cityBlock)
    {
        return !(cityBlock instanceof TownHallBlock);
    }

    private static int findSlot(Inventory inventory, Item item)
    {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            if (inventory.getItem(slot).is(item))
                return slot;
        return -1;
    }

    public static Optional<ConstructionSite> atCityBlock(ServerLevel level, BlockPos pos)
    {
        return ConstructionSiteData.get(level.getServer()).atCityBlock(level.dimension(), pos);
    }

    // Removing a site's city block cancels the construction
    public static void onCityBlockRemoved(ServerLevel level, BlockPos pos)
    {
        atCityBlock(level, pos).ifPresent(site -> {
            ConstructionSiteData.get(level.getServer()).remove(site.id());
            syncDimension(level);
        });
    }

    // --- Telling clients about sites, so they can outline them ---

    public static void syncTo(ServerPlayer player)
    {
        // GameTest mock players have no connection to send to
        if (player.connection == null)
            return;
        List<ConstructionSite.Outline> outlines = ConstructionSiteData.get(player.server).in(player.level().dimension()).stream()
                .map(site -> new ConstructionSite.Outline(site.blueprint(), site.area()))
                .toList();
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ConstructionSitesPacket(outlines));
    }

    public static void syncDimension(ServerLevel level)
    {
        level.players().forEach(ConstructionSites::syncTo);
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
