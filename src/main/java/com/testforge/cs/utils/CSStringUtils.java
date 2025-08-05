package com.testforge.cs.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for string operations
 */
public class CSStringUtils {
    
    private CSStringUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * Check if string is null, empty, or contains only whitespace
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Check if string is not null and not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * Check if string is not null, not empty, and not just whitespace
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * Trim string and return empty string if null
     */
    public static String trim(String str) {
        return str == null ? "" : str.trim();
    }
    
    /**
     * Trim string and return null if empty
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return trimmed.isEmpty() ? null : trimmed;
    }
    
    /**
     * Trim string and return default if empty
     */
    public static String trimToDefault(String str, String defaultValue) {
        String trimmed = trim(str);
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }
    
    /**
     * Left pad string
     */
    public static String leftPad(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        return String.valueOf(padChar).repeat(length - str.length()) + str;
    }
    
    /**
     * Right pad string
     */
    public static String rightPad(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        return str + String.valueOf(padChar).repeat(length - str.length());
    }
    
    /**
     * Center string
     */
    public static String center(String str, int length, char padChar) {
        if (str == null || str.length() >= length) {
            return str;
        }
        int leftPadding = (length - str.length()) / 2;
        int rightPadding = length - str.length() - leftPadding;
        return String.valueOf(padChar).repeat(leftPadding) + str + 
               String.valueOf(padChar).repeat(rightPadding);
    }
    
    /**
     * Capitalize first letter
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    /**
     * Capitalize each word
     */
    public static String capitalizeWords(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Arrays.stream(str.split("\\s+"))
            .map(CSStringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }
    
    /**
     * Convert to camel case
     */
    public static String toCamelCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        String[] words = str.split("[\\s_-]+");
        StringBuilder result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            result.append(capitalize(words[i]));
        }
        return result.toString();
    }
    
    /**
     * Convert to snake case
     */
    public static String toSnakeCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1_$2")
                  .replaceAll("[\\s-]+", "_")
                  .toLowerCase();
    }
    
    /**
     * Convert to kebab case
     */
    public static String toKebabCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1-$2")
                  .replaceAll("[\\s_]+", "-")
                  .toLowerCase();
    }
    
    /**
     * Reverse string
     */
    public static String reverse(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return new StringBuilder(str).reverse().toString();
    }
    
    /**
     * Remove accents from string
     */
    public static String removeAccents(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Normalizer.normalize(str, Normalizer.Form.NFD)
            .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
    
    /**
     * Extract digits from string
     */
    public static String extractDigits(String str) {
        if (isEmpty(str)) {
            return "";
        }
        return str.replaceAll("[^0-9]", "");
    }
    
    /**
     * Extract letters from string
     */
    public static String extractLetters(String str) {
        if (isEmpty(str)) {
            return "";
        }
        return str.replaceAll("[^a-zA-Z]", "");
    }
    
    /**
     * Extract alphanumeric characters from string
     */
    public static String extractAlphanumeric(String str) {
        if (isEmpty(str)) {
            return "";
        }
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }
    
    /**
     * Count occurrences of substring
     */
    public static int countOccurrences(String str, String substring) {
        if (isEmpty(str) || isEmpty(substring)) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
    
    /**
     * Join strings with delimiter
     */
    public static String join(String delimiter, String... strings) {
        return String.join(delimiter, strings);
    }
    
    /**
     * Join collection with delimiter
     */
    public static String join(Collection<?> collection, String delimiter) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        return collection.stream()
            .map(Object::toString)
            .collect(Collectors.joining(delimiter));
    }
    
    /**
     * Split string by delimiter with limit
     */
    public static List<String> split(String str, String delimiter, int limit) {
        if (isEmpty(str)) {
            return new ArrayList<>();
        }
        return Arrays.asList(str.split(Pattern.quote(delimiter), limit));
    }
    
    /**
     * Split string by delimiter
     */
    public static List<String> split(String str, String delimiter) {
        return split(str, delimiter, -1);
    }
    
    /**
     * Truncate string to specified length
     */
    public static String truncate(String str, int maxLength) {
        return truncate(str, maxLength, "...");
    }
    
    /**
     * Truncate string with custom suffix
     */
    public static String truncate(String str, int maxLength, String suffix) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - suffix.length()) + suffix;
    }
    
    /**
     * Generate random string
     */
    public static String randomString(int length) {
        return randomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
    }
    
    /**
     * Generate random string with custom characters
     */
    public static String randomString(int length, String characters) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }
    
    /**
     * Generate random alphabetic string
     */
    public static String randomAlphabetic(int length) {
        return randomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
    }
    
    /**
     * Generate random numeric string
     */
    public static String randomNumeric(int length) {
        return randomString(length, "0123456789");
    }
    
    /**
     * Generate random alphanumeric string
     */
    public static String randomAlphanumeric(int length) {
        return randomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
    }
    
    /**
     * Calculate MD5 hash
     */
    public static String md5(String str) {
        return hash(str, "MD5");
    }
    
    /**
     * Calculate SHA-256 hash
     */
    public static String sha256(String str) {
        return hash(str, "SHA-256");
    }
    
    /**
     * Calculate hash
     */
    private static String hash(String str, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found: " + algorithm, e);
        }
    }
    
    /**
     * Wrap text to specified width
     */
    public static String wrap(String str, int width) {
        if (isEmpty(str) || width <= 0) {
            return str;
        }
        
        StringBuilder wrapped = new StringBuilder();
        String[] words = str.split("\\s+");
        int lineLength = 0;
        
        for (String word : words) {
            if (lineLength + word.length() > width) {
                if (lineLength > 0) {
                    wrapped.append("\n");
                    lineLength = 0;
                }
            }
            if (lineLength > 0) {
                wrapped.append(" ");
                lineLength++;
            }
            wrapped.append(word);
            lineLength += word.length();
        }
        
        return wrapped.toString();
    }
    
    /**
     * Check if string contains only digits
     */
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return str.matches("\\d+");
    }
    
    /**
     * Check if string contains only letters
     */
    public static boolean isAlpha(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return str.matches("[a-zA-Z]+");
    }
    
    /**
     * Check if string contains only alphanumeric characters
     */
    public static boolean isAlphanumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return str.matches("[a-zA-Z0-9]+");
    }
    
    /**
     * Check if string is a valid email
     */
    public static boolean isEmail(String str) {
        if (isEmpty(str)) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        return str.matches(emailRegex);
    }
    
    /**
     * Check if string is a valid URL
     */
    public static boolean isUrl(String str) {
        if (isEmpty(str)) {
            return false;
        }
        String urlRegex = "^(https?|ftp)://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?$";
        return str.matches(urlRegex);
    }
    
    /**
     * Remove HTML tags
     */
    public static String stripHtml(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replaceAll("<[^>]*>", "");
    }
    
    /**
     * Escape HTML special characters
     */
    public static String escapeHtml(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * Unescape HTML special characters
     */
    public static String unescapeHtml(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&quot;", "\"")
                  .replace("&#39;", "'");
    }
    
    /**
     * Convert string to boolean
     */
    public static boolean toBoolean(String str) {
        return toBoolean(str, false);
    }
    
    /**
     * Convert string to boolean with default
     */
    public static boolean toBoolean(String str, boolean defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        String trimmed = str.trim().toLowerCase();
        return "true".equals(trimmed) || "yes".equals(trimmed) || 
               "1".equals(trimmed) || "on".equals(trimmed);
    }
    
    /**
     * Get substring between two strings
     */
    public static String substringBetween(String str, String start, String end) {
        if (isEmpty(str) || isEmpty(start) || isEmpty(end)) {
            return null;
        }
        int startIndex = str.indexOf(start);
        if (startIndex == -1) {
            return null;
        }
        startIndex += start.length();
        int endIndex = str.indexOf(end, startIndex);
        if (endIndex == -1) {
            return null;
        }
        return str.substring(startIndex, endIndex);
    }
    
    /**
     * Get all substrings between two strings
     */
    public static List<String> substringsBetween(String str, String start, String end) {
        List<String> results = new ArrayList<>();
        if (isEmpty(str) || isEmpty(start) || isEmpty(end)) {
            return results;
        }
        
        int startIndex = 0;
        while ((startIndex = str.indexOf(start, startIndex)) != -1) {
            startIndex += start.length();
            int endIndex = str.indexOf(end, startIndex);
            if (endIndex != -1) {
                results.add(str.substring(startIndex, endIndex));
                startIndex = endIndex + end.length();
            } else {
                break;
            }
        }
        return results;
    }
}