package com.philgatzka.minecraftmod.item;

import com.philgatzka.minecraftmod.blueprint.BlueprintCommands;
import com.philgatzka.minecraftmod.blueprint.BlueprintSelection;
import com.philgatzka.minecraftmod.blueprint.Blueprints;
import com.philgatzka.minecraftmod.blueprint.PlayerSelections;
import com.philgatzka.minecraftmod.menu.BlueprintMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

// Marks a blueprint area: right-click a block = corner 1, sneak + right-click a block = corner 2, right-click the air = save screen
public class BlueprintWandItem extends Item
{
    public BlueprintWandItem(Properties properties)
    {
        super(properties);
    }

    // Runs before the clicked block's own right-click action, so corners can be set on chests, Town Halls, ...
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        if (context.getPlayer() instanceof ServerPlayer player)
        {
            if (!Blueprints.canUse(player))
            {
                player.displayClientMessage(Component.translatable("blueprint.minecraftmod.error.no_permission"), true);
                return InteractionResult.FAIL;
            }
            BlockPos pos = context.getClickedPos();
            boolean first = !player.isShiftKeyDown();
            PlayerSelections.Corners corners = first ? PlayerSelections.setFirst(player, pos) : PlayerSelections.setSecond(player, pos);
            player.displayClientMessage(BlueprintCommands.cornerMessage(first, pos, corners), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer)
            openSaveScreen(serverPlayer);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void openSaveScreen(ServerPlayer player)
    {
        if (!Blueprints.canUse(player))
        {
            player.displayClientMessage(Component.translatable("blueprint.minecraftmod.error.no_permission"), true);
            return;
        }
        Optional<BlueprintSelection> selection = PlayerSelections.current(player);
        if (selection.isEmpty())
        {
            player.displayClientMessage(Component.translatable("blueprint.minecraftmod.error.no_selection"), true);
            return;
        }
        Blueprints.Analysis analysis = Blueprints.analyze(player.level(), selection.get());
        if (!analysis.isValid())
        {
            player.displayClientMessage(analysis.error(), true);
            return;
        }
        BlueprintMenu.open(player, selection.get(), analysis);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("item.minecraftmod.blueprint_wand.tooltip.corner1"));
        tooltip.add(Component.translatable("item.minecraftmod.blueprint_wand.tooltip.corner2"));
        tooltip.add(Component.translatable("item.minecraftmod.blueprint_wand.tooltip.save"));
    }
}
