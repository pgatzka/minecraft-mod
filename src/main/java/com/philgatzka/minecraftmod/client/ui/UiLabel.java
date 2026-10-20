package com.philgatzka.minecraftmod.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.List;
import java.util.function.Supplier;

// Text that wraps to a fixed width; the supplier is read every frame, so the text can change (e.g. error messages).
// Never draws more than maxLines lines, so changing text can't spill over the widgets below it; cut-off text ends in "...".
public class UiLabel implements Renderable
{
    public static final int LINE_HEIGHT = 10;
    private static final FormattedText ELLIPSIS = FormattedText.of("...");

    private final Font font;
    private final int x;
    private final int y;
    private final int width;
    private final Supplier<Component> text;
    private final int color;
    private final int maxLines;

    public UiLabel(Font font, int x, int y, int width, Supplier<Component> text, int color, int maxLines)
    {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.text = text;
        this.color = color;
        this.maxLines = maxLines;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        List<FormattedText> lines = font.getSplitter().splitLines(text.get(), width, text.get().getStyle());
        int shown = Math.min(lines.size(), maxLines);
        for (int i = 0; i < shown; i++)
        {
            FormattedText line = lines.get(i);
            if (i == shown - 1 && lines.size() > shown)
                line = FormattedText.composite(font.substrByWidth(line, width - font.width(ELLIPSIS)), ELLIPSIS);
            graphics.drawString(font, Language.getInstance().getVisualOrder(line), x, y + i * LINE_HEIGHT, color, false);
        }
    }
}
