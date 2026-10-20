package com.philgatzka.minecraftmod.entity;

import net.minecraft.util.RandomSource;

import java.util.List;

public final class SettlerNames
{
    public static final List<String> FIRST_NAMES = List.of(
            "Ada", "Alaric", "Bram", "Cora", "Dorian", "Edda", "Elric", "Fiona",
            "Gareth", "Greta", "Hugo", "Ida", "Jasper", "Kira", "Leif", "Mara",
            "Nolan", "Odette", "Piers", "Quinn", "Rowan", "Signe", "Tobin", "Una",
            "Viktor", "Wren", "Yara", "Zeke");

    public static final List<String> LAST_NAMES = List.of(
            "Ashdown", "Barrow", "Blackwood", "Brightwater", "Carter", "Cooper", "Dunmore", "Fairweather",
            "Fletcher", "Hawthorne", "Hollis", "Ironside", "Marsh", "Miller", "Oakheart", "Pennywhistle",
            "Redfield", "Smith", "Stonebridge", "Thatcher", "Underhill", "Wainwright", "Weaver", "Whitlock");

    private SettlerNames()
    {
    }

    public static String randomFirstName(RandomSource random)
    {
        return FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
    }

    public static String randomLastName(RandomSource random)
    {
        return LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
    }
}
