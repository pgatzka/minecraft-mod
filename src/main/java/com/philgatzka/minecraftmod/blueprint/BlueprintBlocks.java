package com.philgatzka.minecraftmod.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The non-air blocks of a blueprint by position inside it (unrotated). Small enough to send to clients for the preview.
public record BlueprintBlocks(Vec3i size, BlockPos anchor, Map<BlockPos, BlockState> blocks)
{
    // Read from a vanilla structure file's "palette"/"palettes" and "blocks" lists
    public static BlueprintBlocks fromTemplateTag(CompoundTag tag, BlockPos anchor)
    {
        ListTag sizeTag = tag.getList("size", Tag.TAG_INT);
        Vec3i size = new Vec3i(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2));
        ListTag paletteTag = tag.contains("palettes", Tag.TAG_LIST) ? tag.getList("palettes", Tag.TAG_LIST).getList(0) : tag.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++)
            palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), paletteTag.getCompound(i)));

        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockList.size(); i++)
        {
            CompoundTag block = blockList.getCompound(i);
            ListTag pos = block.getList("pos", Tag.TAG_INT);
            BlockState state = palette.get(block.getInt("state"));
            if (!state.isAir() && !state.is(Blocks.STRUCTURE_VOID))
                blocks.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), state);
        }
        return new BlueprintBlocks(size, anchor, Collections.unmodifiableMap(blocks));
    }

    // Blocks with at least one side not covered by a full block of the blueprint; the rest can't be seen in a preview
    public Map<BlockPos, BlockState> visibleBlocks()
    {
        Map<BlockPos, BlockState> visible = new LinkedHashMap<>();
        blocks.forEach((pos, state) -> {
            for (Direction direction : Direction.values())
            {
                BlockState neighbour = blocks.get(pos.relative(direction));
                if (neighbour == null || !neighbour.canOcclude())
                {
                    visible.put(pos, state);
                    return;
                }
            }
        });
        return visible;
    }

    // Palette + one (x, y, z, palette index) entry per block; sizes are at most Blueprints.MAX_SIZE, so coordinates fit in a byte
    public void write(FriendlyByteBuf buf)
    {
        buf.writeVarInt(size.getX());
        buf.writeVarInt(size.getY());
        buf.writeVarInt(size.getZ());
        buf.writeBlockPos(anchor);
        Map<BlockState, Integer> palette = new HashMap<>();
        List<BlockState> paletteList = new ArrayList<>();
        blocks.values().forEach(state -> palette.computeIfAbsent(state, s -> {
            paletteList.add(s);
            return paletteList.size() - 1;
        }));
        buf.writeVarInt(paletteList.size());
        paletteList.forEach(state -> buf.writeNbt(NbtUtils.writeBlockState(state)));
        buf.writeVarInt(blocks.size());
        blocks.forEach((pos, state) -> {
            buf.writeByte(pos.getX());
            buf.writeByte(pos.getY());
            buf.writeByte(pos.getZ());
            buf.writeVarInt(palette.get(state));
        });
    }

    public static BlueprintBlocks read(FriendlyByteBuf buf)
    {
        Vec3i size = new Vec3i(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        BlockPos anchor = buf.readBlockPos();
        int paletteSize = buf.readVarInt();
        List<BlockState> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++)
            palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), buf.readNbt()));
        int count = buf.readVarInt();
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int i = 0; i < count; i++)
            blocks.put(new BlockPos(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()), palette.get(buf.readVarInt()));
        return new BlueprintBlocks(size, anchor, Collections.unmodifiableMap(blocks));
    }
}
