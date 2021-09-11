package net.coderbot.batchedentityrendering.impl.ordering;

import net.coderbot.batchedentityrendering.impl.BlendingStateHolder;
import net.coderbot.batchedentityrendering.impl.TransparencyType;
import net.coderbot.batchedentityrendering.impl.WrappableRenderLayer;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;

public class TranslucencyRenderOrderManager implements RenderOrderManager {
    private final EnumMap<TransparencyType, LinkedHashSet<RenderType>> layers;

    public TranslucencyRenderOrderManager() {
        layers = new EnumMap<>(TransparencyType.class);

        for (TransparencyType type : TransparencyType.values()) {
            layers.put(type, new LinkedHashSet<>());
        }
    }

    private static TransparencyType getTransparencyType(RenderType layer) {
        while (layer instanceof WrappableRenderLayer) {
            layer = ((WrappableRenderLayer) layer).unwrap();
        }

        if (layer instanceof BlendingStateHolder) {
            return ((BlendingStateHolder) layer).getTransparencyType();
        }

        // Default to "generally transparent" if we can't figure it out.
        return TransparencyType.GENERAL_TRANSPARENT;
    }

    public void begin(RenderType layer) {
        layers.get(getTransparencyType(layer)).add(layer);
    }

    public void startGroup() {
        // no-op
    }

    public boolean maybeStartGroup() {
        // no-op
        return false;
    }

    public void endGroup() {
        // no-op
    }

    @Override
    public void reset() {
        layers.forEach((type, set) -> {
            set.clear();
        });
    }

    public Iterable<RenderType> getRenderOrder() {
        int layerCount = 0;

        for (LinkedHashSet<RenderType> set : layers.values()) {
            layerCount += set.size();
        }

        List<RenderType> allLayers = new ArrayList<>(layerCount);

        for (LinkedHashSet<RenderType> set : layers.values()) {
            allLayers.addAll(set);
        }

        return allLayers;
    }
}
