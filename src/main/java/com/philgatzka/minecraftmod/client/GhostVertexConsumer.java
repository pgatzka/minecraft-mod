package com.philgatzka.minecraftmod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

// Passes vertices through with their alpha scaled, so block models render see-through ("ghost blocks")
public class GhostVertexConsumer implements VertexConsumer
{
    private final VertexConsumer delegate;
    private final float alpha;

    public GhostVertexConsumer(VertexConsumer delegate, float alpha)
    {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    // Block models are written through this method (see VertexConsumer.putBulkData)
    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light,
                       float normalX, float normalY, float normalZ)
    {
        delegate.vertex(x, y, z, red, green, blue, alpha * this.alpha, u, v, overlay, light, normalX, normalY, normalZ);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        delegate.color(red, green, blue, (int) (alpha * this.alpha));
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v)
    {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v)
    {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v)
    {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex()
    {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha)
    {
        delegate.defaultColor(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor()
    {
        delegate.unsetDefaultColor();
    }
}
