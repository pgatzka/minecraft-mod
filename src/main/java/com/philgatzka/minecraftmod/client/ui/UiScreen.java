package com.philgatzka.minecraftmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;

// Base for slot-less menu screens: a code-drawn panel with a title, and widgets laid out with column()
public abstract class UiScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M>
{
    protected static final int PADDING = 8;

    private Component error = Component.empty();

    protected UiScreen(M menu, Inventory inventory, Component title, int width, int height)
    {
        super(menu, inventory, title);
        imageWidth = width;
        imageHeight = height;
        titleLabelX = PADDING;
        titleLabelY = PADDING;
    }

    // A column starting below the title, spanning the panel's inner width
    protected UiColumn column()
    {
        return new UiColumn(this, font, leftPos + PADDING, topPos + PADDING + font.lineHeight + 6, imageWidth - 2 * PADDING, 4);
    }

    // Shown by a column's errorLabel(), e.g. when the server rejects an action
    public void showError(Component message)
    {
        error = message;
    }

    public void clearError()
    {
        error = Component.empty();
    }

    Component getError()
    {
        return error;
    }

    <T extends GuiEventListener & Renderable & NarratableEntry> T addUiWidget(T widget)
    {
        return addRenderableWidget(widget);
    }

    <T extends Renderable> T addUiRenderable(T renderable)
    {
        return addRenderableOnly(renderable);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        UiDraw.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    // Title only: these screens have no player inventory, so skip its label
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, title, titleLabelX, titleLabelY, UiTheme.TEXT, false);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        for (GuiEventListener child : children())
            if (child instanceof EditBox field)
                field.tick();
    }

    // While typing, send keys to the text field so e.g. the inventory key doesn't close the screen; Escape and Tab still work
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_TAB && getFocused() instanceof EditBox field && field.canConsumeInput())
        {
            field.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
