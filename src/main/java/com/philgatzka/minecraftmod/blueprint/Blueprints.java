package com.philgatzka.minecraftmod.blueprint;

import com.mojang.logging.LogUtils;
import com.philgatzka.minecraftmod.MinecraftMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Saving structures around a city block as blueprint files, and loading/placing them again.
//
// A blueprint is a vanilla structure file (.nbt, so structure blocks can open it too) with extra data under METADATA_KEY.
// Saved blueprints go to <world>/generated/minecraftmod/structures/blueprints/<city block>/<name>.nbt; blueprints shipped
// with the mod live in data/<namespace>/structures/blueprints/... and are loaded the same way (world files win).
public final class Blueprints
{
    private static final Logger LOGGER = LogUtils.getLogger();

    // Same limit as vanilla structure blocks
    public static final int MAX_SIZE = 48;
    public static final int MAX_NAME_LENGTH = 32;
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9_]{1," + MAX_NAME_LENGTH + "}");
    private static final String METADATA_KEY = MinecraftMod.MODID + ":blueprint";
    private static final String FOLDER = "blueprints";

    // Blocks a blueprint can be built around (data/minecraftmod/tags/blocks/city_blocks.json)
    public static final TagKey<Block> CITY_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MinecraftMod.MODID, "city_blocks"));

    private Blueprints()
    {
    }

    // Saving writes files on the server, so it's limited to operators and the singleplayer world's owner
    public static boolean canUse(CommandSourceStack source)
    {
        return source.hasPermission(2) || source.getEntity() instanceof ServerPlayer player && canUse(player);
    }

    public static boolean canUse(ServerPlayer player)
    {
        return player.hasPermissions(2) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    // --- Checking an area ---

    public record Analysis(@Nullable Block cityBlock, @Nullable BlockPos anchor, @Nullable Component error)
    {
        public boolean isValid()
        {
            return error == null;
        }
    }

    // A blueprint area must fit MAX_SIZE and contain exactly one city block
    public static Analysis analyze(Level level, BlueprintSelection selection)
    {
        if (!selection.fitsMaxSize())
            return failure(Component.translatable("blueprint.minecraftmod.error.too_large", MAX_SIZE));

        Block cityBlock = null;
        BlockPos cityBlockPos = null;
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(selection.min(), selection.max()))
        {
            if (level.getBlockState(pos).is(CITY_BLOCKS))
            {
                count++;
                cityBlock = level.getBlockState(pos).getBlock();
                cityBlockPos = pos.immutable();
            }
        }
        if (count == 0)
            return failure(Component.translatable("blueprint.minecraftmod.error.no_city_block"));
        if (count > 1)
            return failure(Component.translatable("blueprint.minecraftmod.error.several_city_blocks", count));
        return new Analysis(cityBlock, cityBlockPos.subtract(selection.min()), null);
    }

    private static Analysis failure(Component error)
    {
        return new Analysis(null, null, error);
    }

    // --- Saving ---

    public record SaveResult(@Nullable ResourceLocation id, @Nullable Path file, @Nullable Component error)
    {
        static SaveResult failure(Component error)
        {
            return new SaveResult(null, null, error);
        }

        public boolean isSuccess()
        {
            return error == null;
        }
    }

    public static ResourceLocation idFor(Block cityBlock, String name)
    {
        return ResourceLocation.fromNamespaceAndPath(MinecraftMod.MODID, FOLDER + "/" + ForgeRegistries.BLOCKS.getKey(cityBlock).getPath() + "/" + name);
    }

    public static SaveResult save(ServerLevel level, BlueprintSelection selection, String name, String author)
    {
        if (!NAME_PATTERN.matcher(name).matches())
            return SaveResult.failure(Component.translatable("blueprint.minecraftmod.error.invalid_name", MAX_NAME_LENGTH));
        Analysis analysis = analyze(level, selection);
        if (!analysis.isValid())
            return SaveResult.failure(analysis.error());

        ResourceLocation id = idFor(analysis.cityBlock(), name);
        Path file = worldFile(level.getServer(), id);
        if (Files.exists(file))
            return SaveResult.failure(Component.translatable("blueprint.minecraftmod.error.exists", id.toString()));

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, selection.min(), selection.size(), false, Blocks.STRUCTURE_VOID);
        template.setAuthor(author);
        CompoundTag tag = template.save(new CompoundTag());
        stripSettlementData(tag);

        CompoundTag metadata = new CompoundTag();
        metadata.putString("CityBlock", ForgeRegistries.BLOCKS.getKey(analysis.cityBlock()).toString());
        metadata.put("Anchor", NbtUtils.writeBlockPos(analysis.anchor()));
        tag.put(METADATA_KEY, metadata);

        try
        {
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(tag, file.toFile());
        }
        catch (IOException e)
        {
            LOGGER.error("Could not save blueprint {} to {}", id, file, e);
            return SaveResult.failure(Component.translatable("blueprint.minecraftmod.error.io", e.getMessage()));
        }
        return new SaveResult(id, file, null);
    }

    // City blocks in a blueprint must not remember the settlement they were captured from
    private static void stripSettlementData(CompoundTag templateTag)
    {
        ListTag blocks = templateTag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++)
        {
            CompoundTag block = blocks.getCompound(i);
            if (block.contains("nbt", Tag.TAG_COMPOUND))
                block.getCompound("nbt").remove("Settlement");
        }
    }

    // --- Loading, listing and placing ---

    public static Path worldFile(MinecraftServer server, ResourceLocation id)
    {
        return server.getWorldPath(LevelResource.GENERATED_DIR).resolve(id.getNamespace()).resolve("structures").resolve(id.getPath() + ".nbt").normalize();
    }

    public static Optional<Blueprint> load(MinecraftServer server, ResourceLocation id)
    {
        try
        {
            CompoundTag tag = null;
            Path file = worldFile(server, id);
            if (Files.exists(file))
            {
                tag = NbtIo.readCompressed(file.toFile());
            }
            else
            {
                Optional<Resource> resource = server.getResourceManager().getResource(id.withPath(path -> "structures/" + path + ".nbt"));
                if (resource.isPresent())
                    try (InputStream stream = resource.get().open())
                    {
                        tag = NbtIo.readCompressed(stream);
                    }
            }
            if (tag == null || !tag.contains(METADATA_KEY, Tag.TAG_COMPOUND))
                return Optional.empty();

            CompoundTag metadata = tag.getCompound(METADATA_KEY);
            Block cityBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(metadata.getString("CityBlock")));
            BlockPos anchor = NbtUtils.readBlockPos(metadata.getCompound("Anchor"));
            StructureTemplate template = server.getStructureManager().readStructure(tag);
            return Optional.of(new Blueprint(id, template, cityBlock, anchor, BlueprintBlocks.fromTemplateTag(tag, anchor)));
        }
        catch (IOException | RuntimeException e)
        {
            LOGGER.error("Could not load blueprint {}", id, e);
            return Optional.empty();
        }
    }

    public record Listed(ResourceLocation id, boolean inWorld)
    {
    }

    // The city block a blueprint is for, from its id (blueprints/<city block>/<name>); null if that block doesn't exist
    @Nullable
    public static Block cityBlockOf(ResourceLocation id)
    {
        String[] parts = id.getPath().split("/");
        if (parts.length != 3 || !parts[0].equals(FOLDER))
            return null;
        ResourceLocation blockId = ResourceLocation.tryBuild(id.getNamespace(), parts[1]);
        return blockId != null && ForgeRegistries.BLOCKS.containsKey(blockId) ? ForgeRegistries.BLOCKS.getValue(blockId) : null;
    }

    // Blueprints saved in this world and shipped in mods/datapacks, sorted by id
    public static List<Listed> list(MinecraftServer server)
    {
        Map<ResourceLocation, Listed> found = new LinkedHashMap<>();
        server.getResourceManager().listResources("structures/" + FOLDER, location -> location.getPath().endsWith(".nbt")).keySet()
                .forEach(location -> {
                    ResourceLocation id = location.withPath(path -> path.substring("structures/".length(), path.length() - ".nbt".length()));
                    found.put(id, new Listed(id, false));
                });

        Path root = server.getWorldPath(LevelResource.GENERATED_DIR).resolve(MinecraftMod.MODID).resolve("structures");
        Path folder = root.resolve(FOLDER);
        if (Files.isDirectory(folder))
        {
            try (Stream<Path> files = Files.walk(folder))
            {
                files.filter(file -> file.toString().endsWith(".nbt")).forEach(file -> {
                    String path = root.relativize(file).toString().replace('\\', '/');
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MinecraftMod.MODID, path.substring(0, path.length() - ".nbt".length()));
                    found.put(id, new Listed(id, true));
                });
            }
            catch (IOException e)
            {
                LOGGER.error("Could not list blueprints in {}", folder, e);
            }
        }

        List<Listed> listed = new ArrayList<>(found.values());
        listed.sort(Comparator.comparing(entry -> entry.id().toString()));
        return listed;
    }

    // Places the blueprint turned by `rotation`, with the min corner of the rotated area at origin, and returns that area
    public static BlueprintSelection place(ServerLevel level, Blueprint blueprint, BlockPos origin, Rotation rotation)
    {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        // The template turns around its first block, so shift it until the rotated area starts at origin again
        BoundingBox box = blueprint.template().getBoundingBox(settings, BlockPos.ZERO);
        BlockPos start = origin.offset(-box.minX(), -box.minY(), -box.minZ());
        blueprint.template().placeInWorld(level, start, start, settings, level.random, Block.UPDATE_ALL);
        return new BlueprintSelection(origin, origin.offset(box.getXSpan() - 1, box.getYSpan() - 1, box.getZSpan() - 1));
    }
}
