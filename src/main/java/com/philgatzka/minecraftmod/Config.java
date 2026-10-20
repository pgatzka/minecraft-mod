package com.philgatzka.minecraftmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// The mod's common config (config/minecraftmod-common.toml)
@Mod.EventBusSubscriber(modid = MinecraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue SETTLERS_ON_FOUNDING = BUILDER
            .comment("How many settlers spawn at the Town Hall when a settlement is founded")
            .defineInRange("settlersOnFounding", 3, 0, 32);

    private static final ForgeConfigSpec.IntValue TOWN_HALL_TERRITORY_RADIUS = BUILDER
            .comment("Territory around a settlement's Town Hall, in chunks from the Town Hall's chunk (2 = a 5x5 square of chunks)")
            .defineInRange("townHallTerritoryRadius", 2, 0, 16);

    private static final ForgeConfigSpec.IntValue BUILDING_TERRITORY_RADIUS = BUILDER
            .comment("Territory each other building adds, in chunks from its chunk (1 = a 3x3 square of chunks)")
            .defineInRange("buildingTerritoryRadius", 1, 0, 16);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int settlersOnFounding;
    public static int townHallTerritoryRadius;
    public static int buildingTerritoryRadius;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        settlersOnFounding = SETTLERS_ON_FOUNDING.get();
        townHallTerritoryRadius = TOWN_HALL_TERRITORY_RADIUS.get();
        buildingTerritoryRadius = BUILDING_TERRITORY_RADIUS.get();
    }
}
