package com.philgatzka.minecraftmod.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

// A button drawn with UiDraw instead of vanilla's widget texture
public class UiButton extends AbstractButton
{
    private final Runnable action;

    public UiButton(int x, int y, int width, int height, Component message, Runnable action)
    {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override
    public void onPress()
    {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        if (!active)
            UiDraw.bevel(graphics, getX(), getY(), width, height, 1, UiTheme.BUTTON_DISABLED_FILL, UiTheme.BUTTON_DISABLED_FILL, UiTheme.BUTTON_DISABLED_FILL);
        else if (isHoveredOrFocused())
            UiDraw.bevel(graphics, getX(), getY(), width, height, 1, UiTheme.BUTTON_HOVER_FILL, UiTheme.BUTTON_HOVER_HIGHLIGHT, UiTheme.BUTTON_HOVER_SHADOW);
        else
            UiDraw.bevel(graphics, getX(), getY(), width, height, 1, UiTheme.BUTTON_FILL, UiTheme.BUTTON_HIGHLIGHT, UiTheme.BUTTON_SHADOW);

        int textColor = active ? UiTheme.BUTTON_TEXT : UiTheme.BUTTON_TEXT_DISABLED;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        defaultButtonNarrationText(output);
    }
}
