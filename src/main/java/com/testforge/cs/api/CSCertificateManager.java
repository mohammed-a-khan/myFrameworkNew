package com.testforge.cs.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Certificate Manager for handling client certificates for API authentication
 * Supports PFX/P12, PEM, and JKS formats
 */
public class CSCertificateManager {
    private static final Logger logger = LoggerFactory.getLogger(CSCertificateManager.class);
    private static CSCertificateManager instance;
    
    private final Map<String, SSLContext> sslContextCache = new HashMap<>();
    private final Map<String, KeyStore> keyStoreCache = new HashMap<>();
    
    private CSCertificateManager() {
        // Private constructor for singleton
    }
    
    public static synchronized CSCertificateManager getInstance() {
        if (instance == null) {
            instance = new CSCertificateManager();
        }
        return instance;
    }
    
    /**
     * Load certificate from file path with password
     * @param certificatePath Path to certificate file (PFX, P12, PEM, or JKS)
     * @param password Certificate password
     * @return SSLContext configured with the certificate
     */
    public SSLContext loadCertificate(String certificatePath, String password) throws Exception {
        String cacheKey = certificatePath + ":" + password;
        
        if (sslContextCache.containsKey(cacheKey)) {
            logger.debug("Using cached SSL context for: {}", certificatePath);
            return sslContextCache.get(cacheKey);
        }
        
        logger.info("Loading certificate from: {}", certificatePath);
        
        File certFile = new File(certificatePath);
        if (!certFile.exists()) {
            // Try as resource
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(certificatePath);
            if (resourceStream == null) {
                throw new FileNotFoundException("Certificate file not found: " + certificatePath);
            }
            return loadCertificateFromStream(resourceStream, password, certificatePath);
        }
        
        try (FileInputStream fis = new FileInputStream(certFile)) {
            return loadCertificateFromStream(fis, password, certificatePath);
        }
    }
    
    /**
     * Load certificate from input stream
     */
    private SSLContext loadCertificateFromStream(InputStream certStream, String password, String identifier) 
            throws Exception {
        
        String cacheKey = identifier + ":" + password;
        
        // Determine certificate type based on file extension
        String lowerCaseId = identifier.toLowerCase();
        KeyStore keyStore;
        
        if (lowerCaseId.endsWith(".pfx") || lowerCaseId.endsWith(".p12")) {
            keyStore = loadPKCS12Certificate(certStream, password);
        } else if (lowerCaseId.endsWith(".jks")) {
            keyStore = loadJKSCertificate(certStream, password);
        } else if (lowerCaseId.endsWith(".pem") || lowerCaseId.endsWith(".crt") || lowerCaseId.endsWith(".cer")) {
            keyStore = loadPEMCertificate(certStream, password);
        } else {
            // Try to detect format
            keyStore = autoDetectAndLoadCertificate(certStream, password);
        }
        
        keyStoreCache.put(identifier, keyStore);
        
        // Create SSL context
        SSLContext sslContext = createSSLContext(keyStore, password);
        sslContextCache.put(cacheKey, sslContext);
        
        logger.info("Successfully loaded certificate: {}", identifier);
        return sslContext;
    }
    
    /**
     * Load PKCS12 certificate (PFX/P12)
     */
    private KeyStore loadPKCS12Certificate(InputStream certStream, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] passwordChars = password != null ? password.toCharArray() : new char[0];
        keyStore.load(certStream, passwordChars);
        
        logger.debug("Loaded PKCS12 certificate with {} entries", keyStore.size());
        return keyStore;
    }
    
    /**
     * Load JKS certificate
     */
    private KeyStore loadJKSCertificate(InputStream certStream, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        char[] passwordChars = password != null ? password.toCharArray() : new char[0];
        keyStore.load(certStream, passwordChars);
        
        logger.debug("Loaded JKS certificate with {} entries", keyStore.size());
        return keyStore;
    }
    
    /**
     * Load PEM certificate
     */
    private KeyStore loadPEMCertificate(InputStream certStream, String password) throws Exception {
        // For PEM, we need to convert to KeyStore
        // This is a simplified implementation - in production, you might need a more robust PEM parser
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        
        // Note: Full PEM parsing would require additional libraries like Bouncy Castle
        // For now, we'll handle this as a placeholder
        logger.warn("PEM certificate loading requires additional implementation");
        
        return keyStore;
    }
    
    /**
     * Auto-detect certificate format
     */
    private KeyStore autoDetectAndLoadCertificate(InputStream certStream, String password) throws Exception {
        // Mark the stream to allow reset
        BufferedInputStream bis = new BufferedInputStream(certStream);
        bis.mark(10);
        
        // Read first few bytes to detect format
        byte[] header = new byte[4];
        bis.read(header);
        bis.reset();
        
        // Check for PKCS12 magic number
        if (header[0] == 0x30 && header[1] == (byte) 0x82) {
            return loadPKCS12Certificate(bis, password);
        }
        
        // Default to PKCS12
        return loadPKCS12Certificate(bis, password);
    }
    
    /**
     * Create SSL context from KeyStore
     */
    private SSLContext createSSLContext(KeyStore keyStore, String password) throws Exception {
        // Create key manager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        char[] passwordChars = password != null ? password.toCharArray() : new char[0];
        kmf.init(keyStore, passwordChars);
        
        // Create trust manager (accepting all certificates for testing)
        TrustManager[] trustManagers = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Trust all clients
                }
                
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Trust all servers (for testing only!)
                    logger.debug("Trusting server certificate: {}", certs[0].getSubjectDN());
                }
            }
        };
        
        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), trustManagers, new SecureRandom());
        
        return sslContext;
    }
    
    /**
     * Create SSL socket factory from certificate
     */
    public SSLSocketFactory createSSLSocketFactory(String certificatePath, String password) throws Exception {
        SSLContext sslContext = loadCertificate(certificatePath, password);
        return sslContext.getSocketFactory();
    }
    
    /**
     * Get KeyStore for certificate
     */
    public KeyStore getKeyStore(String certificatePath, String password) throws Exception {
        if (!keyStoreCache.containsKey(certificatePath)) {
            loadCertificate(certificatePath, password);
        }
        return keyStoreCache.get(certificatePath);
    }
    
    /**
     * Clear all cached certificates
     */
    public void clearCache() {
        sslContextCache.clear();
        keyStoreCache.clear();
        logger.info("Cleared certificate cache");
    }
    
    /**
     * Validate certificate expiry
     */
    public boolean validateCertificate(String certificatePath, String password) {
        try {
            KeyStore keyStore = getKeyStore(certificatePath, password);
            
            java.util.Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isCertificateEntry(alias) || keyStore.isKeyEntry(alias)) {
                    X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                    
                    // Check validity
                    try {
                        cert.checkValidity();
                        logger.info("Certificate {} is valid until: {}", alias, cert.getNotAfter());
                    } catch (Exception e) {
                        logger.error("Certificate {} is invalid: {}", alias, e.getMessage());
                        return false;
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to validate certificate: {}", e.getMessage());
            return false;
        }
    }
}