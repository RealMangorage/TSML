package org.mangorage.tsml.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.bundling.Jar;
import org.mangorage.tsml.gradle.util.EncryptionUtil;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public final class Helper {
    public static void shade(Project project, Jar jar, Configuration configuration) {
        // Use a Callable to lazily resolve the zipTree collection
        jar.from((Callable<Object>) () ->
                        configuration
                                .getFiles()
                                .stream()
                                .map(project::zipTree)
                                .collect(Collectors.toList()),
                copySpec -> {
                    copySpec.into("");
                    copySpec.exclude("META-INF/MANIFEST.MF");
                }
        );
    }

    public static void merge(Project project, Jar jar, Configuration config) {
        // This will only resolve the files when the Jar task executes
        jar.from(config, copySpec -> {
            copySpec.into("JarJarLoader");
        });
    }



    /** Lazily decrypts and returns the client file as a FileCollection. */
    public static FileCollection getClient(Project project) {
        final var config = TSMLGradlePlugin.getDevConfig();
        File decrypted = decryptIfNeeded(project, config.getClientFile(), "client");
        return project.files(decrypted);
    }

    /** Lazily decrypts and returns the server file as a FileCollection. */
    public static FileCollection getServer(Project project) {
        final var config = TSMLGradlePlugin.getDevConfig();
        File decrypted = decryptIfNeeded(project, config.getServerFile(), "server");
        return project.files(decrypted);
    }

    /** Core lazy decryption logic. Finds encrypted file automatically and decrypts if needed. */
    private static File decryptIfNeeded(Project project, File originalFile, String type) {
        // Just call the new helper — it handles locating the encrypted file, lazy decryption, token, and output path
        return EncryptionUtil.decrypt(project, originalFile, type);
    }
}
