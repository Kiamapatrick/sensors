package com.example.sensors;

import android.hardware.Sensor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Model class representing a sensor item for display in the inventory.
 * Wraps a real Sensor (nullable) plus display fields. Includes a synthetic flag
 * for test rows that are not real sensors.
 */
public final class SensorItem {

    @Nullable
    private final Sensor sensor;       // Real sensor, null for synthetic
    @NonNull
    private final String name;         // Display name
    @NonNull
    private final String vendor;       // Display vendor
    private final int type;            // Sensor type constant
    @NonNull
    private final String stringType;   // Sensor string type
    private final boolean synthetic;   // True if this is a synthetic test row

    private SensorItem(@Nullable Sensor sensor, @NonNull String name, @NonNull String vendor,
                       int type, @NonNull String stringType, boolean synthetic) {
        this.sensor = sensor;
        this.name = name;
        this.vendor = vendor;
        this.type = type;
        this.stringType = stringType;
        this.synthetic = synthetic;
    }

    /**
     * Factory method to create a SensorItem from a real Sensor.
     */
    @NonNull
    public static SensorItem fromSensor(@NonNull Sensor sensor) {
        String name = sensor.getName();
        String vendor = sensor.getVendor() != null ? sensor.getVendor() : "";
        int type = sensor.getType();
        String stringType = sensor.getStringType() != null ? sensor.getStringType() : "";
        return new SensorItem(sensor, name, vendor, type, stringType, false);
    }

    /**
     * Factory method to create a synthetic long-name test row.
     * Type 0 is used to indicate synthetic; stringType is empty.
     */
    @NonNull
    public static SensorItem syntheticLongName() {
        String longName = "SYNTHETIC TEST ROW: Ultra-Wideband Multi-Axis Ambient Environmental Light and Proximity Fusion Sensor Hub Prototype v2";
        String longVendor = "Synthetic Vendor for Testing Long Name Display and Rotation Handling";
        return new SensorItem(null, longName, longVendor, 0, "", true);
    }

    // Getters
    @Nullable
    public Sensor getSensor() {
        return sensor;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getVendor() {
        return vendor;
    }

    public int getType() {
        return type;
    }

    @NonNull
    public String getStringType() {
        return stringType;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    /**
     * Returns true if this item represents a real sensor (not synthetic).
     */
    public boolean isReal() {
        return !synthetic && sensor != null;
    }

    /**
     * Returns the wake-up status from the real sensor, or false for synthetic.
     */
    public boolean isWakeUpSensor() {
        return sensor != null && sensor.isWakeUpSensor();
    }

    /**
     * Returns the reporting mode from the real sensor, or -1 for synthetic.
     */
    public int getReportingMode() {
        return sensor != null ? sensor.getReportingMode() : -1;
    }

    /**
     * Returns the maximum range from the real sensor, or -1 for synthetic.
     */
    public float getMaximumRange() {
        return sensor != null ? sensor.getMaximumRange() : -1f;
    }

    /**
     * Returns the resolution from the real sensor, or -1 for synthetic.
     */
    public float getResolution() {
        return sensor != null ? sensor.getResolution() : -1f;
    }

    /**
     * Returns the min delay from the real sensor, or -1 for synthetic.
     */
    public int getMinDelay() {
        return sensor != null ? sensor.getMinDelay() : -1;
    }

    /**
     * Returns the max delay from the real sensor, or -1 for synthetic.
     */
    public int getMaxDelay() {
        return sensor != null ? sensor.getMaxDelay() : -1;
    }

    /**
     * Returns the power from the real sensor, or -1 for synthetic.
     */
    public float getPower() {
        return sensor != null ? sensor.getPower() : -1f;
    }

    /**
     * Returns the FIFO max event count from the real sensor, or -1 for synthetic.
     */
    public int getFifoMaxEventCount() {
        return sensor != null ? sensor.getFifoMaxEventCount() : -1;
    }

    /**
     * Returns the FIFO reserved event count from the real sensor, or -1 for synthetic.
     */
    public int getFifoReservedEventCount() {
        return sensor != null ? sensor.getFifoReservedEventCount() : -1;
    }

    /**
     * Returns the version from the real sensor, or -1 for synthetic.
     */
    public int getVersion() {
        return sensor != null ? sensor.getVersion() : -1;
    }

    /**
     * Returns true if the real sensor is a dynamic sensor (API 24+), false for synthetic.
     */
    public boolean isDynamicSensor() {
        return sensor != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
                && sensor.isDynamicSensor();
    }

    /**
     * Returns true if the real sensor supports additional info (API 26+), false for synthetic.
     */
    public boolean isAdditionalInfoSupported() {
        return sensor != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                && sensor.isAdditionalInfoSupported();
    }
}