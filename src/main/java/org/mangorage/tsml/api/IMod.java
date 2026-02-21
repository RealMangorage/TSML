package org.mangorage.tsml.api;

public interface IMod {
    String getName();
    String getId();
    String getDescription();

    default boolean hasProperty(String property) {
        return false;
    }

    void onInitialize();
}
