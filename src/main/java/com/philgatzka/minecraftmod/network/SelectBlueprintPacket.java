package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.blueprint.Blueprint;
import com.philgatzka.minecraftmod.blueprint.Blueprints;
import com.philgatzka.minecraftmod.item.BuildersWandItem;
import com.philgatzka.minecraftmod.menu.BlueprintPickerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;
import java.util.function.Supplier;

// Client -> server: "put this blueprint (from my open Choose Blueprint screen) on my Builder's Wand"
public record SelectBlueprintPacket(ResourceLocation id)
{
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(id);
    }

    public static SelectBlueprintPacket decode(FriendlyByteBuf buf)
    {
        return new SelectBlueprintPacket(buf.readResourceLocation());
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        ServerPlayer player = context.get().getSender();
        if (player == null || !(player.containerMenu instanceof BlueprintPickerMenu menu) || !menu.offers(id))
            return;
        ItemStack wand = BuildersWandItem.findHeld(player);
        if (wand.isEmpty())
            return;
        Optional<Blueprint> blueprint = Blueprints.load(player.server, id);
        if (blueprint.isEmpty())
        {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new ScreenErrorPacket(Component.translatable("blueprint.minecraftmod.error.not_found", id.toString())));
            return;
        }

        BuildersWandItem.setBlueprint(wand, id);
        player.closeContainer();
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlueprintPreviewPacket(id, blueprint.get().blocks()));
        player.displayClientMessage(Component.translatable("construction.minecraftmod.selected", menu.getInfo().blueprints().stream()
                .filter(entry -> entry.id().equals(id)).findFirst().map(BlueprintPickerMenu.Entry::name).orElse(id.toString())), true);
    }
}
