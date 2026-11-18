package com.example.sensors;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory singleton test log for recording evidence events (E5-01 through E5-08).
 * Survives configuration changes (rotation) because it's a static singleton.
 * Resets when the app process dies.
 */
public final class TestLog {

    private static final String TAG = "TestLog";
    private static final TestLog INSTANCE = new TestLog();

    private final List<Entry> entries = new ArrayList<>();
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private final AtomicInteger sequence = new AtomicInteger(0);

    private TestLog() {
        // Private constructor for singleton
    }

    /**
     * Returns the singleton instance.
     */
    @NonNull
    public static TestLog getInstance() {
        return INSTANCE;
    }

    /**
     * Log entry with ID, timestamp, and description.
     */
    public static final class Entry {
        public final String id;       // Fixed event ID (E5-01, E5-02, etc.)
        public final String timestamp; // HH:mm:ss.SSS
        public final String description; // Human-readable description

        public Entry(@NonNull String id, @NonNull String timestamp, @NonNull String description) {
            this.id = id;
            this.timestamp = timestamp;
            this.description = description;
        }

        @NonNull
        @Override
        public String toString() {
            return "[" + timestamp + "] " + id + ": " + description;
        }
    }

    /**
     * Adds a log entry with a fixed event ID and formatted description.
     * @param eventId Fixed event ID like "E5-01"
     * @param formatString Format string for description (String.format compatible)
     * @param args Arguments for format string
     */
    public void log(@NonNull String eventId, @NonNull String formatString, @Nullable Object... args) {
        String description = String.format(Locale.getDefault(), formatString, args);
        String timestamp = timestampFormat.format(new Date());
        Entry entry = new Entry(eventId, timestamp, description);
        synchronized (entries) {
            entries.add(entry);
        }
        Log.d(TAG, entry.toString());
    }

    /**
     * Returns an immutable snapshot of all log entries.
     */
    @NonNull
    public List<Entry> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    /**
     * Returns all log entries as a single string for copying to clipboard.
     */
    @NonNull
    public String getLogText() {
        List<Entry> snapshot = getEntries();
        if (snapshot.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Entry entry : snapshot) {
            sb.append(entry.toString()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Clears the log (mainly for testing).
     */
    public void clear() {
        synchronized (entries) {
            entries.clear();
            sequence.set(0);
        }
    }

    /**
     * Returns the number of entries.
     */
    public int size() {
        synchronized (entries) {
            return entries.size();
        }
    }

    // ==================== Convenience methods for each lab event ====================

    /**
     * E5-01: Inventory loaded with sensor count.
     */
    public void logInventoryLoaded(int sensorCount) {
        log("E5-01", "Inventory loaded (%d sensors)", sensorCount);
    }

    /**
     * E5-02: Device/environment identified.
     */
    public void logEnvironmentIdentified(@NonNull String environmentLabel) {
        log("E5-02", "Device/environment identified: %s", environmentLabel);
    }

    /**
     * E5-03: Sensor detail opened.
     */
    public void logDetailOpened(@NonNull String sensorName) {
        log("E5-03", "Detail opened for sensor: %s", sensorName);
    }

    /**
     * E5-04: Three-sensor comparison shown.
     */
    public void logComparisonShown() {
        log("E5-04", "Three-sensor comparison shown");
    }

    /**
     * E5-05: Unsupported sensor checked.
     */
    public void logSensorChecked(int sensorType, boolean available) {
        log("E5-05", "Unsupported sensor checked: type=%d, result=%s", sensorType, available ? "available" : "unavailable");
    }

    /**
     * E5-06: Empty inventory simulated.
     */
    public void logEmptyInventorySimulated(boolean simulated) {
        log("E5-06", "Empty inventory simulated: %s", simulated ? "enabled" : "disabled");
    }

    /**
     * E5-07: Rotation occurred.
     */
    public void logRotationOccurred() {
        log("E5-07", "Rotation occurred (config change)");
    }

    /**
     * E5-08: Long-name display verified.
     */
    public void logLongNameVerified() {
        log("E5-08", "Long-name display verified");
    }
}