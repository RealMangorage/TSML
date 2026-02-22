package org.mangorage.tsml.api.classloader;

public interface ITSMLClassloader {
    byte[] getClassBytes(String name);
    boolean hasClass(String name);
}
