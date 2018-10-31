package org.ibatis.persist.impl;

public interface Renderable {
    public void render(RenderingContext rc);
    public void renderProjection(RenderingContext rc);
    public void renderFrom(RenderingContext rc);
}
