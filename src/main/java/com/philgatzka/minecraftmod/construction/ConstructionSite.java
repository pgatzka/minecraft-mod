package com.philgatzka.minecraftmod.construction;

import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// An area marked for a blueprint to be built in: its city block is already placed, the rest is still to be built
public record ConstructionSite(UUID id, ResourceLocation blueprint, ResourceKey<Level> dimension, BlockPos cityBlockPos, Rotation rotation,
                               BlueprintSelection area, @Nullable UUID settlementId)
{
    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Blueprint", blueprint.toString());
        tag.putString("Dimension", dimension.location().toString());
        tag.put("CityBlock", NbtUtils.writeBlockPos(cityBlockPos));
        tag.putString("Rotation", rotation.name());
        tag.put("Min", NbtUtils.writeBlockPos(area.min()));
        tag.put("Max", NbtUtils.writeBlockPos(area.max()));
        if (settlementId != null)
            tag.putUUID("Settlement", settlementId);
        return tag;
    }

    public static ConstructionSite load(CompoundTag tag)
    {
        return new ConstructionSite(
                tag.getUUID("Id"),
                ResourceLocation.parse(tag.getString("Blueprint")),
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension"))),
                NbtUtils.readBlockPos(tag.getCompound("CityBlock")),
                Rotation.valueOf(tag.getString("Rotation")),
                new BlueprintSelection(NbtUtils.readBlockPos(tag.getCompound("Min")), NbtUtils.readBlockPos(tag.getCompound("Max"))),
                tag.hasUUID("Settlement") ? tag.getUUID("Settlement") : null);
    }

    // What clients need to outline a site
    public record Outline(ResourceLocation blueprint, BlueprintSelection area)
    {
        public void write(FriendlyByteBuf buf)
        {
            buf.writeResourceLocation(blueprint);
            area.write(buf);
        }

        public static Outline read(FriendlyByteBuf buf)
        {
            return new Outline(buf.readResourceLocation(), BlueprintSelection.read(buf));
        }
    }
}
