package org.mangorage.tsml.internal.core.mod;

import org.mangorage.tsml.api.dependency.Dependency;
import org.mangorage.tsml.api.mod.IModContainer;

import java.util.List;
import java.util.Optional;

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
            instance = modClass.getConstructor().newInstance();
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
    public List<Dependency> getDependencies() {
        return modInfo.dependencies();
    }


    @Override
    public List<String> getAuthors() {
        return modInfo.authors();
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
    public <T> Optional<T> getProperty(String property, Class<T> type) {
        if (modInfo.properties() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(modInfo.properties().get(property)).filter(type::isInstance).map(type::cast);
    }
}
