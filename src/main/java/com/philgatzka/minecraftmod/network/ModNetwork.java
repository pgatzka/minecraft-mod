package com.philgatzka.minecraftmod.network;

import com.philgatzka.minecraftmod.MinecraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork
{
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(MinecraftMod.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private ModNetwork()
    {
    }

    public static void register()
    {
        int id = 0;
        CHANNEL.messageBuilder(CreateSettlementPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CreateSettlementPacket::encode)
                .decoder(CreateSettlementPacket::decode)
                .consumerMainThread(CreateSettlementPacket::handle)
                .add();
        CHANNEL.messageBuilder(ScreenErrorPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScreenErrorPacket::encode)
                .decoder(ScreenErrorPacket::decode)
                .consumerMainThread(ScreenErrorPacket::handle)
                .add();
        CHANNEL.messageBuilder(AssignWorkerPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AssignWorkerPacket::encode)
                .decoder(AssignWorkerPacket::decode)
                .consumerMainThread(AssignWorkerPacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveBlueprintPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveBlueprintPacket::encode)
                .decoder(SaveBlueprintPacket::decode)
                .consumerMainThread(SaveBlueprintPacket::handle)
                .add();
        CHANNEL.messageBuilder(SelectionSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SelectionSyncPacket::encode)
                .decoder(SelectionSyncPacket::decode)
                .consumerMainThread(SelectionSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(SelectBlueprintPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectBlueprintPacket::encode)
                .decoder(SelectBlueprintPacket::decode)
                .consumerMainThread(SelectBlueprintPacket::handle)
                .add();
        CHANNEL.messageBuilder(RotateBlueprintPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RotateBlueprintPacket::encode)
                .decoder(RotateBlueprintPacket::decode)
                .consumerMainThread(RotateBlueprintPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestBlueprintPreviewPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestBlueprintPreviewPacket::encode)
                .decoder(RequestBlueprintPreviewPacket::decode)
                .consumerMainThread(RequestBlueprintPreviewPacket::handle)
                .add();
        CHANNEL.messageBuilder(BlueprintPreviewPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BlueprintPreviewPacket::encode)
                .decoder(BlueprintPreviewPacket::decode)
                .consumerMainThread(BlueprintPreviewPacket::handle)
                .add();
        CHANNEL.messageBuilder(ConstructionSitesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConstructionSitesPacket::encode)
                .decoder(ConstructionSitesPacket::decode)
                .consumerMainThread(ConstructionSitesPacket::handle)
                .add();
        CHANNEL.messageBuilder(TerritoryPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TerritoryPacket::encode)
                .decoder(TerritoryPacket::decode)
                .consumerMainThread(TerritoryPacket::handle)
                .add();
        CHANNEL.messageBuilder(MoveSettlementPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MoveSettlementPacket::encode)
                .decoder(MoveSettlementPacket::decode)
                .consumerMainThread(MoveSettlementPacket::handle)
                .add();
    }
}
