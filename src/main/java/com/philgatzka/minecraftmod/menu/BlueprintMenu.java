package com.philgatzka.minecraftmod.menu;

import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import com.philgatzka.minecraftmod.blueprint.Blueprints;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

// Slot-less menu behind the "Save Blueprint" screen; the server keeps the selection that was checked when it opened
public class BlueprintMenu extends AbstractContainerMenu
{
    public record Info(BlueprintSelection selection, ResourceLocation cityBlock, BlockPos anchor)
    {
        void write(FriendlyByteBuf buf)
        {
            selection.write(buf);
            buf.writeResourceLocation(cityBlock);
            buf.writeBlockPos(anchor);
        }

        static Info read(FriendlyByteBuf buf)
        {
            return new Info(BlueprintSelection.read(buf), buf.readResourceLocation(), buf.readBlockPos());
        }
    }

    private final Info info;

    // Client side, built from the data written in open()
    public BlueprintMenu(int containerId, Inventory inventory, FriendlyByteBuf buf)
    {
        this(containerId, Info.read(buf));
    }

    public BlueprintMenu(int containerId, Info info)
    {
        super(MinecraftMod.BLUEPRINT_MENU.get(), containerId);
        this.info = info;
    }

    public static void open(ServerPlayer player, BlueprintSelection selection, Blueprints.Analysis analysis)
    {
        Info info = new Info(selection, ForgeRegistries.BLOCKS.getKey(analysis.cityBlock()), analysis.anchor());
        NetworkHooks.openScreen(player, new SimpleMenuProvider((containerId, inventory, p) -> new BlueprintMenu(containerId, info),
                Component.translatable("container.minecraftmod.blueprint")), info::write);
    }

    public Info getInfo()
    {
        return info;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
