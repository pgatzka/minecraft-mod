package com.philgatzka.minecraftmod.block;

import com.philgatzka.minecraftmod.block.entity.WorkplaceBlockEntity;
import com.philgatzka.minecraftmod.construction.ConstructionSites;
import com.philgatzka.minecraftmod.menu.WorkplaceMenu;
import com.philgatzka.minecraftmod.settlement.Settlements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// A settlement building with a job; right-click opens the worker selection screen
public abstract class WorkplaceBlock extends BaseEntityBlock
{
    protected WorkplaceBlock(Properties properties)
    {
        super(properties);
    }

    // BaseEntityBlock defaults to INVISIBLE, which expects a BlockEntityRenderer; use the regular JSON model instead
    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    // Placing it joins the placer's settlement
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && placer instanceof Player player && level.getBlockEntity(pos) instanceof WorkplaceBlockEntity workplace)
            Settlements.claimWorkplace(serverLevel, player, workplace);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (player instanceof ServerPlayer serverPlayer)
            WorkplaceMenu.open(serverPlayer, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel)
        {
            if (level.getBlockEntity(pos) instanceof WorkplaceBlockEntity workplace)
                Settlements.onWorkplaceRemoved(serverLevel, workplace);
            ConstructionSites.onCityBlockRemoved(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
