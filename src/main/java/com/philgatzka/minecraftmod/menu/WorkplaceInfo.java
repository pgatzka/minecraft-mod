package com.philgatzka.minecraftmod.menu;

import com.philgatzka.minecraftmod.block.entity.WorkplaceBlockEntity;
import com.philgatzka.minecraftmod.settlement.Settlement;
import com.philgatzka.minecraftmod.settlement.SettlerJob;
import com.philgatzka.minecraftmod.settlement.SettlerRecord;
import com.philgatzka.minecraftmod.settlement.Settlements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

// What the workplace screen shows, sent to the client when the menu opens
public record WorkplaceInfo(BlockPos pos, SettlerJob job, @Nullable String settlementName, @Nullable String workerName, boolean canManage,
                            List<SettlerEntry> settlers)
{
    // One selectable settler; only sent to the settlement's leader
    public record SettlerEntry(UUID id, String name, @Nullable SettlerJob job, boolean worksHere)
    {
        void write(FriendlyByteBuf buf)
        {
            buf.writeUUID(id);
            buf.writeUtf(name);
            buf.writeNullable(job, FriendlyByteBuf::writeEnum);
            buf.writeBoolean(worksHere);
        }

        static SettlerEntry read(FriendlyByteBuf buf)
        {
            return new SettlerEntry(buf.readUUID(), buf.readUtf(), buf.readNullable(b -> b.readEnum(SettlerJob.class)), buf.readBoolean());
        }
    }

    public static WorkplaceInfo of(ServerPlayer player, WorkplaceBlockEntity workplace)
    {
        BlockPos pos = workplace.getBlockPos();
        Settlement settlement = Settlements.getSettlement(player.serverLevel(), workplace.getSettlementId()).orElse(null);
        if (settlement == null)
            return new WorkplaceInfo(pos, workplace.getJob(), null, null, false, List.of());

        String workerName = settlement.workerAt(pos).map(SettlerRecord::fullName).orElse(null);
        boolean canManage = settlement.leaderId().equals(player.getUUID());
        List<SettlerEntry> settlers = !canManage ? List.of() : settlement.settlers().stream()
                .map(settler -> new SettlerEntry(settler.entityId(), settler.fullName(), settler.job(), settler.worksAt(pos)))
                .toList();
        return new WorkplaceInfo(pos, workplace.getJob(), settlement.name(), workerName, canManage, settlers);
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(pos);
        buf.writeEnum(job);
        buf.writeNullable(settlementName, FriendlyByteBuf::writeUtf);
        buf.writeNullable(workerName, FriendlyByteBuf::writeUtf);
        buf.writeBoolean(canManage);
        buf.writeCollection(settlers, (b, entry) -> entry.write(b));
    }

    public static WorkplaceInfo read(FriendlyByteBuf buf)
    {
        return new WorkplaceInfo(buf.readBlockPos(), buf.readEnum(SettlerJob.class), buf.readNullable(FriendlyByteBuf::readUtf),
                buf.readNullable(FriendlyByteBuf::readUtf), buf.readBoolean(), buf.readList(SettlerEntry::read));
    }
}
