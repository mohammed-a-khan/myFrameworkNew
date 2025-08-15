package com.testforge.cs.config;

import org.testng.annotations.Test;
import org.testng.Assert;
import java.util.Properties;

/**
 * Test class for CSPropertyMapper
 * Verifies property mapping functionality for the two exception properties
 */
public class CSPropertyMapperTest {
    
    @Test
    public void testExceptionPropertyMappings() {
        System.out.println("Testing exception property mappings...");
        
        // Test the two allowed exception properties
        Assert.assertEquals(CSPropertyMapper.getStandardizedProperty("environment.name"), "cs.environment.name");
        Assert.assertEquals(CSPropertyMapper.getStandardizedProperty("browser.name"), "cs.browser.name");
        
        // Test that cs.* prefixed properties are returned as-is
        Assert.assertEquals(CSPropertyMapper.getStandardizedProperty("cs.browser.headless"), "cs.browser.headless");
        Assert.assertEquals(CSPropertyMapper.getStandardizedProperty("cs.azure.devops.enabled"), "cs.azure.devops.enabled");
        
        // Test that other non-cs properties are returned as-is (but should generate warning)
        Assert.assertEquals(CSPropertyMapper.getStandardizedProperty("some.other.property"), "some.other.property");
        
        System.out.println("Exception property mappings verified successfully!");
    }
    
    @Test
    public void testIsAllowedException() {
        // Test allowed exceptions
        Assert.assertTrue(CSPropertyMapper.isAllowedException("environment.name"));
        Assert.assertTrue(CSPropertyMapper.isAllowedException("browser.name"));
        
        // Test non-allowed properties
        Assert.assertFalse(CSPropertyMapper.isAllowedException("cs.browser.name"));
        Assert.assertFalse(CSPropertyMapper.isAllowedException("app.base.url"));
        Assert.assertFalse(CSPropertyMapper.isAllowedException("ado.enabled"));
        
        System.out.println("Exception checks passed!");
    }
    
    @Test
    public void testPropertiesApplyMappings() {
        Properties original = new Properties();
        original.setProperty("environment.name", "production");
        original.setProperty("browser.name", "chrome");
        original.setProperty("cs.app.base.url", "https://example.com");
        original.setProperty("cs.azure.devops.enabled", "true");
        
        Properties mapped = CSPropertyMapper.applyMappings(original);
        
        // Check that exception properties are mapped
        Assert.assertEquals(mapped.getProperty("cs.environment.name"), "production");
        Assert.assertEquals(mapped.getProperty("cs.browser.name"), "chrome");
        
        // Check that original exception properties still exist
        Assert.assertEquals(mapped.getProperty("environment.name"), "production");
        Assert.assertEquals(mapped.getProperty("browser.name"), "chrome");
        
        // Check that already standardized properties are unchanged
        Assert.assertEquals(mapped.getProperty("cs.app.base.url"), "https://example.com");
        Assert.assertEquals(mapped.getProperty("cs.azure.devops.enabled"), "true");
        
        System.out.println("Properties mapping application test passed!");
    }
    
    @Test
    public void testInvalidPropertyWarning() {
        // This test just verifies the method exists and can be called
        // Actual warning logging is tested manually
        CSPropertyMapper.logInvalidPropertyWarning("invalid.property");
        CSPropertyMapper.logInvalidPropertyWarning("environment.name"); // Should not warn
        CSPropertyMapper.logInvalidPropertyWarning("browser.name"); // Should not warn
        CSPropertyMapper.logInvalidPropertyWarning("cs.valid.property"); // Should not warn
        
        System.out.println("Invalid property warning test passed!");
    }
    
    public static void main(String[] args) {
        CSPropertyMapperTest test = new CSPropertyMapperTest();
        test.testExceptionPropertyMappings();
        test.testIsAllowedException();
        test.testPropertiesApplyMappings();
        test.testInvalidPropertyWarning();
        System.out.println("\nAll tests passed successfully!");
    }
}