package org.mangorage.jar;

import org.apache.commons.vfs2.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public final class VFSJar implements IJar {

    // -------------------- Static Factories --------------------

    /** For physical jars on disk */
    public static IJar create(Path path) {
        try {
            FileSystemManager manager = VFS.getManager();
            String uri = "jar:" + path.toUri().toString() + "!/";
            return new VFSJar(manager.resolveFile(uri));
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    public static IJar create(URL url) {
        return create(new File(url.getFile()).toPath());
    }

    /** Internal wrapper for FileObject */
    private static IJar create(FileObject fileObject) {
        return new VFSJar(fileObject);
    }

    // -------------------- Implementation --------------------

    private final FileObject root;
    private final Map<String, FileObject> fileCache = new ConcurrentHashMap<>();
    private final Map<String, IJar> nestedJarCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> dirCache = new ConcurrentHashMap<>();

    private VFSJar(FileObject root) {
        this.root = root;
    }

    @Override
    public String getName() {
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

    /** Resolve a file path with caching */
    private FileObject resolveCached(String path) throws FileSystemException {
        return fileCache.computeIfAbsent(path, p -> {
            try { return root.resolveFile(p); }
            catch (FileSystemException e) { throw new RuntimeException(e); }
        });
    }

    @Override
    public IJar getNestedJar(String path) throws IOException {
        return nestedJarCache.computeIfAbsent(path, p -> {
            try {
                FileObject nestedFile = resolveCached(p);
                if (!nestedFile.exists()) return null;
                String nestedUri = "jar:" + nestedFile.getName().getURI() + "!/";
                return new VFSJar(VFS.getManager().resolveFile(nestedUri));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public List<IJar> getNestedJars() {
        try {
            return Arrays.stream(root.findFiles(new FileExtensionSelector("jar")))
                    .filter(f -> !f.equals(root))
                    .map(f -> {
                        try {
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

    /**
     * Returns the URL of a single resource inside this JAR.
     * Format: jar:<jar-url>!/<resource-path>
     */
    public URL findResource(String name) {
        if (!exists(name)) return null;

        try {
            return resolveCached(name).getURL();
        } catch (FileSystemException e) {
            return null;
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            return resolveCached(path).exists();
        } catch (FileSystemException e) {
            return false;
        }
    }

    @Override
    public InputStream getInputStream(String path) {
        try {
            FileObject file = resolveCached(path);
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
        return dirCache.computeIfAbsent(directory, dir -> {
            try {
                FileObject dirFile = resolveCached(dir);
                if (!dirFile.exists() || dirFile.getType() != FileType.FOLDER) return List.of();
                return Arrays.stream(dirFile.getChildren())
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
        });
    }

    @Override
    public List<String> listEntries() {
        return listEntries("/");
    }

    @Override
    public boolean isDirectory(String path) {
        try {
            return resolveCached(path).getType() == FileType.FOLDER;
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