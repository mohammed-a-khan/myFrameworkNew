package com.testforge.cs.api;

import com.testforge.cs.exceptions.CSApiException;
import com.testforge.cs.utils.CSFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import jakarta.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * SOAP client for testing SOAP web services
 * Uses Java's built-in SOAP API
 */
public class CSSoapClient {
    private static final Logger logger = LoggerFactory.getLogger(CSSoapClient.class);
    
    private final MessageFactory messageFactory;
    private final SOAPConnectionFactory connectionFactory;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final TransformerFactory transformerFactory;
    private final XPathFactory xPathFactory;
    
    private String endpointUrl;
    private String soapAction;
    private Map<String, String> headers;
    private Map<String, String> namespaces;
    
    /**
     * Create SOAP client
     */
    public CSSoapClient() {
        try {
            this.messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            this.connectionFactory = SOAPConnectionFactory.newInstance();
            this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
            this.documentBuilderFactory.setNamespaceAware(true);
            this.transformerFactory = TransformerFactory.newInstance();
            this.xPathFactory = XPathFactory.newInstance();
            this.headers = new HashMap<>();
            this.namespaces = new HashMap<>();
        } catch (Exception e) {
            throw new CSApiException("Failed to initialize SOAP client", e);
        }
    }
    
    /**
     * Set endpoint URL
     */
    public CSSoapClient setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
        return this;
    }
    
    /**
     * Set SOAP action
     */
    public CSSoapClient setSoapAction(String soapAction) {
        this.soapAction = soapAction;
        return this;
    }
    
    /**
     * Add header
     */
    public CSSoapClient addHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }
    
    /**
     * Add namespace
     */
    public CSSoapClient addNamespace(String prefix, String uri) {
        this.namespaces.put(prefix, uri);
        return this;
    }
    
    /**
     * Send SOAP request with XML string
     */
    public CSSoapResponse send(String soapRequestXml) {
        try {
            // Parse XML to document
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(
                new ByteArrayInputStream(soapRequestXml.getBytes(StandardCharsets.UTF_8))
            );
            
            return send(document);
        } catch (Exception e) {
            throw new CSApiException("Failed to parse SOAP request XML", e);
        }
    }
    
    /**
     * Send SOAP request from file
     */
    public CSSoapResponse sendFromFile(String filePath) {
        String soapRequestXml = CSFileUtils.readFileAsString(filePath);
        return send(soapRequestXml);
    }
    
    /**
     * Send SOAP request with Document
     */
    public CSSoapResponse send(Document requestDocument) {
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            throw new CSApiException("Endpoint URL not set");
        }
        
        try {
            logger.debug("Sending SOAP request to: {}", endpointUrl);
            
            // Create SOAP message
            SOAPMessage soapRequest = createSoapMessage(requestDocument);
            
            // Log request
            if (logger.isDebugEnabled()) {
                logger.debug("SOAP Request:\n{}", soapMessageToString(soapRequest));
            }
            
            // Send request
            long startTime = System.currentTimeMillis();
            SOAPConnection connection = connectionFactory.createConnection();
            SOAPMessage soapResponse = connection.call(soapRequest, new URL(endpointUrl));
            connection.close();
            long endTime = System.currentTimeMillis();
            
            // Process response
            String responseXml = soapMessageToString(soapResponse);
            Document responseDocument = soapMessageToDocument(soapResponse);
            
            logger.debug("SOAP Response received in {} ms", endTime - startTime);
            if (logger.isDebugEnabled()) {
                logger.debug("SOAP Response:\n{}", responseXml);
            }
            
            // Check for SOAP fault
            SOAPFault fault = soapResponse.getSOAPBody().getFault();
            
            return new CSSoapResponse(
                responseXml,
                responseDocument,
                fault,
                endTime - startTime,
                endpointUrl,
                soapAction
            );
            
        } catch (Exception e) {
            throw new CSApiException("Failed to send SOAP request", e);
        }
    }
    
    /**
     * Send SOAP request (convenience method)
     */
    public CSSoapResponse sendRequest(String endpointUrl, String soapAction, String soapRequest) {
        this.endpointUrl = endpointUrl;
        this.soapAction = soapAction;
        
        try {
            // Parse the SOAP request string to document
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(soapRequest.getBytes(StandardCharsets.UTF_8)));
            
            // Send the request
            return send(doc);
        } catch (Exception e) {
            throw new CSApiException("Failed to send SOAP request", e);
        }
    }
    
    /**
     * Build SOAP request using builder pattern
     */
    public CSSoapRequestBuilder buildRequest() {
        return new CSSoapRequestBuilder(this);
    }
    
    /**
     * Create SOAP message from document
     */
    private SOAPMessage createSoapMessage(Document document) throws Exception {
        SOAPMessage soapMessage = messageFactory.createMessage();
        
        // Set headers
        MimeHeaders mimeHeaders = soapMessage.getMimeHeaders();
        if (soapAction != null) {
            mimeHeaders.addHeader("SOAPAction", soapAction);
        }
        headers.forEach(mimeHeaders::addHeader);
        
        // Get SOAP envelope
        SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
        
        // Add namespaces
        namespaces.forEach((prefix, uri) -> {
            try {
                envelope.addNamespaceDeclaration(prefix, uri);
            } catch (SOAPException e) {
                logger.warn("Failed to add namespace: {} = {}", prefix, uri);
            }
        });
        
        // Import document into SOAP body
        SOAPBody soapBody = envelope.getBody();
        Node importedNode = soapBody.getOwnerDocument().importNode(
            document.getDocumentElement(), true
        );
        soapBody.appendChild(importedNode);
        
        // Save changes
        soapMessage.saveChanges();
        
        return soapMessage;
    }
    
    /**
     * Convert SOAP message to string
     */
    private String soapMessageToString(SOAPMessage message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }
    
    /**
     * Convert SOAP message to document
     */
    private Document soapMessageToDocument(SOAPMessage message) throws Exception {
        Source source = message.getSOAPPart().getContent();
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        
        // Transform to string first
        StringWriter writer = new StringWriter();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, new StreamResult(writer));
        
        // Parse back to document
        return builder.parse(
            new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8))
        );
    }
    
    /**
     * SOAP request builder
     */
    public static class CSSoapRequestBuilder {
        private final CSSoapClient client;
        private final Document document;
        private Node currentNode;
        
        private CSSoapRequestBuilder(CSSoapClient client) {
            this.client = client;
            try {
                DocumentBuilder builder = client.documentBuilderFactory.newDocumentBuilder();
                this.document = builder.newDocument();
            } catch (Exception e) {
                throw new CSApiException("Failed to create document builder", e);
            }
        }
        
        /**
         * Create root element
         */
        public CSSoapRequestBuilder createRoot(String name, String namespace) {
            Node root = document.createElementNS(namespace, name);
            document.appendChild(root);
            currentNode = root;
            return this;
        }
        
        /**
         * Add element
         */
        public CSSoapRequestBuilder addElement(String name, String value) {
            return addElement(name, null, value);
        }
        
        /**
         * Add element with namespace
         */
        public CSSoapRequestBuilder addElement(String name, String namespace, String value) {
            Node element = namespace != null 
                ? document.createElementNS(namespace, name)
                : document.createElement(name);
                
            if (value != null) {
                element.setTextContent(value);
            }
            
            if (currentNode != null) {
                currentNode.appendChild(element);
            } else {
                document.appendChild(element);
                currentNode = element;
            }
            
            return this;
        }
        
        /**
         * Start complex element
         */
        public CSSoapRequestBuilder startElement(String name) {
            return startElement(name, null);
        }
        
        /**
         * Start complex element with namespace
         */
        public CSSoapRequestBuilder startElement(String name, String namespace) {
            Node element = namespace != null 
                ? document.createElementNS(namespace, name)
                : document.createElement(name);
                
            if (currentNode != null) {
                currentNode.appendChild(element);
            } else {
                document.appendChild(element);
            }
            
            currentNode = element;
            return this;
        }
        
        /**
         * End current element
         */
        public CSSoapRequestBuilder endElement() {
            if (currentNode != null && currentNode.getParentNode() != null) {
                currentNode = currentNode.getParentNode();
            }
            return this;
        }
        
        /**
         * Add attribute
         */
        public CSSoapRequestBuilder addAttribute(String name, String value) {
            if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
                ((org.w3c.dom.Element) currentNode).setAttribute(name, value);
            }
            return this;
        }
        
        /**
         * Build and send request
         */
        public CSSoapResponse send() {
            return client.send(document);
        }
        
        /**
         * Get built document
         */
        public Document getDocument() {
            return document;
        }
        
        /**
         * Get XML string
         */
        public String toXmlString() {
            try {
                Transformer transformer = client.transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(document), new StreamResult(writer));
                return writer.toString();
            } catch (Exception e) {
                throw new CSApiException("Failed to convert document to XML string", e);
            }
        }
    }
}