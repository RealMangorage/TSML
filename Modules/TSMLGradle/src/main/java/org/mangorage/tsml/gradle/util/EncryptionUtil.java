package org.mangorage.tsml.gradle.util;

import org.gradle.api.Project;

import java.io.File;

public final class EncryptionUtil {

    /**
     * Lazily decrypts an encrypted file and returns the decrypted file.
     * Decrypts into build/decrypted/<type>/<name> if not already decrypted.
     *
     * @param project The Gradle project
     * @param originalFile The original file (used to locate encrypted version)
     * @param type "client" or "server" (used for subdirectory)
     * @return The decrypted File
     */
    public static File decrypt(Project project, File originalFile, String type) {
        if (originalFile == null) {
            throw new RuntimeException("Original file cannot be null");
        }

        String fileName = originalFile.getName();

        // Encrypted file location: encryptedFiles/<type>/<name>.enc
        File encryptedFile = new File(project.getRootDir(), "encryptedFiles/" + type + "/" + fileName + ".enc");
        if (!encryptedFile.exists()) {
            throw new RuntimeException("Encrypted file not found: " + encryptedFile);
        }

        // Output directory: build/decrypted/<type>
        File decryptedDir = new File(project.getBuildDir(), "decrypted/" + type);
        if (!decryptedDir.exists() && !decryptedDir.mkdirs()) {
            throw new RuntimeException("Failed to create decrypted directory: " + decryptedDir);
        }

        File decryptedFile = new File(decryptedDir, fileName);
        if (!decryptedFile.exists()) {
            String token = project.findProperty("TSMLEncryptToken") != null
                    ? project.findProperty("TSMLEncryptToken").toString()
                    : System.getenv("TSMLEncryptToken");

            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Missing TSMLEncryptToken");
            }

            try {
                FileCrypto.decryptFile(encryptedFile, decryptedFile, token);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decrypt file: " + encryptedFile, e);
            }
        }

        return decryptedFile;
    }

    /**
     * Encrypts a single file to the specified output.
     *
     * @param inputFile  The file to encrypt
     * @param outputFile The destination file (will be overwritten if exists)
     * @param token      The encryption token
     */
    public static void encryptFile(File inputFile, File outputFile, String token) {
        if (inputFile == null || !inputFile.exists()) {
            throw new RuntimeException("Input file does not exist: " + inputFile);
        }

        if (outputFile == null) {
            throw new RuntimeException("Output file cannot be null");
        }

        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Missing encryption token");
        }

        // Ensure output directory exists
        File parent = outputFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Failed to create output directory: " + parent);
        }

        try {
            FileCrypto.encryptFile(inputFile, outputFile, token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt file: " + inputFile, e);
        }
    }
}
