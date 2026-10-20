package com.philgatzka.minecraftmod.client.screen;

import com.philgatzka.minecraftmod.client.ui.UiButton;
import com.philgatzka.minecraftmod.client.ui.UiColumn;
import com.philgatzka.minecraftmod.client.ui.UiScreen;
import com.philgatzka.minecraftmod.client.ui.UiSelectionList;
import com.philgatzka.minecraftmod.client.ui.UiTheme;
import com.philgatzka.minecraftmod.menu.BlueprintPickerMenu;
import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.network.SelectBlueprintPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// The Builder's Wand's "Choose Blueprint" screen: pick which blueprint the wand places
public class BlueprintPickerScreen extends UiScreen<BlueprintPickerMenu>
{
    private static final int LIST_ROWS = 8;

    @Nullable
    private UiSelectionList<BlueprintPickerMenu.Entry> list;
    @Nullable
    private UiButton selectButton;

    public BlueprintPickerScreen(BlueprintPickerMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title, 240, 192);
    }

    @Override
    protected void init()
    {
        super.init();
        BlueprintPickerMenu.Entry previous = list == null ? null : list.getSelected();
        list = null;
        selectButton = null;

        List<BlueprintPickerMenu.Entry> blueprints = menu.getInfo().blueprints();
        UiColumn column = column();
        if (blueprints.isEmpty())
        {
            column.label(Component.translatable("gui.minecraftmod.picker.empty"), UiTheme.TEXT);
            return;
        }

        column.label(Component.translatable("gui.minecraftmod.picker.choose"), UiTheme.TEXT_MUTED);
        list = column.selectionList(LIST_ROWS, blueprints, BlueprintPickerScreen::rowText, entry -> clearError());
        list.select(previous != null ? previous : blueprints.stream().filter(entry -> entry.id().equals(menu.getInfo().selected())).findFirst().orElse(null));
        selectButton = column.button(Component.translatable("gui.minecraftmod.picker.select"), this::select);
        column.errorLabel();
    }

    private static Component rowText(BlueprintPickerMenu.Entry entry)
    {
        Block cityBlock = ForgeRegistries.BLOCKS.getValue(entry.cityBlock());
        return Component.translatable("gui.minecraftmod.picker.row", cityBlock.getName(), entry.name());
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (selectButton != null && list != null)
            selectButton.active = list.getSelected() != null;
    }

    // On success the server closes this screen and the wand starts previewing the blueprint
    private void select()
    {
        BlueprintPickerMenu.Entry selected = list == null ? null : list.getSelected();
        if (selected == null)
            return;
        clearError();
        ModNetwork.CHANNEL.sendToServer(new SelectBlueprintPacket(selected.id()));
    }
}
