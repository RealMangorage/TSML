package org.mangorage.tsml.api.classloader;

import java.net.URL;

public interface ITSMLClassloader {
    byte[] getClassBytes(String name);
    boolean hasClass(String name);

    URL[] getUrls();
}
