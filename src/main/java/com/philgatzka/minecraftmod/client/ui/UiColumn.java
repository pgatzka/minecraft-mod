package com.philgatzka.minecraftmod.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

// Stacks widgets top to bottom at full width, so screens don't have to hand-place coordinates
public class UiColumn
{
    public static final int TEXT_FIELD_HEIGHT = 18;
    public static final int BUTTON_HEIGHT = 20;

    private final UiScreen<?> screen;
    private final Font font;
    private final int x;
    private final int width;
    private final int gap;
    private int y;

    UiColumn(UiScreen<?> screen, Font font, int x, int y, int width, int gap)
    {
        this.screen = screen;
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.gap = gap;
    }

    public UiLabel label(Component text, int color)
    {
        return label(() -> text, color, font.split(text, width).size());
    }

    // For text that changes later: reserves space for the given number of lines, and never draws more than that
    public UiLabel label(Supplier<Component> text, int color, int lines)
    {
        UiLabel label = screen.addUiRenderable(new UiLabel(font, x, y, width, text, color, lines));
        y += lines * UiLabel.LINE_HEIGHT + gap;
        return label;
    }

    public EditBox textField(Component narration, int maxLength)
    {
        // EditBox draws its border with fills (no texture); inset by 1px so the border lines up with the column
        EditBox field = screen.addUiWidget(new EditBox(font, x + 1, y + 1, width - 2, TEXT_FIELD_HEIGHT - 2, narration));
        field.setMaxLength(maxLength);
        y += TEXT_FIELD_HEIGHT + gap;
        return field;
    }

    public UiButton button(Component text, Runnable action)
    {
        UiButton button = screen.addUiWidget(new UiButton(x, y, width, BUTTON_HEIGHT, text, action));
        y += BUTTON_HEIGHT + gap;
        return button;
    }

    // Several buttons side by side, sharing the column's width
    public List<UiButton> buttonRow(ButtonSpec... specs)
    {
        List<UiButton> buttons = new ArrayList<>();
        int buttonWidth = (width - gap * (specs.length - 1)) / specs.length;
        for (int i = 0; i < specs.length; i++)
            buttons.add(screen.addUiWidget(new UiButton(x + i * (buttonWidth + gap), y, buttonWidth, BUTTON_HEIGHT, specs[i].text(), specs[i].action())));
        y += BUTTON_HEIGHT + gap;
        return buttons;
    }

    public record ButtonSpec(Component text, Runnable action)
    {
    }

    public <T> UiSelectionList<T> selectionList(int rows, List<T> entries, Function<T, Component> rowText, Consumer<T> onSelect)
    {
        UiSelectionList<T> list = screen.addUiWidget(new UiSelectionList<>(x, y, width, rows, entries, rowText, onSelect));
        y += list.getHeight() + gap;
        return list;
    }

    // Reserves room for the screen's error message (see UiScreen.showError)
    public UiLabel errorLabel()
    {
        return errorLabel(2);
    }

    public UiLabel errorLabel(int lines)
    {
        return label(screen::getError, UiTheme.TEXT_ERROR, lines);
    }

    public UiColumn space(int pixels)
    {
        y += pixels;
        return this;
    }
}
