package net.coderbot.batchedentityrendering.impl.wrappers;

import net.coderbot.batchedentityrendering.impl.WrappableRenderLayer;
import net.coderbot.batchedentityrendering.mixin.RenderLayerAccessor;
import net.coderbot.iris.mixin.rendertype.RenderTypeAccessor;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class TaggingRenderLayerWrapper extends RenderType implements WrappableRenderLayer {
    private final int tag;
    private final RenderType wrapped;

    public TaggingRenderLayerWrapper(String name, RenderType wrapped, int tag) {
        super(name, wrapped.format(), wrapped.mode(), wrapped.bufferSize(),
                wrapped.affectsCrumbling(), isTranslucent(wrapped), wrapped::setupRenderState, wrapped::clearRenderState);

        this.tag = tag;
        this.wrapped = wrapped;
    }

    @Override
    public RenderType unwrap() {
        return this.wrapped;
    }

    @Override
    public Optional<RenderType> outline() {
        return this.wrapped.outline();
    }

    @Override
    public boolean isOutline() {
        return this.wrapped.isOutline();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == null) {
            return false;
        }

        if (object.getClass() != this.getClass()) {
            return false;
        }

        TaggingRenderLayerWrapper other = (TaggingRenderLayerWrapper) object;

        return this.tag == other.tag && Objects.equals(this.wrapped, other.wrapped);
    }

    @Override
    public int hashCode() {
        // Add one so that we don't have the exact same hash as the wrapped object.
        // This means that we won't have a guaranteed collision if we're inserted to a map alongside the unwrapped object.
        return this.wrapped.hashCode() + 1;
    }

    @Override
    public String toString() {
        return "tagged(" +tag+ "):" + this.wrapped.toString();
    }

    private static boolean isTranslucent(RenderType layer) {
        return ((RenderTypeAccessor) layer).isTranslucent();
    }
}
