package com.testforge.cs.tests.api;

import com.testforge.cs.api.CSCertificateManager;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class demonstrating certificate-based authentication
 * Tests both API calls and browser-based certificate authentication
 */
public class CSCertificateAuthenticationTest {
    private static final Logger logger = LoggerFactory.getLogger(CSCertificateAuthenticationTest.class);
    
    private CSCertificateManager certificateManager;
    private WebDriver driver;
    
    // Certificate paths
    private static final String BADSSL_CLIENT_CERT = "certificates/badssl.com-client.p12";
    private static final String BADSSL_CERT_PASSWORD = "badssl.com";
    
    // Test URLs
    private static final String BADSSL_CLIENT_CERT_URL = "https://client.badssl.com/";
    private static final String BADSSL_CLIENT_CERT_REQUIRED_URL = "https://client-cert.badssl.com/";
    
    @BeforeClass
    public void setup() {
        certificateManager = CSCertificateManager.getInstance();
        logger.info("Certificate Authentication Test Setup Complete");
    }
    
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    /**
     * Test 1: Basic certificate loading and validation
     */
    @Test(priority = 1)
    public void testCertificateLoading() {
        logger.info("Testing certificate loading and validation");
        
        try {
            // Load certificate
            SSLContext sslContext = certificateManager.loadCertificate(BADSSL_CLIENT_CERT, BADSSL_CERT_PASSWORD);
            Assert.assertNotNull(sslContext, "SSL Context should not be null");
            
            // Validate certificate
            boolean isValid = certificateManager.validateCertificate(BADSSL_CLIENT_CERT, BADSSL_CERT_PASSWORD);
            Assert.assertTrue(isValid, "Certificate should be valid");
            
            logger.info("Certificate loaded and validated successfully");
        } catch (Exception e) {
            logger.error("Failed to load certificate", e);
            Assert.fail("Certificate loading failed: " + e.getMessage());
        }
    }
    
    /**
     * Test 2: API call with client certificate authentication using Apache HttpClient
     */
    @Test(priority = 2)
    public void testAPICallWithClientCertificate() {
        logger.info("Testing API call with client certificate authentication");
        
        try {
            // Load certificate and create SSL context
            SSLContext sslContext = certificateManager.loadCertificate(BADSSL_CLIENT_CERT, BADSSL_CERT_PASSWORD);
            
            // Create SSL socket factory with the client certificate
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2", "TLSv1.3"},
                null,
                NoopHostnameVerifier.INSTANCE
            );
            
            // Create HTTP client with the certificate
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build()) {
                
                // Make request to server requiring client certificate
                HttpGet request = new HttpGet(BADSSL_CLIENT_CERT_URL);
                HttpResponse response = httpClient.execute(request);
                
                // Verify response
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                logger.info("Response Status Code: {}", statusCode);
                logger.info("Response Body Length: {}", responseBody.length());
                
                // Assert successful response
                Assert.assertEquals(statusCode, 200, "Expected 200 OK response");
                Assert.assertTrue(responseBody.contains("client"), 
                    "Response should indicate successful client certificate authentication");
                
                logger.info("API call with client certificate succeeded");
            }
            
        } catch (Exception e) {
            logger.error("API call with certificate failed", e);
            Assert.fail("API call failed: " + e.getMessage());
        }
    }
    
    /**
     * Test 3: Simple HTTPS connection with client certificate
     */
    @Test(priority = 3)
    public void testHTTPSConnectionWithCertificate() {
        logger.info("Testing HTTPS connection with client certificate");
        
        try {
            // Load certificate
            SSLContext sslContext = certificateManager.loadCertificate(BADSSL_CLIENT_CERT, BADSSL_CERT_PASSWORD);
            
            // Create connection
            URL url = new URL(BADSSL_CLIENT_CERT_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            if (connection instanceof javax.net.ssl.HttpsURLConnection) {
                javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) connection;
                httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                
                // Disable hostname verification for testing
                httpsConnection.setHostnameVerifier((hostname, session) -> true);
            }
            
            // Connect and get response
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            logger.info("HTTPS Response Code: {}", responseCode);
            
            Assert.assertEquals(responseCode, 200, "Expected 200 OK response");
            
            connection.disconnect();
            logger.info("HTTPS connection with certificate succeeded");
            
        } catch (Exception e) {
            logger.error("HTTPS connection failed", e);
            Assert.fail("HTTPS connection failed: " + e.getMessage());
        }
    }
    
    /**
     * Test 4: Browser-based certificate authentication with Selenium
     * Note: This requires proper browser configuration to handle client certificates
     */
    @Test(priority = 4, enabled = false) // Disabled by default as it requires special browser setup
    public void testBrowserCertificateAuthentication() {
        logger.info("Testing browser-based certificate authentication");
        
        try {
            // Setup Chrome options for certificate handling
            ChromeOptions options = new ChromeOptions();
            
            // Add certificate handling capabilities
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.default_content_settings.popups", 0);
            prefs.put("download.prompt_for_download", false);
            options.setExperimentalOption("prefs", prefs);
            
            // Accept all SSL certificates
            options.setAcceptInsecureCerts(true);
            
            // For client certificates, you would typically need to:
            // 1. Import the certificate into the browser's certificate store
            // 2. Or use a browser profile with the certificate pre-installed
            // 3. Or use AutoSelectCertificateForUrls policy for Chrome
            
            // Example of AutoSelectCertificateForUrls (requires admin setup)
            String certAutoSelectPattern = "{\"pattern\":\"https://client.badssl.com\",\"filter\":{\"ISSUER\":{\"CN\":\"*.badssl.com\"}}}";
            
            driver = new ChromeDriver(options);
            driver.get(BADSSL_CLIENT_CERT_URL);
            
            // Verify page loaded successfully
            String pageTitle = driver.getTitle();
            logger.info("Page title: {}", pageTitle);
            
            Assert.assertNotNull(pageTitle, "Page should load with certificate");
            
        } catch (Exception e) {
            logger.error("Browser certificate authentication failed", e);
            logger.info("Note: Browser certificate authentication requires special setup");
        }
    }
    
    /**
     * Test 5: Multiple certificate formats
     */
    @Test(priority = 5)
    public void testMultipleCertificateFormats() {
        logger.info("Testing multiple certificate formats");
        
        String[] certFiles = {
            "certificates/badssl.com-client.p12",
            "certificates/badssl.com-client.pfx",
            "certificates/client.pfx"
        };
        
        for (String certFile : certFiles) {
            try {
                logger.info("Testing certificate: {}", certFile);
                
                File file = new File(getClass().getClassLoader().getResource(certFile).getFile());
                if (!file.exists()) {
                    logger.warn("Certificate file not found: {}", certFile);
                    continue;
                }
                
                SSLContext sslContext = certificateManager.loadCertificate(certFile, BADSSL_CERT_PASSWORD);
                Assert.assertNotNull(sslContext, "SSL Context should not be null for: " + certFile);
                
                logger.info("Successfully loaded certificate: {}", certFile);
                
            } catch (Exception e) {
                logger.warn("Failed to load certificate {}: {}", certFile, e.getMessage());
            }
        }
    }
    
    /**
     * Test 6: Certificate caching
     */
    @Test(priority = 6)
    public void testCertificateCaching() {
        logger.info("Testing certificate caching");
        
        try {
            // Load certificate first time
            long startTime = System.currentTimeMillis();
            SSLContext context1 = certificateManager.loadCertificate(BADSSL_CLIENT_CERT, BADSSL_CERT_PASSWORD);
            long firstLoadTime = System.currentTimeMillis() - startTime;
            
            // Load certificate second time (should be cached)
            startTime = System.currentTimeMillis();
            SSLContext context2 = certificateManager.loadCertificate(BADSSL_CLIENT_CERT, BADSSL_CERT_PASSWORD);
            long secondLoadTime = System.currentTimeMillis() - startTime;
            
            Assert.assertSame(context1, context2, "Should return cached SSL context");
            logger.info("First load time: {}ms, Second load time: {}ms", firstLoadTime, secondLoadTime);
            
            // Clear cache
            certificateManager.clearCache();
            
            // Load again after clearing cache
            SSLContext context3 = certificateManager.loadCertificate(BADSSL_CLIENT_CERT, BADSSL_CERT_PASSWORD);
            Assert.assertNotSame(context1, context3, "Should return new SSL context after cache clear");
            
            logger.info("Certificate caching working correctly");
            
        } catch (Exception e) {
            logger.error("Certificate caching test failed", e);
            Assert.fail("Certificate caching test failed: " + e.getMessage());
        }
    }
    
    /**
     * Test 7: Error handling - Invalid password
     */
    @Test(priority = 7, expectedExceptions = Exception.class)
    public void testInvalidCertificatePassword() throws Exception {
        logger.info("Testing invalid certificate password handling");
        certificateManager.loadCertificate(BADSSL_CLIENT_CERT, "wrong_password");
    }
    
    /**
     * Test 8: Error handling - Non-existent certificate
     */
    @Test(priority = 8, expectedExceptions = Exception.class)
    public void testNonExistentCertificate() throws Exception {
        logger.info("Testing non-existent certificate handling");
        certificateManager.loadCertificate("certificates/non_existent.pfx", "password");
    }
}