package org.mangorage.tsml.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.mangorage.tsml.gradle.FileCrypto;
import org.mangorage.tsml.gradle.TSMLGradlePlugin;

import javax.inject.Inject;
import java.io.File;

public abstract class EncryptTSMLTask extends DefaultTask {
    @Inject
    public EncryptTSMLTask() {
        setGroup("tsml-dev");
    }


    @TaskAction
    public void exec() {
        final var files = TSMLGradlePlugin.getDevConfig().getFileSet();
        final var token = (String) getProject().findProperty("TSMLEncryptToken");

        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Missing TSMLEncryptToken environment variable");
        }

        // Root project output folder
        File outputDir = new File(getProject().getRootDir(), "encryptedFiles");

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new RuntimeException("Failed to create output directory: " + outputDir);
        }

        getLogger().lifecycle("Encrypting {} files into {}", files.size(), outputDir);

        for (File input : files) {
            if (!input.exists()) {
                getLogger().warn("Skipping missing file: {}", input);
                continue;
            }

            // Preserve filename + add .enc
            File output = new File(outputDir, input.getName() + ".enc");

            try {
                FileCrypto.encryptFile(input, output, token);
                getLogger().lifecycle("Encrypted: {} -> {}", input.getName(), output.getName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to encrypt: " + input, e);
            }
        }

        getLogger().lifecycle("Encryption complete.");
    }
}
