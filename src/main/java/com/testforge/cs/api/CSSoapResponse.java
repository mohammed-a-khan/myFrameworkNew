package com.testforge.cs.api;

import com.testforge.cs.exceptions.CSApiException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.namespace.NamespaceContext;
import jakarta.xml.soap.SOAPFault;
import javax.xml.xpath.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a SOAP response
 */
public class CSSoapResponse {
    private final String responseXml;
    private final Document responseDocument;
    private final SOAPFault fault;
    private final long responseTime;
    private final String endpointUrl;
    private final String soapAction;
    private final XPath xpath;
    
    public CSSoapResponse(String responseXml, Document responseDocument, SOAPFault fault,
                         long responseTime, String endpointUrl, String soapAction) {
        this.responseXml = responseXml;
        this.responseDocument = responseDocument;
        this.fault = fault;
        this.responseTime = responseTime;
        this.endpointUrl = endpointUrl;
        this.soapAction = soapAction;
        
        // Initialize XPath
        XPathFactory xPathFactory = XPathFactory.newInstance();
        this.xpath = xPathFactory.newXPath();
        
        // Configure namespace resolver
        xpath.setNamespaceContext(new NamespaceResolver());
    }
    
    /**
     * Simplified constructor for testing
     */
    public CSSoapResponse(String responseXml) {
        this.responseXml = responseXml;
        this.responseDocument = parseXml(responseXml);
        this.fault = null;
        this.responseTime = 0;
        this.endpointUrl = "";
        this.soapAction = "";
        
        // Initialize XPath
        XPathFactory xPathFactory = XPathFactory.newInstance();
        this.xpath = xPathFactory.newXPath();
        
        // Configure namespace resolver
        xpath.setNamespaceContext(new NamespaceResolver());
    }
    
    private Document parseXml(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new CSApiException("Failed to parse XML", e);
        }
    }
    
    /**
     * Get response XML as string
     */
    public String getResponseXml() {
        return responseXml;
    }
    
    /**
     * Get response body (alias for getResponseXml)
     */
    public String getResponseBody() {
        return responseXml;
    }
    
    /**
     * Get response as Document
     */
    public Document getResponseDocument() {
        return responseDocument;
    }
    
    /**
     * Check if response has fault
     */
    public boolean hasFault() {
        return fault != null;
    }
    
    /**
     * Get SOAP fault
     */
    public SOAPFault getFault() {
        return fault;
    }
    
    /**
     * Get fault code
     */
    public String getFaultCode() {
        if (fault != null) {
            return fault.getFaultCode();
        }
        return null;
    }
    
    /**
     * Get fault string
     */
    public String getFaultString() {
        if (fault != null) {
            return fault.getFaultString();
        }
        return null;
    }
    
    /**
     * Get fault actor
     */
    public String getFaultActor() {
        if (fault != null) {
            return fault.getFaultActor();
        }
        return null;
    }
    
    /**
     * Get response time in milliseconds
     */
    public long getResponseTime() {
        return responseTime;
    }
    
    /**
     * Get endpoint URL
     */
    public String getEndpointUrl() {
        return endpointUrl;
    }
    
    /**
     * Get SOAP action
     */
    public String getSoapAction() {
        return soapAction;
    }
    
    /**
     * Extract value using XPath
     */
    public String getXPathValue(String xpathExpression) {
        try {
            return (String) xpath.evaluate(xpathExpression, responseDocument, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new CSApiException("Failed to evaluate XPath: " + xpathExpression, e);
        }
    }
    
    /**
     * Get node value (alias for getXPathValue)
     */
    public String getNodeValue(String xpathExpression) {
        return getXPathValue(xpathExpression);
    }
    
    /**
     * Extract node using XPath
     */
    public Node getXPathNode(String xpathExpression) {
        try {
            return (Node) xpath.evaluate(xpathExpression, responseDocument, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new CSApiException("Failed to evaluate XPath: " + xpathExpression, e);
        }
    }
    
    /**
     * Extract nodes using XPath
     */
    public NodeList getXPathNodes(String xpathExpression) {
        try {
            return (NodeList) xpath.evaluate(xpathExpression, responseDocument, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new CSApiException("Failed to evaluate XPath: " + xpathExpression, e);
        }
    }
    
    /**
     * Extract boolean using XPath
     */
    public boolean getXPathBoolean(String xpathExpression) {
        try {
            return (Boolean) xpath.evaluate(xpathExpression, responseDocument, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException e) {
            throw new CSApiException("Failed to evaluate XPath: " + xpathExpression, e);
        }
    }
    
    /**
     * Extract number using XPath
     */
    public double getXPathNumber(String xpathExpression) {
        try {
            return (Double) xpath.evaluate(xpathExpression, responseDocument, XPathConstants.NUMBER);
        } catch (XPathExpressionException e) {
            throw new CSApiException("Failed to evaluate XPath: " + xpathExpression, e);
        }
    }
    
    /**
     * Add namespace to XPath context
     */
    public void addNamespace(String prefix, String uri) {
        ((NamespaceResolver) xpath.getNamespaceContext()).addNamespace(prefix, uri);
    }
    
    /**
     * Assert response has no fault
     */
    public CSSoapResponse assertNoFault() {
        if (hasFault()) {
            throw new AssertionError(String.format(
                "Expected no SOAP fault but got: %s - %s",
                getFaultCode(), getFaultString()
            ));
        }
        return this;
    }
    
    /**
     * Assert XPath value equals expected
     */
    public CSSoapResponse assertXPathValue(String xpathExpression, String expectedValue) {
        String actualValue = getXPathValue(xpathExpression);
        if (!expectedValue.equals(actualValue)) {
            throw new AssertionError(String.format(
                "Expected XPath '%s' to have value '%s' but got '%s'",
                xpathExpression, expectedValue, actualValue
            ));
        }
        return this;
    }
    
    /**
     * Assert XPath node exists
     */
    public CSSoapResponse assertXPathExists(String xpathExpression) {
        Node node = getXPathNode(xpathExpression);
        if (node == null) {
            throw new AssertionError(String.format(
                "Expected XPath '%s' to exist but it was not found",
                xpathExpression
            ));
        }
        return this;
    }
    
    /**
     * Assert XPath node count
     */
    public CSSoapResponse assertXPathCount(String xpathExpression, int expectedCount) {
        NodeList nodes = getXPathNodes(xpathExpression);
        int actualCount = nodes != null ? nodes.getLength() : 0;
        if (actualCount != expectedCount) {
            throw new AssertionError(String.format(
                "Expected XPath '%s' to have %d nodes but found %d",
                xpathExpression, expectedCount, actualCount
            ));
        }
        return this;
    }
    
    /**
     * Assert response contains text
     */
    public CSSoapResponse assertResponseContains(String text) {
        if (!responseXml.contains(text)) {
            throw new AssertionError(String.format(
                "Expected response to contain '%s' but it didn't",
                text
            ));
        }
        return this;
    }
    
    /**
     * Assert response time is less than threshold
     */
    public CSSoapResponse assertResponseTime(long maxMillis) {
        if (responseTime > maxMillis) {
            throw new AssertionError(String.format(
                "Expected response time to be less than %d ms but was %d ms",
                maxMillis, responseTime
            ));
        }
        return this;
    }
    
    /**
     * Print formatted response
     */
    public void printResponse() {
        System.out.println("=== SOAP Response ===");
        System.out.println("Endpoint: " + endpointUrl);
        System.out.println("SOAP Action: " + soapAction);
        System.out.println("Response Time: " + responseTime + " ms");
        System.out.println("Has Fault: " + hasFault());
        if (hasFault()) {
            System.out.println("Fault Code: " + getFaultCode());
            System.out.println("Fault String: " + getFaultString());
        }
        System.out.println("Response XML:");
        System.out.println(responseXml);
        System.out.println("===================");
    }
    
    @Override
    public String toString() {
        if (hasFault()) {
            return String.format("SOAP Fault: %s - %s (%d ms)", 
                getFaultCode(), getFaultString(), responseTime);
        } else {
            return String.format("SOAP Response from %s (%d ms)", 
                endpointUrl, responseTime);
        }
    }
    
    /**
     * Custom namespace resolver for XPath
     */
    private static class NamespaceResolver implements NamespaceContext {
        private final Map<String, String> namespaceMap;
        
        public NamespaceResolver() {
            this.namespaceMap = new HashMap<>();
            // Add default SOAP namespaces
            namespaceMap.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
            namespaceMap.put("soap12", "http://www.w3.org/2003/05/soap-envelope");
            namespaceMap.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            namespaceMap.put("xsd", "http://www.w3.org/2001/XMLSchema");
        }
        
        public void addNamespace(String prefix, String uri) {
            namespaceMap.put(prefix, uri);
        }
        
        @Override
        public String getNamespaceURI(String prefix) {
            return namespaceMap.get(prefix);
        }
        
        @Override
        public String getPrefix(String namespaceURI) {
            for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                if (entry.getValue().equals(namespaceURI)) {
                    return entry.getKey();
                }
            }
            return null;
        }
        
        @Override
        public java.util.Iterator<String> getPrefixes(String namespaceURI) {
            java.util.List<String> prefixes = new java.util.ArrayList<>();
            for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                if (entry.getValue().equals(namespaceURI)) {
                    prefixes.add(entry.getKey());
                }
            }
            return prefixes.iterator();
        }
    }
}