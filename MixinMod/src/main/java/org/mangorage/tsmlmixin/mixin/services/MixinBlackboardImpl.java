package org.mangorage.tsmlmixin.mixin.services;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the mixin blackboard provider.
 *
 * @author vectrix
 * @since 1.0.0
 */
public final class MixinBlackboardImpl implements IGlobalPropertyService {
    private final Map<IPropertyKey, Object> map = new HashMap<>();

    public MixinBlackboardImpl() {
    }

    public record Key(String name) implements IPropertyKey {}

    @Override
    public IPropertyKey resolveKey(final  String name) {
        return new Key(name);
    }

    @Override
    public <T> T getProperty(final IPropertyKey key) {
        return this.getProperty(key, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setProperty(final IPropertyKey key, final Object other) {
        map.put(key, other);
    }

    @Override
    public String getPropertyString(final IPropertyKey key, final String defaultValue) {
        return this.getProperty(key, defaultValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(final IPropertyKey key, final T defaultValue) {
        return (T) map.getOrDefault(key, defaultValue);
    }

}
