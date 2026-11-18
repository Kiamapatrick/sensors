package com.example.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Locale;

/**
 * SensorDetailActivity: Shows detailed metadata for a single sensor (R2, C2).
 * Displays all available fields with correct units and "what this means" explanations.
 * Guards API-level-dependent calls with SDK_INT checks.
 * Handles synthetic test rows with a banner and honest "Not reported" values.
 */
public class SensorDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SENSOR_NAME = "sensor_name";
    public static final String EXTRA_SENSOR_TYPE = "sensor_type";
    public static final String EXTRA_SENSOR_VENDOR = "sensor_vendor";
    public static final String EXTRA_SENSOR_VERSION = "sensor_version";
    public static final String EXTRA_SENSOR_MAX_RANGE = "sensor_max_range";
    public static final String EXTRA_SENSOR_RESOLUTION = "sensor_resolution";
    public static final String EXTRA_SENSOR_MIN_DELAY = "sensor_min_delay";
    public static final String EXTRA_SENSOR_MAX_DELAY = "sensor_max_delay";
    public static final String EXTRA_SENSOR_POWER = "sensor_power";
    public static final String EXTRA_SENSOR_REPORTING_MODE = "sensor_reporting_mode";
    public static final String EXTRA_SENSOR_WAKEUP = "sensor_wakeup";
    public static final String EXTRA_SENSOR_FIFO_MAX = "sensor_fifo_max";
    public static final String EXTRA_SENSOR_FIFO_RESERVED = "sensor_fifo_reserved";
    public static final String EXTRA_SENSOR_STRING_TYPE = "sensor_string_type";
    public static final String EXTRA_SENSOR_DYNAMIC = "sensor_dynamic";
    public static final String EXTRA_SENSOR_ADDITIONAL_INFO = "sensor_additional_info";
    public static final String EXTRA_SENSOR_SYNTHETIC = "sensor_synthetic";

    private LinearLayout metadataContainer;
    private View accentStrip;
    private int sensorType;
    private boolean isSynthetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_sensor_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_container), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Log rotation if this is a recreation
        if (savedInstanceState != null) {
            TestLog.getInstance().logRotationOccurred(this);
        }

        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.detail_title);
        }

        metadataContainer = findViewById(R.id.metadata_container);
        accentStrip = findViewById(R.id.detail_accent_strip);

        // Extract sensor data from intent
        String name = getIntent().getStringExtra(EXTRA_SENSOR_NAME);
        sensorType = getIntent().getIntExtra(EXTRA_SENSOR_TYPE, -1);
        String vendor = getIntent().getStringExtra(EXTRA_SENSOR_VENDOR);
        int version = getIntent().getIntExtra(EXTRA_SENSOR_VERSION, -1);
        float maxRange = getIntent().getFloatExtra(EXTRA_SENSOR_MAX_RANGE, -1f);
        float resolution = getIntent().getFloatExtra(EXTRA_SENSOR_RESOLUTION, -1f);
        int minDelay = getIntent().getIntExtra(EXTRA_SENSOR_MIN_DELAY, -1);
        int maxDelay = getIntent().getIntExtra(EXTRA_SENSOR_MAX_DELAY, -1);
        float power = getIntent().getFloatExtra(EXTRA_SENSOR_POWER, -1f);
        int reportingMode = getIntent().getIntExtra(EXTRA_SENSOR_REPORTING_MODE, -1);
        boolean wakeup = getIntent().getBooleanExtra(EXTRA_SENSOR_WAKEUP, false);
        int fifoMax = getIntent().getIntExtra(EXTRA_SENSOR_FIFO_MAX, -1);
        int fifoReserved = getIntent().getIntExtra(EXTRA_SENSOR_FIFO_RESERVED, -1);
        String stringType = getIntent().getStringExtra(EXTRA_SENSOR_STRING_TYPE);
        isSynthetic = getIntent().getBooleanExtra(EXTRA_SENSOR_SYNTHETIC, false);

        // Set name and type in header
        TextView tvName = findViewById(R.id.tv_detail_name);
        TextView tvType = findViewById(R.id.tv_detail_type);
        tvName.setText(name != null ? name : getString(R.string.value_not_reported));

        // Use the same name logic as the list: prefer readable name from string type
        String readableType;
        if (isSynthetic) {
            readableType = "Synthetic Test";
        } else if (stringType != null && !stringType.isEmpty()) {
            String simpleName = stringType.substring(stringType.lastIndexOf('.') + 1);
            readableType = capitalizeWords(simpleName.replace('_', ' '));
        } else {
            readableType = SensorInfoFormatter.getTypeNameFromConstant(sensorType);
        }
        String typeText = String.format(Locale.getDefault(), "%s (type %d) • %s",
                readableType, sensorType, stringType != null && !stringType.isEmpty() ? stringType : getString(R.string.value_not_reported));
        tvType.setText(typeText);

        // Synthetic banner
        if (isSynthetic) {
            addSyntheticBanner();
            accentStrip.setBackgroundColor(getColor(R.color.status_emulator));
        } else {
            accentStrip.setBackgroundColor(getColor(R.color.status_available));
        }

        // Populate metadata rows
        addMetadataRow(R.string.detail_vendor, vendor != null && !vendor.isEmpty() ? vendor : getString(R.string.value_not_reported), null);
        addMetadataRow(R.string.detail_version, version >= 0 ? String.valueOf(version) : getString(R.string.value_not_reported), null);

        // Range and resolution with units
        addMetadataRow(R.string.detail_max_range, SensorInfoFormatter.formatRangeOrResolution(maxRange, sensorType, this), "range");
        addMetadataRow(R.string.detail_resolution, SensorInfoFormatter.formatRangeOrResolution(resolution, sensorType, this), "resolution");

        // Min/Max delay
        addMetadataRow(R.string.detail_min_delay, SensorInfoFormatter.formatMinDelay(minDelay, this), "minDelay");
        addMetadataRow(R.string.detail_max_delay, SensorInfoFormatter.formatMaxDelay(maxDelay, this), "maxDelay");

        // Power
        addMetadataRow(R.string.detail_power, SensorInfoFormatter.formatPower(power, this), "power");

        // Reporting mode
        String reportingModeStr;
        if (reportingMode >= 0) {
            reportingModeStr = SensorInfoFormatter.getReportingModeString(reportingMode, this);
        } else if (isSynthetic) {
            reportingModeStr = getString(R.string.value_not_reported);
        } else {
            reportingModeStr = getString(R.string.value_not_reported);
        }
        addMetadataRow(R.string.detail_reporting_mode, reportingModeStr, "reportingMode");

        // Wake-up
        addMetadataRow(R.string.detail_wakeup, SensorInfoFormatter.formatBoolean(wakeup, this), "wakeup");

        // FIFO
        addMetadataRow(R.string.detail_fifo_max, fifoMax >= 0 ? String.valueOf(fifoMax) : getString(R.string.value_not_reported), "fifo");
        addMetadataRow(R.string.detail_fifo_reserved, fifoReserved >= 0 ? String.valueOf(fifoReserved) : getString(R.string.value_not_reported), "fifo");

        // Dynamic sensor (API 24+)
        boolean hasDynamicExtra = getIntent().hasExtra(EXTRA_SENSOR_DYNAMIC);
        boolean dynamic = getIntent().getBooleanExtra(EXTRA_SENSOR_DYNAMIC, false);
        if (isSynthetic) {
            addMetadataRow(R.string.detail_dynamic, getString(R.string.value_not_reported), "dynamic");
        } else if (hasDynamicExtra) {
            addMetadataRow(R.string.detail_dynamic, SensorInfoFormatter.formatBoolean(dynamic, this), "dynamic");
        } else {
            // API level doesn't support it or extra not provided
            addMetadataRow(R.string.detail_dynamic, getString(R.string.not_reported_requires_api, Build.VERSION_CODES.N), "dynamic");
        }

        // Additional info (API 26+)
        boolean hasAdditionalInfoExtra = getIntent().hasExtra(EXTRA_SENSOR_ADDITIONAL_INFO);
        boolean additionalInfo = getIntent().getBooleanExtra(EXTRA_SENSOR_ADDITIONAL_INFO, false);
        if (isSynthetic) {
            addMetadataRow(R.string.detail_additional_info, getString(R.string.value_not_reported), "additionalInfo");
        } else if (hasAdditionalInfoExtra) {
            addMetadataRow(R.string.detail_additional_info, SensorInfoFormatter.formatBoolean(additionalInfo, this), "additionalInfo");
        } else {
            addMetadataRow(R.string.detail_additional_info, getString(R.string.not_reported_requires_api, Build.VERSION_CODES.O), "additionalInfo");
        }
    }

    private void addSyntheticBanner() {
        // Add a banner at the top of the metadata container
        View bannerView = getLayoutInflater().inflate(R.layout.item_metadata_row, metadataContainer, false);
        TextView tvLabel = bannerView.findViewById(R.id.tv_meta_label);
        TextView tvValue = bannerView.findViewById(R.id.tv_meta_value);
        TextView tvMeaning = bannerView.findViewById(R.id.tv_meta_meaning);

        tvLabel.setText(R.string.synthetic_banner);
        tvLabel.setTextColor(getColor(R.color.status_emulator));
        tvValue.setVisibility(View.GONE);
        tvMeaning.setVisibility(View.GONE);

        // Insert at position 0
        metadataContainer.addView(bannerView, 0);
    }

    private void addMetadataRow(int labelResId, @NonNull String value, @Nullable String meaningKey) {
        View rowView = getLayoutInflater().inflate(R.layout.item_metadata_row, metadataContainer, false);
        TextView tvLabel = rowView.findViewById(R.id.tv_meta_label);
        TextView tvValue = rowView.findViewById(R.id.tv_meta_value);
        TextView tvMeaning = rowView.findViewById(R.id.tv_meta_meaning);

        tvLabel.setText(labelResId);
        tvValue.setText(value);

        if (meaningKey != null) {
            String meaning = SensorInfoFormatter.getMeaningForField(meaningKey, this);
            tvMeaning.setText(meaning);
            tvMeaning.setVisibility(View.VISIBLE);
        } else {
            tvMeaning.setVisibility(View.GONE);
        }

        metadataContainer.addView(rowView);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}