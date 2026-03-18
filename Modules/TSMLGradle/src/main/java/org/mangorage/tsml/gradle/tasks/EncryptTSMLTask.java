package org.mangorage.tsml.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.mangorage.tsml.gradle.DevConfig;
import org.mangorage.tsml.gradle.util.EncryptionUtil;
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
        DevConfig config = TSMLGradlePlugin.getDevConfig();

        // Fetch token
        String token = (String) getProject().findProperty("TSMLEncryptToken");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Missing TSMLEncryptToken environment variable");
        }

        File baseOutputDir = new File(getProject().getRootDir(), "encryptedFiles");

        // Encrypt client file into encryptedFiles/client/
        File clientFile = config.getClientFile();
        if (clientFile != null && clientFile.exists()) {
            File clientDir = new File(baseOutputDir, "client");
            if (!clientDir.exists() && !clientDir.mkdirs()) {
                throw new RuntimeException("Failed to create client output directory: " + clientDir);
            }

            File encryptedClient = new File(clientDir, clientFile.getName() + ".enc");
            EncryptionUtil.encryptFile(clientFile, encryptedClient, token);
            getLogger().lifecycle("Encrypted client: {} -> {}", clientFile.getName(), encryptedClient.getPath());
        }

        // Encrypt server file into encryptedFiles/server/
        File serverFile = config.getServerFile();
        if (serverFile != null && serverFile.exists()) {
            File serverDir = new File(baseOutputDir, "server");
            if (!serverDir.exists() && !serverDir.mkdirs()) {
                throw new RuntimeException("Failed to create server output directory: " + serverDir);
            }

            File encryptedServer = new File(serverDir, serverFile.getName() + ".enc");
            EncryptionUtil.encryptFile(serverFile, encryptedServer, token);
            getLogger().lifecycle("Encrypted server: {} -> {}", serverFile.getName(), encryptedServer.getPath());
        }

        getLogger().lifecycle("Encryption complete.");
    }
}
