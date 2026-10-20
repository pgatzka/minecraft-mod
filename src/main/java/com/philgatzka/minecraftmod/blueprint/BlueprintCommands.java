package com.philgatzka.minecraftmod.blueprint;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Optional;

// /blueprint pos1|pos2 [pos], clear, save <name>, load <id> [pos], list
public final class BlueprintCommands
{
    private static final SuggestionProvider<CommandSourceStack> BLUEPRINT_IDS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(Blueprints.list(context.getSource().getServer()).stream().map(Blueprints.Listed::id), builder);

    private BlueprintCommands()
    {
    }

    public static void register(RegisterCommandsEvent event)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("blueprint")
                .requires(Blueprints::canUse)
                .then(Commands.literal("pos1")
                        .executes(context -> setCorner(context, true, BlockPos.containing(context.getSource().getPosition())))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> setCorner(context, true, BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("pos2")
                        .executes(context -> setCorner(context, false, BlockPos.containing(context.getSource().getPosition())))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> setCorner(context, false, BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("clear")
                        .executes(BlueprintCommands::clear))
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> save(context, StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("load")
                        .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(BLUEPRINT_IDS)
                                .executes(context -> load(context, ResourceLocationArgument.getId(context, "id"),
                                        BlockPos.containing(context.getSource().getPosition())))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> load(context, ResourceLocationArgument.getId(context, "id"),
                                                BlockPosArgument.getLoadedBlockPos(context, "pos"))))))
                .then(Commands.literal("list")
                        .executes(BlueprintCommands::list)));
    }

    private static int setCorner(CommandContext<CommandSourceStack> context, boolean first, BlockPos pos) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerSelections.Corners corners = first ? PlayerSelections.setFirst(player, pos) : PlayerSelections.setSecond(player, pos);
        context.getSource().sendSuccess(() -> cornerMessage(first, pos, corners), false);
        return 1;
    }

    // "Corner 1 set to 1 2 3", plus the selection size once both corners are set
    public static Component cornerMessage(boolean first, BlockPos pos, PlayerSelections.Corners corners)
    {
        Component corner = Component.translatable(first ? "blueprint.minecraftmod.corner1" : "blueprint.minecraftmod.corner2", pos.getX(), pos.getY(), pos.getZ());
        Optional<BlueprintSelection> selection = corners.complete();
        if (selection.isEmpty())
            return corner;
        Vec3i size = selection.get().size();
        return Component.translatable("blueprint.minecraftmod.corner_with_size", corner, size.getX(), size.getY(), size.getZ());
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        PlayerSelections.clear(context.getSource().getPlayerOrException());
        context.getSource().sendSuccess(() -> Component.translatable("blueprint.minecraftmod.cleared"), false);
        return 1;
    }

    private static int save(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Optional<BlueprintSelection> selection = PlayerSelections.current(player);
        if (selection.isEmpty())
        {
            context.getSource().sendFailure(Component.translatable("blueprint.minecraftmod.error.no_selection"));
            return 0;
        }
        Blueprints.SaveResult result = Blueprints.save(player.serverLevel(), selection.get(), name, player.getGameProfile().getName());
        if (!result.isSuccess())
        {
            context.getSource().sendFailure(result.error());
            return 0;
        }
        context.getSource().sendSuccess(() -> savedMessage(result), true);
        return 1;
    }

    public static Component savedMessage(Blueprints.SaveResult result)
    {
        return Component.translatable("blueprint.minecraftmod.saved", result.id().toString(), result.file().toString());
    }

    private static int load(CommandContext<CommandSourceStack> context, ResourceLocation id, BlockPos origin)
    {
        CommandSourceStack source = context.getSource();
        Optional<Blueprint> blueprint = Blueprints.load(source.getServer(), id);
        if (blueprint.isEmpty())
        {
            source.sendFailure(Component.translatable("blueprint.minecraftmod.error.not_found", id.toString()));
            return 0;
        }
        BlueprintSelection placed = Blueprints.place(source.getLevel(), blueprint.get(), origin);
        // Select what was placed, so it is outlined and can be edited and saved again
        if (source.getEntity() instanceof ServerPlayer player)
            PlayerSelections.set(player, placed);
        Vec3i size = placed.size();
        source.sendSuccess(() -> Component.translatable("blueprint.minecraftmod.placed", id.toString(), origin.getX(), origin.getY(), origin.getZ(),
                size.getX(), size.getY(), size.getZ()), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context)
    {
        CommandSourceStack source = context.getSource();
        List<Blueprints.Listed> blueprints = Blueprints.list(source.getServer());
        if (blueprints.isEmpty())
        {
            source.sendSuccess(() -> Component.translatable("blueprint.minecraftmod.list.empty"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("blueprint.minecraftmod.list.header", blueprints.size()), false);
        for (Blueprints.Listed entry : blueprints)
            source.sendSuccess(() -> Component.translatable(entry.inWorld() ? "blueprint.minecraftmod.list.world" : "blueprint.minecraftmod.list.mod",
                    entry.id().toString()), false);
        return blueprints.size();
    }
}
