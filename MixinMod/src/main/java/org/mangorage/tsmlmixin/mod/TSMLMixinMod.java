package org.mangorage.tsmlmixin.mod;

import org.mangorage.tsml.api.IMod;

public final class TSMLMixinMod implements IMod {
    @Override
    public String getName() {
        return "TSMLMixinMod";
    }

    @Override
    public String getId() {
        return "tsmlmixin";
    }

    @Override
    public String getDescription() {
        return "Adds support for Mixins in TSML.";
    }

    @Override
    public void onInitialize() {

    }
}
