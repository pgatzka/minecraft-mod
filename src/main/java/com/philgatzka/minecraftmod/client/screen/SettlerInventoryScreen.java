package com.philgatzka.minecraftmod.client.screen;

import com.philgatzka.minecraftmod.client.ui.UiDraw;
import com.philgatzka.minecraftmod.client.ui.UiScreen;
import com.philgatzka.minecraftmod.client.ui.UiTheme;
import com.philgatzka.minecraftmod.menu.SettlerInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

// A settler's inventory: armor column and storage above the player's inventory, all slots drawn in code.
// The menu decides where the slots are; vanilla's container screen draws the items, dragging and tooltips.
public class SettlerInventoryScreen extends UiScreen<SettlerInventoryMenu>
{
    public SettlerInventoryScreen(SettlerInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title, 200, 190);
        inventoryLabelX = SettlerInventoryMenu.STORAGE_X - 1;
        inventoryLabelY = SettlerInventoryMenu.PLAYER_INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        // Empty armor slots show vanilla's armor icons, which the container screen draws on top of these
        for (Slot slot : menu.slots)
            UiDraw.slot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1, UiTheme.SLOT_FILL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, UiTheme.TEXT, false);
    }
}
