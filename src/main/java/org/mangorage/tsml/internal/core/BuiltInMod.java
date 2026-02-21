package org.mangorage.tsml.internal.core;

import org.mangorage.tsml.TSMLLogger;
import org.mangorage.tsml.api.IMod;

public class BuiltInMod implements IMod {
    @Override
    public String getName() {
        return "BuiltInModTSML";
    }

    @Override
    public String getId() {
        return "built-in-tsml";
    }

    @Override
    public String getDescription() {
        return "This is a built-in mod for TSML.";
    }

    @Override
    public void onInitialize() {
        TSMLLogger.get().info("Built-in mod initialized!");
    }

    @Override
    public boolean hasProperty(String property) {
        return property.contains("mixins");
    }
}
