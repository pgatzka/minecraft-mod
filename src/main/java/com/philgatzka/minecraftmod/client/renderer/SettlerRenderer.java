package com.philgatzka.minecraftmod.client.renderer;

import com.philgatzka.minecraftmod.entity.SettlerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

// Renders settlers with the player model and the default Steve skin
public class SettlerRenderer extends HumanoidMobRenderer<SettlerEntity, PlayerModel<SettlerEntity>>
{
    public SettlerRenderer(EntityRendererProvider.Context context)
    {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SettlerEntity entity)
    {
        return DefaultPlayerSkin.getDefaultSkin();
    }
}
