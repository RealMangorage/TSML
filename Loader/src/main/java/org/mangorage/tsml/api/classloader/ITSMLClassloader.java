package org.mangorage.tsml.api.classloader;

import org.mangorage.jar.IJar;

import java.util.List;

public interface ITSMLClassloader {
    byte[] getClassBytes(String name);
    boolean hasClass(String name);

    List<IJar> getJars();
}
