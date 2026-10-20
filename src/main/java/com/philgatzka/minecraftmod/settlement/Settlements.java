package com.philgatzka.minecraftmod.settlement;

import com.philgatzka.minecraftmod.Config;
import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.block.entity.TownHallBlockEntity;
import com.philgatzka.minecraftmod.block.entity.WorkplaceBlockEntity;
import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import com.philgatzka.minecraftmod.entity.SettlerEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

// Server-side rules for settlements, their settlers and their jobs
public final class Settlements
{
    public static final int MAX_NAME_LENGTH = 32;
    // How far from the Town Hall founding settlers may appear
    private static final int SPAWN_RADIUS = 3;

    private Settlements()
    {
    }

    public record CreateResult(@Nullable Settlement settlement, @Nullable Component error)
    {
        public static CreateResult success(Settlement settlement)
        {
            return new CreateResult(settlement, null);
        }

        public static CreateResult failure(Component error)
        {
            return new CreateResult(null, error);
        }

        public boolean isSuccess()
        {
            return settlement != null;
        }
    }

    public record Result(@Nullable Component error)
    {
        public static final Result SUCCESS = new Result(null);

        public static Result failure(Component error)
        {
            return new Result(error);
        }

        public boolean isSuccess()
        {
            return error == null;
        }
    }

    // A player can lead only one settlement, and a Town Hall can host only one settlement.
    // Founding spawns Config.settlersOnFounding settlers around the Town Hall, all members of the new settlement.
    public static CreateResult tryCreate(Player player, TownHallBlockEntity townHall, String requestedName)
    {
        ServerLevel level = serverLevel(townHall.getLevel());
        SettlementData data = SettlementData.get(level.getServer());

        Optional<Settlement> existing = getSettlement(level, townHall.getSettlementId());
        if (existing.isPresent())
            return CreateResult.failure(Component.translatable("settlement.minecraftmod.error.town_hall_taken", existing.get().name()));

        Optional<Settlement> led = data.getByLeader(player.getUUID());
        if (led.isPresent())
            return CreateResult.failure(Component.translatable("settlement.minecraftmod.error.already_leader", led.get().name()));

        BlockPos pos = townHall.getBlockPos();
        Optional<Settlement> owner = Territory.claimedBy(level, new BlueprintSelection(pos, pos), null);
        if (owner.isPresent())
            return CreateResult.failure(Component.translatable("construction.minecraftmod.error.other_territory", owner.get().name()));

        String name = SharedConstants.filterText(requestedName).trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH)
            return CreateResult.failure(Component.translatable("settlement.minecraftmod.error.invalid_name", MAX_NAME_LENGTH));

        Settlement settlement = data.create(name, player.getUUID(), player.getGameProfile().getName(), level.dimension(), townHall.getBlockPos());
        townHall.setSettlementId(settlement.id());
        spawnSettlers(level, data, settlement, townHall.getBlockPos(), Config.settlersOnFounding);
        Territory.syncChanged(level.getServer(), settlement);
        return CreateResult.success(settlement);
    }

    // A leader whose settlement lost its Town Hall can make a free Town Hall its new one. Like founding, the Town Hall
    // must not be in another settlement's territory; it must also be in the settlement's dimension.
    public static CreateResult tryMoveTo(Player player, TownHallBlockEntity townHall)
    {
        ServerLevel level = serverLevel(townHall.getLevel());
        SettlementData data = SettlementData.get(level.getServer());

        Optional<Settlement> existing = getSettlement(level, townHall.getSettlementId());
        if (existing.isPresent())
            return CreateResult.failure(Component.translatable("settlement.minecraftmod.error.town_hall_taken", existing.get().name()));

        Optional<Settlement> led = data.getByLeader(player.getUUID());
        if (led.isEmpty())
            return CreateResult.failure(Component.translatable("settlement.minecraftmod.error.not_a_leader"));
        Settlement settlement = led.get();
        if (settlement.townHallPos() != null)
            return CreateResult.failure(Component.translatable("settlement.minecraftmod.error.has_town_hall", settlement.name()));
        if (!settlement.dimension().equals(level.dimension()))
            return CreateResult.failure(Component.translatable("settlement.minecraftmod.error.other_dimension", settlement.name()));

        BlockPos pos = townHall.getBlockPos();
        Optional<Settlement> owner = Territory.claimedBy(level, new BlueprintSelection(pos, pos), settlement.id());
        if (owner.isPresent())
            return CreateResult.failure(Component.translatable("construction.minecraftmod.error.other_territory", owner.get().name()));

        data.setTownHall(settlement, pos);
        townHall.setSettlementId(settlement.id());
        Territory.syncChanged(level.getServer(), settlement);
        return CreateResult.success(settlement);
    }

    private static void spawnSettlers(ServerLevel level, SettlementData data, Settlement settlement, BlockPos center, int count)
    {
        List<BlockPos> spots = findSpawnSpots(level, center);
        for (int i = 0; i < count; i++)
        {
            // Spread settlers over the nearest free spots; if there are none, put them on top of the Town Hall
            BlockPos pos = spots.isEmpty() ? center.above() : spots.get(i % spots.size());
            SettlerEntity settler = MinecraftMod.SETTLER.get().create(level);
            if (settler == null)
                continue;
            settler.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
            settler.setSettlementId(settlement.id());
            level.addFreshEntity(settler);
            data.addSettler(settlement, new SettlerRecord(settler.getUUID(), settler.getFirstName(), settler.getLastName(), null, null));
        }
    }

    // Positions near the Town Hall with solid ground and two free blocks for a settler to stand in, nearest first
    private static List<BlockPos> findSpawnSpots(ServerLevel level, BlockPos center)
    {
        Iterable<BlockPos> area = BlockPos.betweenClosed(center.offset(-SPAWN_RADIUS, -1, -SPAWN_RADIUS), center.offset(SPAWN_RADIUS, 1, SPAWN_RADIUS));
        return StreamSupport.stream(area.spliterator(), false)
                .map(BlockPos::immutable)
                .filter(pos -> level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty())
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(center)))
                .toList();
    }

    public static Optional<Settlement> getSettlement(ServerLevel level, @Nullable UUID settlementId)
    {
        return settlementId == null ? Optional.empty() : SettlementData.get(level.getServer()).get(settlementId);
    }

    public static Optional<Settlement> getLedBy(ServerLevel level, Player player)
    {
        return SettlementData.get(level.getServer()).getByLeader(player.getUUID());
    }

    // The settlement outlives its Town Hall; it just no longer has one (and loses the Town Hall's part of its territory)
    public static void onTownHallRemoved(ServerLevel level, TownHallBlockEntity townHall)
    {
        getSettlement(level, townHall.getSettlementId())
                .filter(settlement -> townHall.getBlockPos().equals(settlement.townHallPos()))
                .ifPresent(settlement -> {
                    SettlementData.get(level.getServer()).setTownHall(settlement, null);
                    Territory.syncChanged(level.getServer(), settlement);
                });
    }

    public static void onSettlerDied(ServerLevel level, SettlerEntity settler)
    {
        getSettlement(level, settler.getSettlementId())
                .ifPresent(settlement -> SettlementData.get(level.getServer()).removeSettler(settlement, settler.getUUID()));
    }

    // An unclaimed workplace joins the settlement led by the player who places or opens it, if it's inside that settlement's
    // territory. It becomes one of the settlement's buildings, which extends the territory around it.
    public static void claimWorkplace(ServerLevel level, Player player, WorkplaceBlockEntity workplace)
    {
        if (workplace.getSettlementId() != null)
        {
            // Workplaces claimed before settlements tracked their buildings get registered the next time they're opened
            getSettlement(level, workplace.getSettlementId())
                    .filter(settlement -> !settlement.buildings().contains(workplace.getBlockPos()))
                    .ifPresent(settlement -> {
                        SettlementData.get(level.getServer()).addBuilding(settlement, workplace.getBlockPos());
                        Territory.syncChanged(level.getServer(), settlement);
                    });
            return;
        }
        getLedBy(level, player)
                .filter(settlement -> settlement.dimension().equals(level.dimension()))
                .filter(settlement -> Territory.contains(settlement, workplace.getBlockPos()))
                .ifPresent(settlement -> {
                    workplace.setSettlementId(settlement.id());
                    SettlementData.get(level.getServer()).addBuilding(settlement, workplace.getBlockPos());
                    Territory.syncChanged(level.getServer(), settlement);
                });
    }

    // Gives the workplace's job to a settler (null = nobody). A workplace has one worker and a settler has one job,
    // so the workplace's previous worker becomes unemployed and the settler leaves any previous job.
    public static Result assignWorker(Player player, WorkplaceBlockEntity workplace, @Nullable UUID settlerId)
    {
        ServerLevel level = serverLevel(workplace.getLevel());
        SettlementData data = SettlementData.get(level.getServer());

        Optional<Settlement> found = getSettlement(level, workplace.getSettlementId());
        if (found.isEmpty())
            return Result.failure(Component.translatable("settlement.minecraftmod.error.workplace_unclaimed"));
        Settlement settlement = found.get();
        if (!settlement.leaderId().equals(player.getUUID()))
            return Result.failure(Component.translatable("settlement.minecraftmod.error.not_leader", settlement.name()));
        if (settlerId != null && settlement.settler(settlerId).isEmpty())
            return Result.failure(Component.translatable("settlement.minecraftmod.error.unknown_settler"));

        BlockPos pos = workplace.getBlockPos();
        settlement.workerAt(pos).ifPresent(current -> data.setJob(settlement, current.entityId(), null, null));
        if (settlerId != null)
            data.setJob(settlement, settlerId, workplace.getJob(), pos);
        return Result.SUCCESS;
    }

    // Removing a workplace leaves its worker unemployed, and the territory it added goes away
    public static void onWorkplaceRemoved(ServerLevel level, WorkplaceBlockEntity workplace)
    {
        getSettlement(level, workplace.getSettlementId()).ifPresent(settlement -> {
            SettlementData data = SettlementData.get(level.getServer());
            settlement.workerAt(workplace.getBlockPos()).ifPresent(worker -> data.setJob(settlement, worker.entityId(), null, null));
            data.removeBuilding(settlement, workplace.getBlockPos());
            Territory.syncChanged(level.getServer(), settlement);
        });
    }

    private static ServerLevel serverLevel(@Nullable net.minecraft.world.level.Level level)
    {
        if (!(level instanceof ServerLevel serverLevel))
            throw new IllegalStateException("Settlements can only be changed on the server");
        return serverLevel;
    }
}
