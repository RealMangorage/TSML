package org.mangorage.tsml.gradle;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public class FileCrypto {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12; // recommended for GCM
    private static final int TAG_SIZE = 128;

    // Derive AES key from token
    private static SecretKey keyFromToken(String token) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes("UTF-8"));
        return new SecretKeySpec(Arrays.copyOf(hash, 16), "AES"); // 128-bit key
    }

    public static void encryptFile(File input, File output, String token) throws Exception {
        SecretKey key = keyFromToken(token);

        Cipher cipher = Cipher.getInstance(ALGO);

        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        try (FileOutputStream fos = new FileOutputStream(output);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher);
             FileInputStream fis = new FileInputStream(input)) {

            // Write IV at the start of file
            fos.write(iv);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, read);
            }
        }
    }

    public static void decryptFile(File input, File output, String token) throws Exception {
        SecretKey key = keyFromToken(token);

        try (FileInputStream fis = new FileInputStream(input)) {

            byte[] iv = new byte[IV_SIZE];
            if (fis.read(iv) != IV_SIZE) {
                throw new RuntimeException("Invalid encrypted file.");
            }

            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                 FileOutputStream fos = new FileOutputStream(output)) {

                byte[] buffer = new byte[4096];
                int read;
                while ((read = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
        }
    }
}