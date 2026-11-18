package com.example.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;

/**
 * Helper class for formatting sensor information into human-readable strings with correct units.
 * Centralizes all "what this means" explanations and unit mappings so they are consistent across screens.
 */
public final class SensorInfoFormatter {

    private SensorInfoFormatter() {
        // Utility class, no instances
    }

    /**
     * Returns a human-readable name for a sensor type constant.
     * Uses sensor.getStringType() as primary, falls back to known constants.
     */
    @NonNull
    public static String getReadableTypeName(@NonNull Sensor sensor) {
        // getStringType() returns vendor-specific type string (e.g., "android.sensor.accelerometer")
        String stringType = sensor.getStringType();
        if (stringType != null && !stringType.isEmpty()) {
            // Try to extract a readable name from the string type
            String simpleName = stringType.substring(stringType.lastIndexOf('.') + 1);
            return capitalizeWords(simpleName.replace('_', ' '));
        }
        // Fallback to known type constants
        return getTypeNameFromConstant(sensor.getType());
    }

    /**
     * Returns a human-readable name for a SensorItem.
     * Prefers stringType for real sensors, falls back to constant mapping.
     * For synthetic items, returns the name as-is.
     */
    @NonNull
    public static String getReadableTypeNameFromItem(@NonNull SensorItem item) {
        if (item.isSynthetic()) {
            return "Synthetic Test";
        }
        String stringType = item.getStringType();
        if (stringType != null && !stringType.isEmpty()) {
            String simpleName = stringType.substring(stringType.lastIndexOf('.') + 1);
            return capitalizeWords(simpleName.replace('_', ' '));
        }
        return getTypeNameFromConstant(item.getType());
    }

    /**
     * Returns a human-readable name for a sensor type constant (without a Sensor object).
     * Only uses constants available in API 24 (minSdk). Newer constants fall to default.
     */
    @NonNull
    public static String getTypeNameFromConstant(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return "Accelerometer";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "Magnetic Field";
            case Sensor.TYPE_ORIENTATION:
                return "Orientation (deprecated)";
            case Sensor.TYPE_GYROSCOPE:
                return "Gyroscope";
            case Sensor.TYPE_LIGHT:
                return "Light";
            case Sensor.TYPE_PRESSURE:
                return "Pressure";
            case Sensor.TYPE_TEMPERATURE:
                return "Temperature (deprecated)";
            case Sensor.TYPE_PROXIMITY:
                return "Proximity";
            case Sensor.TYPE_GRAVITY:
                return "Gravity";
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return "Linear Acceleration";
            case Sensor.TYPE_ROTATION_VECTOR:
                return "Rotation Vector";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "Relative Humidity";
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "Ambient Temperature";
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return "Magnetic Field Uncalibrated";
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return "Game Rotation Vector";
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return "Gyroscope Uncalibrated";
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return "Significant Motion";
            case Sensor.TYPE_STEP_DETECTOR:
                return "Step Detector";
            case Sensor.TYPE_STEP_COUNTER:
                return "Step Counter";
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return "Geomagnetic Rotation Vector";
            case Sensor.TYPE_HEART_RATE:
                return "Heart Rate";
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                return "Accelerometer Uncalibrated";
            // Constants added in API 26+ fall through to default
            // TYPE_TILT_DETECTOR (26), TYPE_WAKE_GESTURE (26), TYPE_GLANCE_GESTURE (26),
            // TYPE_PICK_UP_GESTURE (26), TYPE_WRIST_TILT_GESTURE (26), TYPE_DEVICE_ORIENTATION (26),
            // TYPE_POSE_6DOF (28), TYPE_STATIONARY_DETECT (29), TYPE_MOTION_DETECT (29),
            // TYPE_HEART_BEAT (29), TYPE_DYNAMIC_SENSOR_META (29), TYPE_ADDITIONAL_INFO (29),
            // TYPE_LOW_LATENCY_OFFBODY_DETECT (29), TYPE_HINGE_ANGLE (30), TYPE_HEAD_TRACKER (30),
            // TYPE_ACCELEROMETER_LIMITED_AXES (33), TYPE_GYROSCOPE_LIMITED_AXES (33),
            // TYPE_MAGNETOMETER_LIMITED_AXES (33), TYPE_HEADING (33)
            default:
                return "Unknown (type " + type + ")";
        }
    }

    /**
     * Returns the unit string for a given sensor type.
     * This is used for range, resolution, and similar metadata fields.
     */
    @NonNull
    public static String getUnitForSensorType(int sensorType, @NonNull Context context) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return context.getString(R.string.unit_m_s2);
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return context.getString(R.string.unit_rad_s);
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return context.getString(R.string.unit_uT);
            case Sensor.TYPE_LIGHT:
                return context.getString(R.string.unit_lux);
            case Sensor.TYPE_PROXIMITY:
                return context.getString(R.string.unit_cm);
            case Sensor.TYPE_PRESSURE:
                return context.getString(R.string.unit_hPa);
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
            case Sensor.TYPE_TEMPERATURE:
                return context.getString(R.string.unit_celsius);
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return context.getString(R.string.unit_percent);
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return context.getString(R.string.unit_unitless);
            case Sensor.TYPE_STEP_COUNTER:
            case Sensor.TYPE_STEP_DETECTOR:
                return context.getString(R.string.unit_steps);
            case Sensor.TYPE_HEART_RATE:
            case Sensor.TYPE_HEART_BEAT:
                return context.getString(R.string.unit_bpm);
            default:
                return context.getString(R.string.unit_unknown);
        }
    }

    /**
     * Formats a float value with its unit, handling "not reported" cases (0 or negative).
     * Uses Locale.US to avoid localized decimal separators and trims trailing zeros.
     */
    @NonNull
    public static String formatRangeOrResolution(float value, int sensorType, @NonNull Context context) {
        if (value <= 0) {
            return context.getString(R.string.value_not_reported);
        }
        String unit = getUnitForSensorType(sensorType, context);
        return String.format(Locale.US, "%.6g %s", value, unit);
    }

    /**
     * Formats minDelay (microseconds) with Hz equivalent and explanation.
     * Uses Locale.US to avoid localized decimal separators.
     */
    @NonNull
    public static String formatMinDelay(int minDelay, @NonNull Context context) {
        if (minDelay == 0) {
            return context.getString(R.string.delay_zero_event_driven);
        }
        if (minDelay < 0) {
            return context.getString(R.string.value_not_reported);
        }
        double hz = 1_000_000.0 / minDelay;
        return String.format(Locale.US, "%d %s (≈ %.1f %s)", minDelay, context.getString(R.string.unit_microseconds), hz, context.getString(R.string.unit_hz));
    }

    /**
     * Formats maxDelay (microseconds).
     */
    @NonNull
    public static String formatMaxDelay(int maxDelay, @NonNull Context context) {
        if (maxDelay == 0) {
            return context.getString(R.string.delay_zero_not_reported);
        }
        if (maxDelay < 0) {
            return context.getString(R.string.value_not_reported);
        }
        return String.format("%d %s", maxDelay, context.getString(R.string.unit_microseconds));
    }

    /**
     * Formats power (mA) with honest "not reported" handling.
     * Uses Locale.US to avoid localized decimal separators.
     */
    @NonNull
    public static String formatPower(float power, @NonNull Context context) {
        if (power <= 0) {
            return context.getString(R.string.power_zero_not_reported);
        }
        return String.format(Locale.US, "%.3f %s", power, context.getString(R.string.unit_mA));
    }

    /**
     * Returns human-readable reporting mode string.
     * REPORTING_MODE_SPECIAL (value 3) added in API 29; use constant value for compatibility.
     */
    @NonNull
    public static String getReportingModeString(int reportingMode, @NonNull Context context) {
        switch (reportingMode) {
            case Sensor.REPORTING_MODE_CONTINUOUS: // 0
                return context.getString(R.string.reporting_continuous);
            case Sensor.REPORTING_MODE_ON_CHANGE:  // 1
                return context.getString(R.string.reporting_on_change);
            case Sensor.REPORTING_MODE_ONE_SHOT:   // 2
                return context.getString(R.string.reporting_one_shot);
            case 3: // Sensor.REPORTING_MODE_SPECIAL (API 29+)
                return context.getString(R.string.reporting_special);
            default:
                return context.getString(R.string.reporting_unknown);
        }
    }

    /**
     * Returns the "what this means" explanation for a metadata field.
     */
    @NonNull
    public static String getMeaningForField(@NonNull String fieldKey, @NonNull Context context) {
        switch (fieldKey) {
            case "range":
                return context.getString(R.string.meaning_range);
            case "resolution":
                return context.getString(R.string.meaning_resolution);
            case "minDelay":
                return context.getString(R.string.meaning_min_delay);
            case "maxDelay":
                return context.getString(R.string.meaning_max_delay);
            case "power":
                return context.getString(R.string.meaning_power);
            case "reportingMode":
                return context.getString(R.string.meaning_reporting_mode);
            case "wakeup":
                return context.getString(R.string.meaning_wakeup);
            case "fifo":
                return context.getString(R.string.meaning_fifo);
            case "dynamic":
                return context.getString(R.string.meaning_dynamic);
            case "additionalInfo":
                return context.getString(R.string.meaning_additional_info);
            default:
                return "";
        }
    }

    /**
     * Formats a boolean as Yes/No with localization.
     */
    @NonNull
    public static String formatBoolean(boolean value, @NonNull Context context) {
        return value ? context.getString(R.string.value_yes) : context.getString(R.string.value_no);
    }

    /**
     * Capitalizes each word in a string (for string type parsing).
     */
    @NonNull
    private static String capitalizeWords(@NonNull String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (c == ' ' || c == '_') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Checks if a sensor type is a wake-up sensor (by constant).
     * Note: Actual wake-up status should come from sensor.isWakeUpSensor().
     */
    public static boolean isKnownWakeUpType(int type) {
        // Most sensors can have wake-up variants; we rely on sensor.isWakeUpSensor() at runtime.
        return false;
    }
}