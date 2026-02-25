package org.mangorage.tsml.api.classloader;

import org.mangorage.tsml.internal.core.nested.api.IJar;

import java.util.List;

public interface ITSMLClassloader {
    byte[] getClassBytes(String name);
    boolean hasClass(String name);
    List<IJar> getJars();
}
