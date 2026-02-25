package org.mangorage.tsml.bootstrap.internal.core.nested.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public interface IJar {

    String getName();

    /** URL of this jar (or nested jar entry) */
    URL getURL();

    /** Get a nested jar by path, e.g. "libs/foo.jar" */
    IJar getNestedJar(String path) throws IOException;

    /** List all nested jars inside this jar */
    List<IJar> getNestedJars() throws IOException;

    /* =========================
       File access API
       ========================= */

    /** Returns true if this path exists in the jar */
    boolean exists(String path);

    /** Open a file inside the jar as a stream */
    InputStream getInputStream(String path);

    /** Read a file fully into memory */
    byte[] readBytes(String path);

    /** List all entries in this jar (files + dirs) */
    List<String> listEntries();

    /** List entries under a directory (e.g. "assets/") */
    List<String> listEntries(String directory);

    /** True if entry is a directory */
    boolean isDirectory(String path);
}