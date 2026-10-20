package com.philgatzka.minecraftmod.menu;

import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.block.entity.WorkplaceBlockEntity;
import com.philgatzka.minecraftmod.settlement.Settlements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

// Slot-less menu for any workplace (e.g. the Builder's Hut); lets the server check which workplace the player has open
public class WorkplaceMenu extends AbstractContainerMenu
{
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private final WorkplaceInfo info;
    private final ContainerLevelAccess access;

    // Client side, built from the data written in open()
    public WorkplaceMenu(int containerId, Inventory inventory, FriendlyByteBuf buf)
    {
        this(containerId, WorkplaceInfo.read(buf), ContainerLevelAccess.NULL);
    }

    public WorkplaceMenu(int containerId, WorkplaceInfo info, ContainerLevelAccess access)
    {
        super(MinecraftMod.WORKPLACE_MENU.get(), containerId);
        this.info = info;
        this.access = access;
    }

    // Opens (or reopens, to refresh) the workplace screen. An unclaimed workplace is claimed by its viewer's settlement first.
    public static void open(ServerPlayer player, BlockPos pos)
    {
        if (!(player.level().getBlockEntity(pos) instanceof WorkplaceBlockEntity workplace))
            return;
        Settlements.claimWorkplace(player.serverLevel(), player, workplace);
        WorkplaceInfo info = WorkplaceInfo.of(player, workplace);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, p) -> new WorkplaceMenu(containerId, info, ContainerLevelAccess.create(player.level(), pos)),
                workplace.getBlockState().getBlock().getName()), info::write);
    }

    public WorkplaceInfo getInfo()
    {
        return info;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof WorkplaceBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MAX_DISTANCE_SQR, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
