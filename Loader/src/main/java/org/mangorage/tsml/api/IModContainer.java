package org.mangorage.tsml.api;

public interface IModContainer {
    Object getInstance();

    String getId();
    String getDescription();
    String getName();
    String getVersion();

    <T, D> T getProperty(String property, Class<D> type);
}
