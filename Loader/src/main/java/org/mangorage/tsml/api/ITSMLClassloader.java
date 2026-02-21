package org.mangorage.tsml.api;

public interface ITSMLClassloader {
    byte[] getClassBytes(String name);
    boolean hasClass(String name);
}
