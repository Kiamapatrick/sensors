package com.example.sensors;

import android.os.Build;
import androidx.annotation.NonNull;

/**
 * Detects whether the app is running on a physical device or an emulator.
 * Uses heuristics on Build properties as recommended by Android documentation.
 * This is a best-effort detection; no single property is 100% reliable.
 */
public final class EnvironmentDetector {

    private EnvironmentDetector() {
        // Utility class
    }

    /**
     * Result of environment detection.
     */
    public static class Result {
        public final boolean isEmulator;
        @NonNull public final String label;
        @NonNull public final String caution; // Empty if physical device

        public Result(boolean isEmulator, @NonNull String label, @NonNull String caution) {
            this.isEmulator = isEmulator;
            this.label = label;
            this.caution = caution;
        }
    }

    /**
     * Performs the detection based on Build properties.
     */
    @NonNull
    public static Result detect() {
        // Common emulator indicators from Build properties
        String fingerprint = Build.FINGERPRINT != null ? Build.FINGERPRINT.toLowerCase() : "";
        String model = Build.MODEL != null ? Build.MODEL.toLowerCase() : "";
        String hardware = Build.HARDWARE != null ? Build.HARDWARE.toLowerCase() : "";
        String product = Build.PRODUCT != null ? Build.PRODUCT.toLowerCase() : "";
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
        String brand = Build.BRAND != null ? Build.BRAND.toLowerCase() : "";
        String device = Build.DEVICE != null ? Build.DEVICE.toLowerCase() : "";
        String tags = Build.TAGS != null ? Build.TAGS.toLowerCase() : "";

        // Check for known emulator signatures
        boolean isEmulator = false;

        // 1. Build.FINGERPRINT often contains "generic", "emulator", "sdk_gphone", "ranchu"
        if (fingerprint.contains("generic")
                || fingerprint.contains("emulator")
                || fingerprint.contains("sdk_gphone")
                || fingerprint.contains("ranchu")) {
            isEmulator = true;
        }

        // 2. Build.MODEL often contains "sdk", "emulator", "android sdk built for"
        if (model.contains("sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for")) {
            isEmulator = true;
        }

        // 3. Build.HARDWARE often contains "goldfish", "ranchu"
        if (hardware.contains("goldfish")
                || hardware.contains("ranchu")) {
            isEmulator = true;
        }

        // 4. Build.PRODUCT often contains "sdk", "emulator", "google_sdk"
        if (product.contains("sdk")
                || product.contains("emulator")
                || product.contains("google_sdk")) {
            isEmulator = true;
        }

        // 5. Build.MANUFACTURER / BRAND often "generic", "google" for emulator
        if (manufacturer.contains("generic")
                || manufacturer.contains("emulator")
                || brand.contains("generic")
                || brand.contains("emulator")) {
            isEmulator = true;
        }

        // 6. Build.TAGS often contains "test-keys" for emulator (though some physical devices too)
        // Only use as supporting evidence, not primary
        if (tags.contains("test-keys") && (model.contains("sdk") || hardware.contains("goldfish"))) {
            isEmulator = true;
        }

        // 7. Explicit check for "google_sdk" in product (common for AVD images)
        if (product.equals("google_sdk") || product.equals("sdk_gphone64_arm64") || product.contains("sdk_gphone")) {
            isEmulator = true;
        }

        if (isEmulator) {
            return new Result(true,
                    "Emulator (simulated sensors)",
                    "Emulator sensor values and list are simulated and may not match a real phone.");
        } else {
            return new Result(false,
                    "Physical device",
                    "");
        }
    }
}