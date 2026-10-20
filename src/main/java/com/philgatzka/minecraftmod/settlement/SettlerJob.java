package com.philgatzka.minecraftmod.settlement;

import net.minecraft.network.chat.Component;

// Jobs a settler can be assigned to; each job is tied to a workplace block (e.g. the Builder's Hut)
public enum SettlerJob
{
    BUILDER;

    public Component displayName()
    {
        return Component.translatable("job.minecraftmod." + name().toLowerCase());
    }
}
