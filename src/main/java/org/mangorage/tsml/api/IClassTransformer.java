package org.mangorage.tsml.api;

public interface IClassTransformer {
    byte[] transform(String className, byte[] classData);
}