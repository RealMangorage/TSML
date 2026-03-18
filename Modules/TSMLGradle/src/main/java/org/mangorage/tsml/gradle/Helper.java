package org.mangorage.tsml.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    public static FileCollection decrypt(Project project) {
        // Directory where encrypted files live
        File encryptedDir = new File(project.getRootDir(), "encryptedFiles");
        if (!encryptedDir.exists()) {
            throw new RuntimeException("encryptedFiles directory does not exist");
        }

        // Output directory: build/run
        File runDir = new File(project.getBuildDir(), "run");
        if (!runDir.exists() && !runDir.mkdirs()) {
            throw new RuntimeException("Failed to create run directory: " + runDir);
        }

        // Delete old decrypted-* files
        File[] existing = runDir.listFiles((dir, name) -> name.startsWith("decrypted"));
        if (existing != null) {
            for (File f : existing) {
                if (!f.delete()) {
                    throw new RuntimeException("Failed to delete old file: " + f);
                }
            }
        }

        // Fetch token
        String token = project.findProperty("TSMLEncryptToken") != null
                ? project.findProperty("TSMLEncryptToken").toString()
                : System.getenv("TSMLEncryptToken");

        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Missing TSMLEncryptToken");
        }

        List<File> outputs = new ArrayList<>();

        File[] encryptedFiles = encryptedDir.listFiles((dir, name) -> name.endsWith(".enc"));
        if (encryptedFiles == null || encryptedFiles.length == 0) {
            return project.files(); // nothing to decrypt
        }

        for (File input : encryptedFiles) {
            try {
                // Strip .enc
                String baseName = input.getName().substring(0, input.getName().length() - 4);

                // Remove .jar if it’s already there to avoid .jar.jar
                if (baseName.endsWith(".jar")) {
                    baseName = baseName.substring(0, baseName.length() - 4);
                }

                // Final output name: decrypted-<name>.jar
                File output = new File(runDir, "decrypted-" + baseName + ".jar");

                // Perform decryption
                FileCrypto.decryptFile(input, output, token);

                outputs.add(output);

            } catch (Exception e) {
                throw new RuntimeException("Failed to decrypt: " + input, e);
            }
        }

        return project.files(outputs);
    }

    public static FileCollection getDecryptedFiles(Project project) {
        File runDir = new File(project.getBuildDir(), "run");

        if (!runDir.exists()) {
            return project.files(); // empty, nothing to return
        }

        File[] files = runDir.listFiles((dir, name) ->
                name.startsWith("decrypted") && name.endsWith(".jar")
        );

        if (files == null || files.length == 0) {
            return project.files(); // still empty
        }

        return project.files((Object[]) files);
    }
}
