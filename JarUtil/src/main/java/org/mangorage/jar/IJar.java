package org.mangorage.jar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.jar.Manifest;

public interface IJar {

    String getName();

    /** URL of this jar (or nested jar entry) */
    URL getURL();

    /** Get a nested jar by path, e.g. "libs/foo.jar" */
    IJar getNestedJar(String path) throws IOException;

    List<IJar> getNestedJars();

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

    Manifest getManifest() throws IOException;

    default String getManifestAttribute(String attribute) throws IOException {
        final var manifest = getManifest();
        if (manifest == null) return null;
        return manifest.getMainAttributes().getValue(attribute);
    }
}