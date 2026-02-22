package org.mangorage.tsml.internal.core.classloader;

import java.net.URL;

public record ClassBytesWithCodeSource(byte[] bytes, URL originJar) {}