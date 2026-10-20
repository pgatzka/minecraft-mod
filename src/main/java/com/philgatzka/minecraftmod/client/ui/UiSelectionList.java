package com.philgatzka.minecraftmod.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

// A code-drawn, scrollable list where one entry can be selected by clicking; scroll with the mouse wheel
public class UiSelectionList<T> extends AbstractWidget
{
    public static final int ROW_HEIGHT = 12;
    private static final int INSET = 2;
    private static final int SCROLLBAR_WIDTH = 4;

    private final List<T> entries;
    private final Function<T, Component> rowText;
    private final Consumer<T> onSelect;
    private int selected = -1;
    private int scroll;

    public UiSelectionList(int x, int y, int width, int rows, List<T> entries, Function<T, Component> rowText, Consumer<T> onSelect)
    {
        super(x, y, width, rows * ROW_HEIGHT + 2 * INSET, Component.empty());
        this.entries = List.copyOf(entries);
        this.rowText = rowText;
        this.onSelect = onSelect;
    }

    @Nullable
    public T getSelected()
    {
        return selected < 0 ? null : entries.get(selected);
    }

    public void select(@Nullable T entry)
    {
        selected = entry == null ? -1 : entries.indexOf(entry);
        if (selected >= 0)
            scroll = Mth.clamp(selected - visibleRows() / 2, 0, maxScroll());
    }

    private int visibleRows()
    {
        return (height - 2 * INSET) / ROW_HEIGHT;
    }

    private int maxScroll()
    {
        return Math.max(0, entries.size() - visibleRows());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        Font font = Minecraft.getInstance().font;
        // Sunken well: dark top-left edge, light bottom-right edge (the reverse of a raised panel)
        UiDraw.bevel(graphics, getX(), getY(), width, height, 1, UiTheme.LIST_FILL, UiTheme.PANEL_SHADOW, UiTheme.PANEL_HIGHLIGHT);

        boolean scrollable = maxScroll() > 0;
        int rowWidth = width - 2 * INSET - (scrollable ? SCROLLBAR_WIDTH + 1 : 0);
        int hovered = rowAt(mouseX, mouseY);
        for (int row = 0; row < visibleRows() && scroll + row < entries.size(); row++)
        {
            int index = scroll + row;
            int rowX = getX() + INSET;
            int rowY = getY() + INSET + row * ROW_HEIGHT;
            if (index == selected)
                graphics.fill(rowX, rowY, rowX + rowWidth, rowY + ROW_HEIGHT, UiTheme.LIST_SELECTED);
            else if (index == hovered)
                graphics.fill(rowX, rowY, rowX + rowWidth, rowY + ROW_HEIGHT, UiTheme.LIST_HOVER);
            Component text = rowText.apply(entries.get(index));
            graphics.drawString(font, font.plainSubstrByWidth(text.getString(), rowWidth - 4), rowX + 2, rowY + 2, UiTheme.LIST_TEXT);
        }

        if (scrollable)
        {
            int trackX = getX() + width - INSET - SCROLLBAR_WIDTH;
            int trackTop = getY() + INSET;
            int trackHeight = height - 2 * INSET;
            int thumbHeight = Math.max(8, trackHeight * visibleRows() / entries.size());
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll();
            graphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackTop + trackHeight, UiTheme.LIST_SCROLL_TRACK);
            graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, UiTheme.LIST_SCROLL_THUMB);
        }
    }

    // Index of the entry under the mouse, or -1
    private int rowAt(double mouseX, double mouseY)
    {
        if (mouseX < getX() + INSET || mouseX >= getX() + width - INSET || mouseY < getY() + INSET || mouseY >= getY() + height - INSET)
            return -1;
        int index = scroll + (int) ((mouseY - getY() - INSET) / ROW_HEIGHT);
        return index < entries.size() ? index : -1;
    }

    @Override
    public void onClick(double mouseX, double mouseY)
    {
        int index = rowAt(mouseX, mouseY);
        if (index >= 0)
        {
            selected = index;
            onSelect.accept(entries.get(index));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (!isMouseOver(mouseX, mouseY) || maxScroll() == 0)
            return false;
        scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        T entry = getSelected();
        if (entry != null)
            output.add(NarratedElementType.TITLE, rowText.apply(entry));
    }
}
