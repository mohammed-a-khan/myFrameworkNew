package com.testforge.cs.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encryption and Decryption utility for CS Framework
 * Supports automatic encryption/decryption of values marked with ENC() prefix
 * Uses AES-256-GCM for strong encryption
 */
public class CSEncryptionUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSEncryptionUtils.class);
    
    // Encryption constants
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    
    // Pattern to detect encrypted values
    private static final Pattern ENCRYPTED_PATTERN = Pattern.compile("ENC\\(([^)]+)\\)");
    private static final String ENCRYPTED_PREFIX = "ENC(";
    private static final String ENCRYPTED_SUFFIX = ")";
    
    // Secret key for encryption/decryption
    private static SecretKey secretKey;
    
    static {
        initializeKey();
    }
    
    /**
     * Initialize the encryption key
     * Uses a fixed internal master key for the framework
     */
    private static void initializeKey() {
        try {
            // Use fixed internal master key - same for all CS Framework installations
            // This provides convenience over security - suitable for test automation
            String masterKey = "CSTestForge2024MasterKey!@#$%^&*";
            byte[] keyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
            byte[] key256 = new byte[32]; // 256 bits
            System.arraycopy(keyBytes, 0, key256, 0, Math.min(keyBytes.length, 32));
            secretKey = new SecretKeySpec(key256, KEY_ALGORITHM);
            logger.debug("Encryption initialized with internal master key");
        } catch (Exception e) {
            logger.error("Failed to initialize encryption key", e);
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }
    
    /**
     * Generate a new random key for encryption
     * @return Base64 encoded key
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(KEY_LENGTH);
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            logger.error("Failed to generate encryption key", e);
            throw new RuntimeException("Failed to generate key", e);
        }
    }
    
    /**
     * Encrypt a plain text value
     * @param plainText The text to encrypt
     * @return Encrypted text wrapped in ENC() format
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            // Generate random IV
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            // Setup cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            
            // Encrypt
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and cipher text
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            
            // Encode to Base64 and wrap in ENC()
            String encrypted = Base64.getEncoder().encodeToString(byteBuffer.array());
            return ENCRYPTED_PREFIX + encrypted + ENCRYPTED_SUFFIX;
            
        } catch (Exception e) {
            logger.error("Failed to encrypt value", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt an encrypted value
     * @param encryptedText The encrypted text (may or may not be wrapped in ENC())
     * @return Decrypted plain text
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        // Extract encrypted value from ENC() wrapper if present
        String encrypted = extractEncryptedValue(encryptedText);
        if (encrypted == null) {
            // Not an encrypted value, return as is
            return encryptedText;
        }
        
        try {
            // Decode from Base64
            byte[] cipherMessage = Base64.getDecoder().decode(encrypted);
            
            // Extract IV and cipher text
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);
            
            // Setup cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            // Decrypt
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Failed to decrypt value: {}", encryptedText, e);
            // Return original value if decryption fails
            return encryptedText;
        }
    }
    
    /**
     * Process a string that may contain multiple encrypted values
     * @param text Text that may contain ENC() wrapped values
     * @return Text with all encrypted values decrypted
     */
    public static String processEncryptedValues(String text) {
        if (text == null || !text.contains(ENCRYPTED_PREFIX)) {
            return text;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = ENCRYPTED_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String encryptedValue = matcher.group(1);
            String decryptedValue = decryptDirectValue(encryptedValue);
            matcher.appendReplacement(result, Matcher.quoteReplacement(decryptedValue));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Check if a value is encrypted (wrapped in ENC())
     * @param value The value to check
     * @return true if the value is encrypted
     */
    public static boolean isEncrypted(String value) {
        return value != null && ENCRYPTED_PATTERN.matcher(value).find();
    }
    
    /**
     * Extract encrypted value from ENC() wrapper
     * @param text Text that may contain ENC() wrapper
     * @return The encrypted value without wrapper, or null if not encrypted
     */
    private static String extractEncryptedValue(String text) {
        if (text == null) {
            return null;
        }
        
        Matcher matcher = ENCRYPTED_PATTERN.matcher(text);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Decrypt a value directly (without ENC wrapper)
     * @param encryptedValue The encrypted value
     * @return Decrypted value
     */
    private static String decryptDirectValue(String encryptedValue) {
        try {
            // Decode from Base64
            byte[] cipherMessage = Base64.getDecoder().decode(encryptedValue);
            
            // Extract IV and cipher text
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);
            
            // Setup cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            // Decrypt
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Failed to decrypt direct value", e);
            return encryptedValue;
        }
    }
    
    /**
     * Set a custom encryption key
     * @param key Base64 encoded encryption key
     */
    public static void setEncryptionKey(String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
            logger.info("Custom encryption key set");
        } catch (Exception e) {
            logger.error("Failed to set encryption key", e);
            throw new RuntimeException("Invalid encryption key", e);
        }
    }
    
    /**
     * Utility method to encrypt values for configuration
     * This can be used to generate encrypted values to store in properties files
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("CS Encryption Utility");
            System.out.println("Usage:");
            System.out.println("  Encrypt: java CSEncryptionUtils encrypt <value>");
            System.out.println("  Decrypt: java CSEncryptionUtils decrypt <encrypted_value>");
            System.out.println("  Generate Key: java CSEncryptionUtils genkey");
            return;
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "encrypt":
                if (args.length < 2) {
                    System.out.println("Please provide a value to encrypt");
                    return;
                }
                String encrypted = encrypt(args[1]);
                System.out.println("Encrypted value: " + encrypted);
                break;
                
            case "decrypt":
                if (args.length < 2) {
                    System.out.println("Please provide a value to decrypt");
                    return;
                }
                String decrypted = decrypt(args[1]);
                System.out.println("Decrypted value: " + decrypted);
                break;
                
            case "genkey":
                String key = generateKey();
                System.out.println("Generated key: " + key);
                System.out.println("Set this as environment variable: CS_ENCRYPTION_KEY=" + key);
                break;
                
            default:
                System.out.println("Unknown command: " + command);
        }
    }
}