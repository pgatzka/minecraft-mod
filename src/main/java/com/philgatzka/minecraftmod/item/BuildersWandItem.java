package com.philgatzka.minecraftmod.item;

import com.philgatzka.minecraftmod.blueprint.Blueprint;
import com.philgatzka.minecraftmod.blueprint.Blueprints;
import com.philgatzka.minecraftmod.construction.ConstructionSites;
import com.philgatzka.minecraftmod.menu.BlueprintPickerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

// Places blueprints as construction sites: right-click the air = choose a blueprint, R = rotate, right-click a block = place.
// The chosen blueprint and rotation are stored on the item, so the client can preview them.
public class BuildersWandItem extends Item
{
    private static final String BLUEPRINT_KEY = "Blueprint";
    private static final String ROTATION_KEY = "Rotation";

    public BuildersWandItem(Properties properties)
    {
        super(properties);
    }

    @Nullable
    public static ResourceLocation getBlueprint(ItemStack stack)
    {
        return stack.getTag() != null && stack.getTag().contains(BLUEPRINT_KEY) ? ResourceLocation.tryParse(stack.getTag().getString(BLUEPRINT_KEY)) : null;
    }

    public static void setBlueprint(ItemStack stack, ResourceLocation id)
    {
        stack.getOrCreateTag().putString(BLUEPRINT_KEY, id.toString());
    }

    public static Rotation getRotation(ItemStack stack)
    {
        if (stack.getTag() == null || !stack.getTag().contains(ROTATION_KEY))
            return Rotation.NONE;
        try
        {
            return Rotation.valueOf(stack.getTag().getString(ROTATION_KEY));
        }
        catch (IllegalArgumentException e)
        {
            return Rotation.NONE;
        }
    }

    public static Rotation rotate(ItemStack stack)
    {
        Rotation rotation = getRotation(stack).getRotated(Rotation.CLOCKWISE_90);
        stack.getOrCreateTag().putString(ROTATION_KEY, rotation.name());
        return rotation;
    }

    // The Builder's Wand in the main hand, else in the off hand, else EMPTY
    public static ItemStack findHeld(Player player)
    {
        for (InteractionHand hand : InteractionHand.values())
            if (player.getItemInHand(hand).getItem() instanceof BuildersWandItem)
                return player.getItemInHand(hand);
        return ItemStack.EMPTY;
    }

    // Where the city block goes when clicking a block face: into a replaceable block (grass, snow, ...) or in front of the face
    public static BlockPos targetFor(Level level, BlockPos clicked, Direction face)
    {
        return level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
    }

    public static Component rotationName(Rotation rotation)
    {
        return Component.translatable("construction.minecraftmod.rotation." + rotation.name().toLowerCase());
    }

    // Runs before the clicked block's own right-click action, so sites can be placed against chests, Town Halls, ...
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        if (context.getPlayer() instanceof ServerPlayer player)
            place(player, stack, targetFor(context.getLevel(), context.getClickedPos(), context.getClickedFace()));
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static void place(ServerPlayer player, ItemStack stack, BlockPos target)
    {
        ResourceLocation id = getBlueprint(stack);
        if (id == null)
        {
            player.displayClientMessage(Component.translatable("construction.minecraftmod.error.no_blueprint"), true);
            return;
        }
        Optional<Blueprint> blueprint = Blueprints.load(player.server, id);
        if (blueprint.isEmpty())
        {
            player.displayClientMessage(Component.translatable("blueprint.minecraftmod.error.not_found", id.toString()), true);
            return;
        }
        ConstructionSites.Result result = ConstructionSites.place(player, player.serverLevel(), blueprint.get(), target, getRotation(stack));
        player.displayClientMessage(result.isSuccess()
                ? Component.translatable("construction.minecraftmod.placed", blueprint.get().cityBlock().getName())
                : result.error(), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer)
            BlueprintPickerMenu.open(serverPlayer, getBlueprint(stack));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        ResourceLocation id = getBlueprint(stack);
        if (id != null)
            tooltip.add(Component.translatable("item.minecraftmod.builders_wand.tooltip.selected", id.toString(), rotationName(getRotation(stack))));
        tooltip.add(Component.translatable("item.minecraftmod.builders_wand.tooltip.choose"));
        tooltip.add(Component.translatable("item.minecraftmod.builders_wand.tooltip.rotate"));
        tooltip.add(Component.translatable("item.minecraftmod.builders_wand.tooltip.place"));
        tooltip.add(Component.translatable("item.minecraftmod.builders_wand.tooltip.territory"));
    }
}
