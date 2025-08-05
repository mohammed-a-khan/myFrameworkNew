package com.testforge.cs.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.testforge.cs.exceptions.CSDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility class for JSON operations using Jackson
 */
public class CSJsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(CSJsonUtils.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectMapper prettyMapper = new ObjectMapper();
    
    static {
        // Configure mappers
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.findAndRegisterModules();
        
        prettyMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        prettyMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
        prettyMapper.findAndRegisterModules();
    }
    
    private CSJsonUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Convert object to JSON string
     */
    public static String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to convert object to JSON", e);
        }
    }
    
    /**
     * Convert object to pretty JSON string
     */
    public static String toPrettyJson(Object object) {
        try {
            return prettyMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to convert object to pretty JSON", e);
        }
    }
    
    /**
     * Parse JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to parse JSON to " + clazz.getName(), e);
        }
    }
    
    /**
     * Parse JSON string to object with TypeReference
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return mapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to parse JSON to type reference", e);
        }
    }
    
    /**
     * Read JSON from file
     */
    public static <T> T readJsonFile(String filePath, Class<T> clazz) {
        try {
            logger.debug("Reading JSON file: {}", filePath);
            return mapper.readValue(new File(filePath), clazz);
        } catch (IOException e) {
            throw new CSDataException("Failed to read JSON file: " + filePath, e);
        }
    }
    
    /**
     * Read JSON from file with TypeReference
     */
    public static <T> T readJsonFile(String filePath, TypeReference<T> typeRef) {
        try {
            logger.debug("Reading JSON file: {}", filePath);
            return mapper.readValue(new File(filePath), typeRef);
        } catch (IOException e) {
            throw new CSDataException("Failed to read JSON file: " + filePath, e);
        }
    }
    
    /**
     * Parse JSON string to Map
     */
    public static Map<String, Object> parseJson(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {});
    }
    
    /**
     * Parse JSON array to List of Maps
     */
    public static List<Map<String, Object>> parseJsonArray(String json) {
        return fromJson(json, new TypeReference<List<Map<String, Object>>>() {});
    }
    
    /**
     * Write object to JSON file
     */
    public static void writeJsonFile(String filePath, Object object) {
        writeJsonFile(filePath, object, false);
    }
    
    /**
     * Write object to JSON file with pretty print option
     */
    public static void writeJsonFile(String filePath, Object object, boolean prettyPrint) {
        try {
            logger.debug("Writing JSON file: {}", filePath);
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            
            if (prettyPrint) {
                prettyMapper.writeValue(new File(filePath), object);
            } else {
                mapper.writeValue(new File(filePath), object);
            }
        } catch (IOException e) {
            throw new CSDataException("Failed to write JSON file: " + filePath, e);
        }
    }
    
    /**
     * Parse JSON to Map
     */
    public static Map<String, Object> jsonToMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {});
    }
    
    /**
     * Parse JSON to List
     */
    public static List<Object> jsonToList(String json) {
        return fromJson(json, new TypeReference<List<Object>>() {});
    }
    
    /**
     * Parse JSON to List of Maps
     */
    public static List<Map<String, Object>> jsonToListOfMaps(String json) {
        return fromJson(json, new TypeReference<List<Map<String, Object>>>() {});
    }
    
    /**
     * Get value from JSON path
     */
    public static String getValue(String json, String jsonPath) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode valueNode = getNodeByPath(rootNode, jsonPath);
            return valueNode != null ? valueNode.asText() : null;
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to get value from JSON path: " + jsonPath, e);
        }
    }
    
    /**
     * Get object from JSON path
     */
    public static <T> T getObject(String json, String jsonPath, Class<T> clazz) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode valueNode = getNodeByPath(rootNode, jsonPath);
            return valueNode != null ? mapper.treeToValue(valueNode, clazz) : null;
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to get object from JSON path: " + jsonPath, e);
        }
    }
    
    /**
     * Set value in JSON
     */
    public static String setValue(String json, String jsonPath, Object value) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            setNodeByPath(rootNode, jsonPath, value);
            return mapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to set value in JSON path: " + jsonPath, e);
        }
    }
    
    /**
     * Merge two JSON strings
     */
    public static String mergeJson(String json1, String json2) {
        try {
            JsonNode node1 = mapper.readTree(json1);
            JsonNode node2 = mapper.readTree(json2);
            return mapper.writeValueAsString(merge(node1, node2));
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to merge JSON", e);
        }
    }
    
    /**
     * Check if JSON is valid
     */
    public static boolean isValidJson(String json) {
        try {
            mapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    /**
     * Compare two JSON strings
     */
    public static boolean compareJson(String json1, String json2) {
        try {
            JsonNode node1 = mapper.readTree(json1);
            JsonNode node2 = mapper.readTree(json2);
            return node1.equals(node2);
        } catch (JsonProcessingException e) {
            logger.error("Failed to compare JSON", e);
            return false;
        }
    }
    
    /**
     * Create empty JSON object
     */
    public static ObjectNode createObject() {
        return mapper.createObjectNode();
    }
    
    /**
     * Create empty JSON array
     */
    public static ArrayNode createArray() {
        return mapper.createArrayNode();
    }
    
    /**
     * Convert Map to JSON object
     */
    public static ObjectNode mapToJsonObject(Map<String, Object> map) {
        return mapper.convertValue(map, ObjectNode.class);
    }
    
    /**
     * Convert List to JSON array
     */
    public static ArrayNode listToJsonArray(List<?> list) {
        return mapper.convertValue(list, ArrayNode.class);
    }
    
    /**
     * Get all keys from JSON object
     */
    public static Set<String> getKeys(String json) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            Set<String> keys = new HashSet<>();
            if (rootNode.isObject()) {
                rootNode.fieldNames().forEachRemaining(keys::add);
            }
            return keys;
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to get keys from JSON", e);
        }
    }
    
    /**
     * Get all values for a specific key in JSON array
     */
    public static List<String> getValuesFromArray(String json, String key) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            List<String> values = new ArrayList<>();
            
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    JsonNode valueNode = node.get(key);
                    if (valueNode != null) {
                        values.add(valueNode.asText());
                    }
                }
            }
            return values;
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to get values from JSON array", e);
        }
    }
    
    /**
     * Filter JSON array by key-value
     */
    public static String filterJsonArray(String json, String key, String value) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            ArrayNode filteredArray = mapper.createArrayNode();
            
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    JsonNode keyNode = node.get(key);
                    if (keyNode != null && keyNode.asText().equals(value)) {
                        filteredArray.add(node);
                    }
                }
            }
            return mapper.writeValueAsString(filteredArray);
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to filter JSON array", e);
        }
    }
    
    /**
     * Sort JSON array by key
     */
    public static String sortJsonArray(String json, String key, boolean ascending) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            if (!rootNode.isArray()) {
                return json;
            }
            
            List<JsonNode> nodeList = new ArrayList<>();
            rootNode.forEach(nodeList::add);
            
            nodeList.sort((n1, n2) -> {
                JsonNode v1 = n1.get(key);
                JsonNode v2 = n2.get(key);
                if (v1 == null || v2 == null) return 0;
                
                int result = v1.asText().compareTo(v2.asText());
                return ascending ? result : -result;
            });
            
            ArrayNode sortedArray = mapper.createArrayNode();
            nodeList.forEach(sortedArray::add);
            
            return mapper.writeValueAsString(sortedArray);
        } catch (JsonProcessingException e) {
            throw new CSDataException("Failed to sort JSON array", e);
        }
    }
    
    /**
     * Get node by path (supports dot notation and array indices)
     */
    private static JsonNode getNodeByPath(JsonNode node, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = node;
        
        for (String part : parts) {
            if (current == null) return null;
            
            // Check if part contains array index
            if (part.contains("[") && part.contains("]")) {
                String fieldName = part.substring(0, part.indexOf("["));
                String indexStr = part.substring(part.indexOf("[") + 1, part.indexOf("]"));
                int index = Integer.parseInt(indexStr);
                
                current = current.get(fieldName);
                if (current != null && current.isArray() && index < current.size()) {
                    current = current.get(index);
                } else {
                    return null;
                }
            } else {
                current = current.get(part);
            }
        }
        
        return current;
    }
    
    /**
     * Set node by path
     */
    private static void setNodeByPath(JsonNode rootNode, String path, Object value) {
        String[] parts = path.split("\\.");
        JsonNode current = rootNode;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            if (part.contains("[") && part.contains("]")) {
                String fieldName = part.substring(0, part.indexOf("["));
                String indexStr = part.substring(part.indexOf("[") + 1, part.indexOf("]"));
                int index = Integer.parseInt(indexStr);
                
                JsonNode arrayNode = current.get(fieldName);
                if (arrayNode != null && arrayNode.isArray() && index < arrayNode.size()) {
                    current = arrayNode.get(index);
                } else {
                    return;
                }
            } else {
                JsonNode nextNode = current.get(part);
                if (nextNode == null) {
                    ((ObjectNode) current).putObject(part);
                    nextNode = current.get(part);
                }
                current = nextNode;
            }
        }
        
        // Set the final value
        String lastPart = parts[parts.length - 1];
        if (current instanceof ObjectNode) {
            if (value instanceof String) {
                ((ObjectNode) current).put(lastPart, (String) value);
            } else if (value instanceof Integer) {
                ((ObjectNode) current).put(lastPart, (Integer) value);
            } else if (value instanceof Long) {
                ((ObjectNode) current).put(lastPart, (Long) value);
            } else if (value instanceof Double) {
                ((ObjectNode) current).put(lastPart, (Double) value);
            } else if (value instanceof Boolean) {
                ((ObjectNode) current).put(lastPart, (Boolean) value);
            } else {
                ((ObjectNode) current).putPOJO(lastPart, value);
            }
        }
    }
    
    /**
     * Merge two JSON nodes
     */
    private static JsonNode merge(JsonNode node1, JsonNode node2) {
        if (node1.isObject() && node2.isObject()) {
            ObjectNode merged = mapper.createObjectNode();
            merged.setAll((ObjectNode) node1);
            merged.setAll((ObjectNode) node2);
            return merged;
        } else if (node1.isArray() && node2.isArray()) {
            ArrayNode merged = mapper.createArrayNode();
            merged.addAll((ArrayNode) node1);
            merged.addAll((ArrayNode) node2);
            return merged;
        }
        return node2; // Return second node if types don't match
    }
}