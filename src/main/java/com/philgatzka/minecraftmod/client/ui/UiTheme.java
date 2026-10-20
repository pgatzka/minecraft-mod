package com.philgatzka.minecraftmod.client.ui;

// Colors (ARGB) for all code-drawn UI, styled after vanilla's gray container panels
public final class UiTheme
{
    public static final int OUTLINE = 0xFF000000;

    public static final int PANEL_FILL = 0xFFC6C6C6;
    public static final int PANEL_HIGHLIGHT = 0xFFFFFFFF;
    public static final int PANEL_SHADOW = 0xFF555555;

    public static final int BUTTON_FILL = 0xFF6F6F6F;
    public static final int BUTTON_HIGHLIGHT = 0xFFA8A8A8;
    public static final int BUTTON_SHADOW = 0xFF3F3F3F;
    public static final int BUTTON_HOVER_FILL = 0xFF7B8CC2;
    public static final int BUTTON_HOVER_HIGHLIGHT = 0xFFB4C2EC;
    public static final int BUTTON_HOVER_SHADOW = 0xFF4A5A8C;
    public static final int BUTTON_DISABLED_FILL = 0xFF4A4A4A;

    public static final int LIST_FILL = 0xFF8B8B8B;
    public static final int LIST_HOVER = 0xFF9E9E9E;
    public static final int LIST_SELECTED = 0xFF5D6E9E;
    public static final int LIST_TEXT = 0xFFFFFFFF;
    public static final int LIST_SCROLL_TRACK = 0xFF6B6B6B;
    public static final int LIST_SCROLL_THUMB = 0xFFD0D0D0;

    // Inventory slots, sunken like vanilla's
    public static final int SLOT_FILL = 0xFF8B8B8B;
    public static final int SLOT_SHADOW = 0xFF373737;
    public static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;

    public static final int TEXT = 0xFF404040;
    public static final int TEXT_MUTED = 0xFF6B6B6B;
    public static final int TEXT_ERROR = 0xFFAA0000;
    public static final int BUTTON_TEXT = 0xFFFFFFFF;
    public static final int BUTTON_TEXT_DISABLED = 0xFFA0A0A0;

    private UiTheme()
    {
    }
}
