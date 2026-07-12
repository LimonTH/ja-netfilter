/*
 *
 *  * Original Code by Neo Peng pengzhile@gmail.com
 *  * Copyright (C) 2026 LimonTH (Modifications and updates)
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://gnu.org>.
 *
 */

package com.janetfilter.core.commons;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility for encrypting and decrypting configuration files using AES-128.
 * <p>
 * The encryption key is derived from a passphrase using SHA-256 hashing,
 * taking the first 16 bytes of the hash as the AES key.
 * The key is obtained from the {@code JANF_CONFIG_KEY} environment variable
 * or the {@code janf.config.key} system property.
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Encrypt a config file
 * byte[] key = ConfigCipher.getKey();
 * String encrypted = ConfigCipher.encrypt(plainText, key);
 * // Save as: ENC:<encrypted>
 *
 * // Decryption is automatic in ConfigParser when file starts with "ENC:"
 * }</pre>
 */
public final class ConfigCipher {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private ConfigCipher() {
        // Utility class, prevent instantiation
    }

    /**
     * Get the encryption key from the environment variable {@code JANF_CONFIG_KEY}
     * or the system property {@code janf.config.key}.
     * <p>
     * The passphrase is hashed with SHA-256 and the first 16 bytes are used as the AES-128 key.
     * </p>
     *
     * @return the 16-byte AES key, or {@code null} if no key is configured
     */
    public static byte[] getKey() {
        String keyStr = System.getProperty("janf.config.key");
        if (null == keyStr || keyStr.isEmpty()) {
            keyStr = System.getenv("JANF_CONFIG_KEY");
        }
        if (null == keyStr || keyStr.isEmpty()) {
            return null;
        }

        return deriveKey(keyStr);
    }

    /**
     * Check if an encryption key is configured.
     *
     * @return {@code true} if an encryption key is available, {@code false} otherwise
     */
    public static boolean isConfigured() {
        return getKey() != null;
    }

    /**
     * Encrypt plaintext content using AES-128/ECB/PKCS5Padding.
     * The result is Base64-encoded for safe storage in text files.
     *
     * @param plainText the plaintext to encrypt
     * @param key       the 16-byte AES encryption key
     * @return Base64-encoded ciphertext
     * @throws Exception if encryption fails (e.g., invalid key size)
     */
    public static String encrypt(String plainText, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypt a Base64-encoded ciphertext using AES-128/ECB/PKCS5Padding.
     *
     * @param cipherText the Base64-encoded ciphertext to decrypt
     * @param key        the 16-byte AES decryption key
     * @return decrypted plaintext
     * @throws Exception if decryption fails (e.g., wrong key or corrupted data)
     */
    public static String decrypt(String cipherText, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Derive a 16-byte AES key from a passphrase using SHA-256 hashing.
     * The passphrase is UTF-8 encoded, hashed with SHA-256, and the first
     * 16 bytes of the hash are returned.
     *
     * @param passphrase the passphrase to derive the key from
     * @return a 16-byte AES-128 key
     * @throws RuntimeException if SHA-256 is not available
     */
    private static byte[] deriveKey(String passphrase) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(passphrase.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, 16); // AES-128
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive key", e);
        }
    }
}