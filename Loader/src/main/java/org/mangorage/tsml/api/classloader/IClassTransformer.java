package org.mangorage.tsml.api.classloader;

public interface IClassTransformer {
    byte[] transform(String className, byte[] classData);
}