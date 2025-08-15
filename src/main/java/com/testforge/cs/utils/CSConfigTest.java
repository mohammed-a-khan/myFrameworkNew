package com.testforge.cs.utils;

import com.testforge.cs.config.CSConfigManager;

public class CSConfigTest {
    public static void main(String[] args) {
        CSConfigManager config = CSConfigManager.getInstance();
        
        System.out.println("Current environment: " + config.getCurrentEnvironment());
        System.out.println("Akhan URL: " + config.getProperty("cs.akhan.url"));
        System.out.println("OrangeHRM URL: " + config.getProperty("cs.orangehrm.url"));
        System.out.println("Azure DevOps Test Plan ID: " + config.getProperty("cs.azure.devops.test.plan.id"));
        System.out.println("Azure DevOps Test Suite ID: " + config.getProperty("cs.azure.devops.test.suite.id"));
        System.out.println("DB Host: " + config.getProperty("cs.db.default.host"));
        System.out.println("Performance Threshold: " + config.getProperty("cs.performance.threshold"));
    }
}