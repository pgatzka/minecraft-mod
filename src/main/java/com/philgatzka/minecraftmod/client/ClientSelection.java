package com.philgatzka.minecraftmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// The local player's blueprint selection as last sent by the server (see PlayerSelections), for drawing the outline
public final class ClientSelection
{
    @Nullable
    private static BlockPos first;
    @Nullable
    private static BlockPos second;
    @Nullable
    private static ResourceKey<Level> dimension;

    private ClientSelection()
    {
    }

    static void set(@Nullable BlockPos newFirst, @Nullable BlockPos newSecond)
    {
        first = newFirst;
        second = newSecond;
        dimension = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.dimension();
    }

    static void clear()
    {
        set(null, null);
    }

    // Corners are only shown in the dimension they were set in
    static boolean isIn(ResourceKey<Level> level)
    {
        return level.equals(dimension);
    }

    @Nullable
    static BlockPos first()
    {
        return first;
    }

    @Nullable
    static BlockPos second()
    {
        return second;
    }
}
