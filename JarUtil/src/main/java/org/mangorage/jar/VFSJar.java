package org.mangorage.jar;

import org.apache.commons.vfs2.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public final class VFSJar implements IJar {

    // -------------------- Static Factories --------------------

    /** For physical jars on the disk (Your F: drive projects) */
    public static IJar create(Path path) {
        try {
            FileSystemManager manager = VFS.getManager();
            // jar:file:///F:/.../TSML.jar!/
            String uri = "jar:" + path.toUri().toString() + "!/";
            return new VFSJar(manager.resolveFile(uri));
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    public static IJar create(URL url) {
        return create(
                new File(
                        url.getFile()
                ).toPath()

        );
    }

    /** For creating the wrapper around a VFS FileObject (used internally for nesting) */
    private static IJar create(FileObject fileObject) {
        return new VFSJar(fileObject);
    }

    // -------------------- Implementation --------------------

    private final FileObject root;

    private VFSJar(FileObject root) {
        this.root = root;
    }

    @Override
    public String getName() {
        // Returns the filename (e.g., "example.jar")
        return root.getName().getBaseName();
    }

    @Override
    public URL getURL() {
        try {
            return root.getURL();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IJar getNestedJar(String path) throws IOException {
        FileObject nestedFile = root.resolveFile(path);
        if (!nestedFile.exists()) return null;

        // The "Secret Sauce": Take the nested file and wrap it in its own JAR layer
        FileSystemManager manager = VFS.getManager();
        String nestedUri = "jar:" + nestedFile.getName().getURI() + "!/";
        return new VFSJar(manager.resolveFile(nestedUri));
    }

    @Override
    public List<IJar> getNestedJars() {
        try {
            // Find all .jar files inside this jar
            return Arrays.stream(root.findFiles(new FileExtensionSelector("jar")))
                    .filter(f -> !f.equals(root)) // Don't include yourself
                    .map(f -> {
                        try {
                            // Convert the relative path into a new VFSJar
                            return getNestedJar(root.getName().getRelativeName(f.getName()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            return root.resolveFile(path).exists();
        } catch (FileSystemException e) {
            return false;
        }
    }

    @Override
    public InputStream getInputStream(String path) {
        try {
            FileObject file = root.resolveFile(path);
            return file.exists() ? file.getContent().getInputStream() : null;
        } catch (FileSystemException e) {
            return null;
        }
    }

    @Override
    public byte[] readBytes(String path) {
        try (InputStream in = getInputStream(path)) {
            return (in != null) ? in.readAllBytes() : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public List<String> listEntries(String directory) {
        try {
            FileObject dir = root.resolveFile(directory);
            return Arrays.stream(dir.getChildren())
                    .map(f -> {
                        try {
                            return root.getName().getRelativeName(f.getName());
                        } catch (FileSystemException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (FileSystemException e) {
            return List.of();
        }
    }

    @Override
    public List<String> listEntries() {
        return listEntries("/");
    }

    @Override
    public boolean isDirectory(String path) {
        try {
            return root.resolveFile(path).getType() == FileType.FOLDER;
        } catch (FileSystemException e) {
            return false;
        }
    }

    @Override
    public Manifest getManifest() throws IOException {
        try (InputStream in = getInputStream("META-INF/MANIFEST.MF")) {
            return (in != null) ? new Manifest(in) : null;
        }
    }
}