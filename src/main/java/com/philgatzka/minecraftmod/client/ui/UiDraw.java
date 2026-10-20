package com.philgatzka.minecraftmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;

// Primitive shapes built from filled rectangles, so screens need no texture assets
public final class UiDraw
{
    private UiDraw()
    {
    }

    // A raised box: 1px outline, then a light top-left edge and a dark bottom-right edge of the given thickness
    public static void bevel(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int fill, int highlight, int shadow)
    {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, UiTheme.OUTLINE);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, shadow);
        graphics.fill(x + 1, y + 1, right - 1 - thickness, bottom - 1 - thickness, highlight);
        graphics.fill(x + 1 + thickness, y + 1 + thickness, right - 1 - thickness, bottom - 1 - thickness, fill);
    }

    // The background panel of a screen, like vanilla's container windows
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height)
    {
        bevel(graphics, x, y, width, height, 2, UiTheme.PANEL_FILL, UiTheme.PANEL_HIGHLIGHT, UiTheme.PANEL_SHADOW);
    }
}
