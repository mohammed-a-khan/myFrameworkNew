package com.testforge.cs.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Utility class for date and time operations
 */
public class CSDateTimeUtils {
    
    // Common date formats
    public static final String ISO_DATE = "yyyy-MM-dd";
    public static final String ISO_DATETIME = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String ISO_DATETIME_MS = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String US_DATE = "MM/dd/yyyy";
    public static final String US_DATETIME = "MM/dd/yyyy HH:mm:ss";
    public static final String EU_DATE = "dd/MM/yyyy";
    public static final String EU_DATETIME = "dd/MM/yyyy HH:mm:ss";
    public static final String TIMESTAMP = "yyyyMMddHHmmss";
    public static final String TIMESTAMP_MS = "yyyyMMddHHmmssSSS";
    
    private static final Map<String, DateTimeFormatter> formatterCache = new HashMap<>();
    
    static {
        // Pre-cache common formatters
        formatterCache.put(ISO_DATE, DateTimeFormatter.ofPattern(ISO_DATE));
        formatterCache.put(ISO_DATETIME, DateTimeFormatter.ofPattern(ISO_DATETIME));
        formatterCache.put(ISO_DATETIME_MS, DateTimeFormatter.ofPattern(ISO_DATETIME_MS));
        formatterCache.put(US_DATE, DateTimeFormatter.ofPattern(US_DATE));
        formatterCache.put(US_DATETIME, DateTimeFormatter.ofPattern(US_DATETIME));
        formatterCache.put(EU_DATE, DateTimeFormatter.ofPattern(EU_DATE));
        formatterCache.put(EU_DATETIME, DateTimeFormatter.ofPattern(EU_DATETIME));
        formatterCache.put(TIMESTAMP, DateTimeFormatter.ofPattern(TIMESTAMP));
        formatterCache.put(TIMESTAMP_MS, DateTimeFormatter.ofPattern(TIMESTAMP_MS));
    }
    
    private CSDateTimeUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get current date
     */
    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }
    
    /**
     * Get current date in specific timezone
     */
    public static LocalDate getCurrentDate(String timezone) {
        return LocalDate.now(ZoneId.of(timezone));
    }
    
    /**
     * Get current time
     */
    public static LocalTime getCurrentTime() {
        return LocalTime.now();
    }
    
    /**
     * Get current date time
     */
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }
    
    /**
     * Get current date time in specific timezone
     */
    public static ZonedDateTime getCurrentDateTime(String timezone) {
        return ZonedDateTime.now(ZoneId.of(timezone));
    }
    
    /**
     * Get current timestamp in milliseconds
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * Get current timestamp in seconds
     */
    public static long getCurrentTimestampSeconds() {
        return Instant.now().getEpochSecond();
    }
    
    /**
     * Format date to string
     */
    public static String formatDate(LocalDate date, String pattern) {
        return getFormatter(pattern).format(date);
    }
    
    /**
     * Format date time to string
     */
    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        return getFormatter(pattern).format(dateTime);
    }
    
    /**
     * Format zoned date time to string
     */
    public static String formatDateTime(ZonedDateTime dateTime, String pattern) {
        return getFormatter(pattern).format(dateTime);
    }
    
    /**
     * Parse date from string
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        try {
            return LocalDate.parse(dateStr, getFormatter(pattern));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Failed to parse date: " + dateStr + " with pattern: " + pattern, e);
        }
    }
    
    /**
     * Parse date time from string
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        try {
            return LocalDateTime.parse(dateTimeStr, getFormatter(pattern));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Failed to parse datetime: " + dateTimeStr + " with pattern: " + pattern, e);
        }
    }
    
    /**
     * Try parse date with multiple patterns
     */
    public static LocalDate tryParseDate(String dateStr, String... patterns) {
        for (String pattern : patterns) {
            try {
                return parseDate(dateStr, pattern);
            } catch (IllegalArgumentException e) {
                // Try next pattern
            }
        }
        throw new IllegalArgumentException("Failed to parse date: " + dateStr + " with any provided pattern");
    }
    
    /**
     * Add days to date
     */
    public static LocalDate addDays(LocalDate date, long days) {
        return date.plusDays(days);
    }
    
    /**
     * Add weeks to date
     */
    public static LocalDate addWeeks(LocalDate date, long weeks) {
        return date.plusWeeks(weeks);
    }
    
    /**
     * Add months to date
     */
    public static LocalDate addMonths(LocalDate date, long months) {
        return date.plusMonths(months);
    }
    
    /**
     * Add years to date
     */
    public static LocalDate addYears(LocalDate date, long years) {
        return date.plusYears(years);
    }
    
    /**
     * Add hours to date time
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        return dateTime.plusHours(hours);
    }
    
    /**
     * Add minutes to date time
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime.plusMinutes(minutes);
    }
    
    /**
     * Add seconds to date time
     */
    public static LocalDateTime addSeconds(LocalDateTime dateTime, long seconds) {
        return dateTime.plusSeconds(seconds);
    }
    
    /**
     * Get difference in days between two dates
     */
    public static long getDaysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }
    
    /**
     * Get difference in hours between two date times
     */
    public static long getHoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }
    
    /**
     * Get difference in minutes between two date times
     */
    public static long getMinutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }
    
    /**
     * Get difference in seconds between two date times
     */
    public static long getSecondsBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.SECONDS.between(start, end);
    }
    
    /**
     * Check if date is weekend
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
    
    /**
     * Check if date is weekday
     */
    public static boolean isWeekday(LocalDate date) {
        return !isWeekend(date);
    }
    
    /**
     * Get first day of month
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }
    
    /**
     * Get last day of month
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }
    
    /**
     * Get first day of year
     */
    public static LocalDate getFirstDayOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfYear());
    }
    
    /**
     * Get last day of year
     */
    public static LocalDate getLastDayOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfYear());
    }
    
    /**
     * Get days between two LocalDateTime instances
     */
    public static long daysBetween(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return ChronoUnit.DAYS.between(dateTime1, dateTime2);
    }
    
    /**
     * Get days between two LocalDate instances
     */
    public static long daysBetween(LocalDate date1, LocalDate date2) {
        return ChronoUnit.DAYS.between(date1, date2);
    }
    
    /**
     * Check if date string is valid
     */
    public static boolean isValidDateString(String dateStr, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDate.parse(dateStr, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    
    /**
     * Get next working day
     */
    public static LocalDate getNextWorkingDay(LocalDate date) {
        LocalDate nextDay = date.plusDays(1);
        while (isWeekend(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }
    
    /**
     * Get previous working day
     */
    public static LocalDate getPreviousWorkingDay(LocalDate date) {
        LocalDate prevDay = date.minusDays(1);
        while (isWeekend(prevDay)) {
            prevDay = prevDay.minusDays(1);
        }
        return prevDay;
    }
    
    /**
     * Convert LocalDate to Date
     */
    public static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * Convert LocalDateTime to Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * Convert Date to LocalDate
     */
    public static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    
    /**
     * Convert Date to LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * Convert timestamp to LocalDateTime
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
    
    /**
     * Convert LocalDateTime to timestamp
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * Get age from birth date
     */
    public static int getAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
    
    /**
     * Get list of dates between two dates
     */
    public static List<LocalDate> getDatesBetween(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }
    
    /**
     * Get list of working days between two dates
     */
    public static List<LocalDate> getWorkingDaysBetween(LocalDate start, LocalDate end) {
        return getDatesBetween(start, end).stream()
            .filter(CSDateTimeUtils::isWeekday)
            .toList();
    }
    
    /**
     * Format duration in human readable format
     */
    public static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    /**
     * Get start of day
     */
    public static LocalDateTime getStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }
    
    /**
     * Get end of day
     */
    public static LocalDateTime getEndOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
    
    /**
     * Check if date is in past
     */
    public static boolean isPast(LocalDate date) {
        return date.isBefore(LocalDate.now());
    }
    
    /**
     * Check if date is in future
     */
    public static boolean isFuture(LocalDate date) {
        return date.isAfter(LocalDate.now());
    }
    
    /**
     * Check if date is today
     */
    public static boolean isToday(LocalDate date) {
        return date.equals(LocalDate.now());
    }
    
    /**
     * Get quarter of year
     */
    public static int getQuarter(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }
    
    /**
     * Get week of year
     */
    public static int getWeekOfYear(LocalDate date) {
        return date.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfYear());
    }
    
    /**
     * Parse duration from string (e.g., "2h 30m", "1d 2h", "45s")
     */
    public static Duration parseDuration(String durationStr) {
        Duration duration = Duration.ZERO;
        String[] parts = durationStr.toLowerCase().split("\\s+");
        
        for (String part : parts) {
            if (part.endsWith("d")) {
                duration = duration.plusDays(Long.parseLong(part.substring(0, part.length() - 1)));
            } else if (part.endsWith("h")) {
                duration = duration.plusHours(Long.parseLong(part.substring(0, part.length() - 1)));
            } else if (part.endsWith("m")) {
                duration = duration.plusMinutes(Long.parseLong(part.substring(0, part.length() - 1)));
            } else if (part.endsWith("s")) {
                duration = duration.plusSeconds(Long.parseLong(part.substring(0, part.length() - 1)));
            }
        }
        
        return duration;
    }
    
    /**
     * Get time zones
     */
    public static Set<String> getAvailableTimeZones() {
        return ZoneId.getAvailableZoneIds();
    }
    
    /**
     * Convert between time zones
     */
    public static ZonedDateTime convertTimeZone(LocalDateTime dateTime, String fromZone, String toZone) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of(fromZone));
        return zonedDateTime.withZoneSameInstant(ZoneId.of(toZone));
    }
    
    /**
     * Sleep for specified duration
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
    
    /**
     * Sleep for specified milliseconds
     */
    public static void sleep(long milliseconds) {
        sleep(Duration.ofMillis(milliseconds));
    }
    
    /**
     * Get formatter from cache or create new one
     */
    private static DateTimeFormatter getFormatter(String pattern) {
        return formatterCache.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }
}