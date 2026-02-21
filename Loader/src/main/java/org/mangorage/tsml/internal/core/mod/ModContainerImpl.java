package org.mangorage.tsml.internal.core.mod;

import org.mangorage.tsml.api.IModContainer;

import java.util.List;

public final class ModContainerImpl implements IModContainer {
    private final ModInfo modInfo;
    private final Class<?> modClass;
    private Object instance;

    ModContainerImpl(ModInfo modInfo, Class<?> modClass) {
        this.modInfo = modInfo;
        this.modClass = modClass;
    }


    void init() {
        try {
            modClass.getConstructor().newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getInstance() {
        return instance;
    }

    @Override
    public String getId() {
        return modInfo.id();
    }

    @Override
    public String getDescription() {
        return modInfo.description();
    }

    @Override
    public String getName() {
        return modInfo.name();
    }

    @Override
    public String getVersion() {
        return modInfo.version();
    }

    @Override
    public <T, D> T getProperty(String property, Class<D> type) {
        if (modInfo.properties() == null) return null;
        if (property.equals("mixins") && type == List.class) {
            return (T) modInfo.properties().get("mixins");
        }
        return null;
    }

}
