package com.philgatzka.minecraftmod.menu;

import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.block.entity.TownHallBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

// Slot-less menu: it exists so the server knows which Town Hall the player has open and that they are still close to it
public class TownHallMenu extends AbstractContainerMenu
{
    private final TownHallInfo info;
    private final ContainerLevelAccess access;

    // Client side, built from the data written in open()
    public TownHallMenu(int containerId, Inventory inventory, FriendlyByteBuf buf)
    {
        this(containerId, TownHallInfo.read(buf), ContainerLevelAccess.NULL);
    }

    public TownHallMenu(int containerId, TownHallInfo info, ContainerLevelAccess access)
    {
        super(MinecraftMod.TOWN_HALL_MENU.get(), containerId);
        this.info = info;
        this.access = access;
    }

    // Opens (or reopens, to refresh) the Town Hall screen for the player
    public static void open(ServerPlayer player, BlockPos pos)
    {
        if (!(player.level().getBlockEntity(pos) instanceof TownHallBlockEntity townHall))
            return;
        TownHallInfo info = TownHallInfo.of(player, townHall);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, p) -> new TownHallMenu(containerId, info, ContainerLevelAccess.create(player.level(), pos)),
                Component.translatable("container.minecraftmod.town_hall")), info::write);
    }

    public TownHallInfo getInfo()
    {
        return info;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(access, player, MinecraftMod.TOWN_HALL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
