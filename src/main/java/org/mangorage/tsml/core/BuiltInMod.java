package org.mangorage.tsml.core;

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
        System.out.println("Built-in mod initialized!");
    }

    @Override
    public boolean hasProperty(String property) {
        return property.contains("mixins");
    }
}
