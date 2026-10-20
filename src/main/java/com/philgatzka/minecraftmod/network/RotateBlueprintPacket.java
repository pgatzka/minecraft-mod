package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.item.BuildersWandItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Client -> server: the rotate key was pressed while holding a Builder's Wand; turn its blueprint 90° clockwise
public record RotateBlueprintPacket()
{
    public void encode(FriendlyByteBuf buf)
    {
    }

    public static RotateBlueprintPacket decode(FriendlyByteBuf buf)
    {
        return new RotateBlueprintPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context)
    {
        ServerPlayer player = context.get().getSender();
        if (player == null)
            return;
        ItemStack wand = BuildersWandItem.findHeld(player);
        if (wand.isEmpty() || BuildersWandItem.getBlueprint(wand) == null)
            return;
        Rotation rotation = BuildersWandItem.rotate(wand);
        player.displayClientMessage(Component.translatable("construction.minecraftmod.rotated", BuildersWandItem.rotationName(rotation)), true);
    }
}
