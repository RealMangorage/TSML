package org.mangorage.tsml.api.mod;

import org.mangorage.tsml.api.dependency.Dependency;

import java.util.List;
import java.util.Optional;

public interface IModContainer {
    Object getInstance();

    String getId();
    String getName();
    String getVersion();
    String getDescription();
    List<Dependency> getDependencies();
    List<String> getAuthors();

    default <T> Optional<List<T>> getPropertyList(String property, Class<T> type) {
        return getProperty(property, List.class).map(list -> (List<T>) list);
    }

    <T> Optional<T> getProperty(String property, Class<T> type);
}
