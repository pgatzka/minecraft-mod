package com.philgatzka.minecraftmod.menu;

import com.philgatzka.minecraftmod.MinecraftMod;
import com.philgatzka.minecraftmod.blueprint.Blueprints;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

// Slot-less menu behind the Builder's Wand's "Choose Blueprint" screen
public class BlueprintPickerMenu extends AbstractContainerMenu
{
    public record Entry(ResourceLocation id, ResourceLocation cityBlock)
    {
        // The last part of the id, e.g. "small_house" for minecraftmod:blueprints/town_hall/small_house
        public String name()
        {
            return id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        }
    }

    public record Info(List<Entry> blueprints, @Nullable ResourceLocation selected)
    {
        void write(FriendlyByteBuf buf)
        {
            buf.writeCollection(blueprints, (b, entry) -> {
                b.writeResourceLocation(entry.id());
                b.writeResourceLocation(entry.cityBlock());
            });
            buf.writeNullable(selected, FriendlyByteBuf::writeResourceLocation);
        }

        static Info read(FriendlyByteBuf buf)
        {
            return new Info(buf.readList(b -> new Entry(b.readResourceLocation(), b.readResourceLocation())),
                    buf.readNullable(FriendlyByteBuf::readResourceLocation));
        }
    }

    private final Info info;

    // Client side, built from the data written in open()
    public BlueprintPickerMenu(int containerId, Inventory inventory, FriendlyByteBuf buf)
    {
        this(containerId, Info.read(buf));
    }

    public BlueprintPickerMenu(int containerId, Info info)
    {
        super(MinecraftMod.BLUEPRINT_PICKER_MENU.get(), containerId);
        this.info = info;
    }

    // Lists every blueprint whose city block exists
    public static void open(ServerPlayer player, @Nullable ResourceLocation selected)
    {
        List<Entry> entries = Blueprints.list(player.server).stream()
                .map(listed -> {
                    var cityBlock = Blueprints.cityBlockOf(listed.id());
                    return cityBlock == null ? null : new Entry(listed.id(), ForgeRegistries.BLOCKS.getKey(cityBlock));
                })
                .filter(Objects::nonNull)
                .toList();
        Info info = new Info(entries, selected);
        NetworkHooks.openScreen(player, new SimpleMenuProvider((containerId, inventory, p) -> new BlueprintPickerMenu(containerId, info),
                Component.translatable("container.minecraftmod.blueprint_picker")), info::write);
    }

    public Info getInfo()
    {
        return info;
    }

    public boolean offers(ResourceLocation id)
    {
        return info.blueprints().stream().anyMatch(entry -> entry.id().equals(id));
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
