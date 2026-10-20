package com.philgatzka.minecraftmod.client.screen;

import com.philgatzka.minecraftmod.client.ui.UiButton;
import com.philgatzka.minecraftmod.client.ui.UiColumn;
import com.philgatzka.minecraftmod.client.ui.UiScreen;
import com.philgatzka.minecraftmod.client.ui.UiTheme;
import com.philgatzka.minecraftmod.menu.TownHallInfo;
import com.philgatzka.minecraftmod.menu.TownHallMenu;
import com.philgatzka.minecraftmod.network.CreateSettlementPacket;
import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.network.MoveSettlementPacket;
import com.philgatzka.minecraftmod.settlement.Settlements;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

// Shows the Town Hall's settlement, or lets the player found one if the Town Hall is free and they don't lead one yet,
// or move their settlement here if it has lost its Town Hall
public class TownHallScreen extends UiScreen<TownHallMenu>
{
    @Nullable
    private EditBox nameField;
    @Nullable
    private UiButton createButton;

    public TownHallScreen(TownHallMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title, 220, 130);
    }

    @Override
    protected void init()
    {
        super.init();
        // init() runs again when the window is resized; keep what the player already typed
        String typedName = nameField == null ? "" : nameField.getValue();
        nameField = null;
        createButton = null;

        TownHallInfo info = menu.getInfo();
        UiColumn column = column();
        if (info.hasSettlement())
        {
            column.label(Component.translatable("gui.minecraftmod.town_hall.settlement", info.settlementName()), UiTheme.TEXT);
            column.label(Component.translatable("gui.minecraftmod.town_hall.leader", info.leaderName()), UiTheme.TEXT_MUTED);
            column.label(Component.translatable("gui.minecraftmod.town_hall.settlers", info.settlerCount()), UiTheme.TEXT_MUTED);
        }
        else if (info.canMoveHere())
        {
            column.label(Component.translatable("gui.minecraftmod.town_hall.no_settlement"), UiTheme.TEXT);
            column.label(Component.translatable("gui.minecraftmod.town_hall.homeless", info.playerSettlementName()), UiTheme.TEXT_MUTED);
            column.space(4);
            column.button(Component.translatable("gui.minecraftmod.town_hall.move", info.playerSettlementName()), this::moveSettlement);
            column.errorLabel();
        }
        else if (!info.canCreate())
        {
            column.label(Component.translatable("gui.minecraftmod.town_hall.no_settlement"), UiTheme.TEXT);
            column.label(Component.translatable("gui.minecraftmod.town_hall.already_leader", info.playerSettlementName()), UiTheme.TEXT_MUTED);
        }
        else
        {
            column.label(Component.translatable("gui.minecraftmod.town_hall.no_settlement"), UiTheme.TEXT);
            column.space(4);
            column.label(Component.translatable("gui.minecraftmod.town_hall.name"), UiTheme.TEXT_MUTED);
            nameField = column.textField(Component.translatable("gui.minecraftmod.town_hall.name"), Settlements.MAX_NAME_LENGTH);
            nameField.setValue(typedName);
            createButton = column.button(Component.translatable("gui.minecraftmod.town_hall.create"), this::createSettlement);
            column.errorLabel();
            setInitialFocus(nameField);
        }
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (createButton != null && nameField != null)
            createButton.active = !nameField.getValue().isBlank();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && nameField != null && nameField.isFocused())
        {
            createSettlement();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // The server checks everything again; on success it reopens this screen showing the new settlement
    private void createSettlement()
    {
        if (nameField == null || nameField.getValue().isBlank())
            return;
        clearError();
        ModNetwork.CHANNEL.sendToServer(new CreateSettlementPacket(nameField.getValue()));
    }

    // The server checks everything again; on success it reopens this screen showing the settlement
    private void moveSettlement()
    {
        clearError();
        ModNetwork.CHANNEL.sendToServer(new MoveSettlementPacket());
    }
}
