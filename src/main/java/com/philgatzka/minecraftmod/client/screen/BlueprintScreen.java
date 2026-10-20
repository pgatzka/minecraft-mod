package com.philgatzka.minecraftmod.client.screen;

import com.philgatzka.minecraftmod.blueprint.Blueprints;
import com.philgatzka.minecraftmod.client.ui.UiButton;
import com.philgatzka.minecraftmod.client.ui.UiColumn;
import com.philgatzka.minecraftmod.client.ui.UiScreen;
import com.philgatzka.minecraftmod.client.ui.UiTheme;
import com.philgatzka.minecraftmod.menu.BlueprintMenu;
import com.philgatzka.minecraftmod.network.ModNetwork;
import com.philgatzka.minecraftmod.network.SaveBlueprintPacket;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

// "Save Blueprint": shows the checked area and its city block, and asks for a file name
public class BlueprintScreen extends UiScreen<BlueprintMenu>
{
    @Nullable
    private EditBox nameField;
    @Nullable
    private UiButton saveButton;

    public BlueprintScreen(BlueprintMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title, 260, 180);
    }

    @Override
    protected void init()
    {
        super.init();
        // init() runs again when the window is resized; keep what the player already typed
        String typedName = nameField == null ? "" : nameField.getValue();

        BlueprintMenu.Info info = menu.getInfo();
        Block cityBlock = ForgeRegistries.BLOCKS.getValue(info.cityBlock());
        Vec3i size = info.selection().size();
        BlockPos anchor = info.anchor();

        UiColumn column = column();
        column.label(Component.translatable("gui.minecraftmod.blueprint.city_block", cityBlock.getName(), anchor.getX(), anchor.getY(), anchor.getZ()),
                UiTheme.TEXT);
        column.label(Component.translatable("gui.minecraftmod.blueprint.size", size.getX(), size.getY(), size.getZ()), UiTheme.TEXT_MUTED);
        column.space(2);
        column.label(Component.translatable("gui.minecraftmod.blueprint.name"), UiTheme.TEXT_MUTED);
        nameField = column.textField(Component.translatable("gui.minecraftmod.blueprint.name"), Blueprints.MAX_NAME_LENGTH);
        nameField.setFilter(text -> text.matches("[a-z0-9_]*"));
        nameField.setValue(typedName);
        column.label(() -> nameField.getValue().isEmpty() ? Component.empty()
                : Component.translatable("gui.minecraftmod.blueprint.id", Blueprints.idFor(cityBlock, nameField.getValue()).toString()), UiTheme.TEXT_MUTED, 2);
        saveButton = column.button(Component.translatable("gui.minecraftmod.blueprint.save"), this::save);
        column.errorLabel(3);
        setInitialFocus(nameField);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (saveButton != null && nameField != null)
            saveButton.active = !nameField.getValue().isEmpty();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && nameField != null && nameField.isFocused())
        {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // The server checks the area again; on success it closes this screen and reports where the file went
    private void save()
    {
        if (nameField == null || nameField.getValue().isEmpty())
            return;
        clearError();
        ModNetwork.CHANNEL.sendToServer(new SaveBlueprintPacket(nameField.getValue()));
    }
}
