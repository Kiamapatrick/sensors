package com.example.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CompareActivity: Three-sensor auto-pick comparison + two-sensor user comparison + MavunoLink idea (R2, R3).
 * Auto-picks up to 3 sensors in preference order. Shows side-by-side cards.
 * User can select two sensor types from spinners for detailed row-by-row comparison.
 * Includes MavunoLink justification section.
 */
public class CompareActivity extends AppCompatActivity {

    private static final String TAG = "CompareActivity";

    // Preference order for auto-pick
    private static final int[] PREFERRED_TYPES = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE
    };

    // Spinner sensor types for user comparison
    private static final int[] SPINNER_TYPES = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_HEART_RATE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_ROTATION_VECTOR
    };

    private static final String[] SPINNER_LABELS = {
            "Accelerometer",
            "Light",
            "Proximity",
            "Gyroscope",
            "Magnetic Field",
            "Pressure",
            "Ambient Temperature",
            "Relative Humidity",
            "Step Counter",
            "Heart Rate",
            "Gravity",
            "Rotation Vector"
    };

    private SensorManager sensorManager;
    private Map<Integer, Sensor> availableSensors = new HashMap<>();
    private LinearLayout autoCompareRow;
    private TextView tvAutoPickNote;
    private MaterialAutoCompleteTextView spinnerFirst;
    private MaterialAutoCompleteTextView spinnerSecond;
    private TextView tvInterpretation;
    private HorizontalScrollView compareTableScroll;
    private LinearLayout compareTableRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_compare);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.compare_container), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.compare_title);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        autoCompareRow = findViewById(R.id.auto_compare_row);
        tvAutoPickNote = findViewById(R.id.tv_auto_pick_note);
        spinnerFirst = findViewById(R.id.spinner_compare_first);
        spinnerSecond = findViewById(R.id.spinner_compare_second);
        tvInterpretation = findViewById(R.id.tv_compare_interpretation);
        compareTableScroll = findViewById(R.id.compare_table_scroll);
        compareTableRow = findViewById(R.id.compare_table_row);

        loadAvailableSensors();
        setupAutoComparison();
        setupSpinners();
        setupMavunoLinkSection();

        TestLog.getInstance().logComparisonShown();
    }

    private void loadAvailableSensors() {
        availableSensors.clear();
        if (sensorManager != null) {
            List<Sensor> all = sensorManager.getSensorList(Sensor.TYPE_ALL);
            if (all != null) {
                for (Sensor s : all) {
                    // Keep only the first (default) sensor for each type
                    if (!availableSensors.containsKey(s.getType())) {
                        availableSensors.put(s.getType(), s);
                    }
                }
            }
        }
    }

    private void setupAutoComparison() {
        autoCompareRow.removeAllViews();

        List<Sensor> picked = new ArrayList<>();
        for (int type : PREFERRED_TYPES) {
            Sensor s = availableSensors.get(type);
            if (s != null) {
                picked.add(s);
                if (picked.size() >= 3) break;
            }
        }

        if (picked.isEmpty()) {
            tvAutoPickNote.setText(getString(R.string.compare_auto_pick_fewer, 0));
            tvAutoPickNote.setVisibility(View.VISIBLE);
            return;
        }

        if (picked.size() < 3) {
            tvAutoPickNote.setText(getString(R.string.compare_auto_pick_fewer, picked.size()));
            tvAutoPickNote.setVisibility(View.VISIBLE);
        } else {
            tvAutoPickNote.setVisibility(View.GONE);
        }

        for (Sensor sensor : picked) {
            View cardView = getLayoutInflater().inflate(R.layout.item_compare_sensor_card, autoCompareRow, false);
            populateCompareCard(cardView, sensor);
            autoCompareRow.addView(cardView);
        }
    }

    private void populateCompareCard(View cardView, Sensor sensor) {
        TextView tvName = cardView.findViewById(R.id.tv_compare_name);
        TextView tvType = cardView.findViewById(R.id.tv_compare_type);
        TextView tvVendor = cardView.findViewById(R.id.tv_compare_vendor);
        TextView tvRange = cardView.findViewById(R.id.tv_compare_range);
        TextView tvResolution = cardView.findViewById(R.id.tv_compare_resolution);
        TextView tvPower = cardView.findViewById(R.id.tv_compare_power);
        TextView tvMinDelay = cardView.findViewById(R.id.tv_compare_min_delay);

        tvName.setText(sensor.getName());
        String readableType = SensorInfoFormatter.getReadableTypeName(sensor);
        tvType.setText(String.format(Locale.getDefault(), "%s (type %d)", readableType, sensor.getType()));
        String vendor = sensor.getVendor();
        tvVendor.setText(getString(R.string.sensor_vendor_prefix) + (vendor != null && !vendor.isEmpty() ? vendor : getString(R.string.value_not_reported)));

        tvRange.setText(SensorInfoFormatter.formatRangeOrResolution(sensor.getMaximumRange(), sensor.getType(), this));
        tvResolution.setText(SensorInfoFormatter.formatRangeOrResolution(sensor.getResolution(), sensor.getType(), this));
        tvPower.setText(SensorInfoFormatter.formatPower(sensor.getPower(), this));
        tvMinDelay.setText(SensorInfoFormatter.formatMinDelay(sensor.getMinDelay(), this));
    }

    private void setupSpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, SPINNER_LABELS);
        spinnerFirst.setAdapter(adapter);
        spinnerSecond.setAdapter(adapter);

        // Default: Accelerometer vs Light
        spinnerFirst.setText(SPINNER_LABELS[0], false);
        spinnerSecond.setText(SPINNER_LABELS[1], false);

        AdapterView.OnItemClickListener listener = (parent, view, position, id) -> {
            updateComparison();
        };
        spinnerFirst.setOnItemClickListener(listener);
        spinnerSecond.setOnItemClickListener(listener);

        // Initial comparison
        updateComparison();
    }

    private void updateComparison() {
        String firstLabel = spinnerFirst.getText().toString();
        String secondLabel = spinnerSecond.getText().toString();

        if (firstLabel.equals(secondLabel)) {
            tvInterpretation.setText(R.string.compare_same_type);
            tvInterpretation.setVisibility(View.VISIBLE);
            compareTableScroll.setVisibility(View.GONE);
            return;
        }

        int firstType = findTypeForLabel(firstLabel);
        int secondType = findTypeForLabel(secondLabel);

        Sensor firstSensor = availableSensors.get(firstType);
        Sensor secondSensor = availableSensors.get(secondType);

        if (firstSensor == null || secondSensor == null) {
            tvInterpretation.setText(R.string.compare_missing_one);
            tvInterpretation.setVisibility(View.VISIBLE);
            compareTableScroll.setVisibility(View.GONE);
            return;
        }

        // Both available - show comparison table
        tvInterpretation.setVisibility(View.GONE);
        compareTableScroll.setVisibility(View.VISIBLE);
        compareTableRow.removeAllViews();

        // Add first sensor column
        View col1 = getLayoutInflater().inflate(R.layout.item_compare_column, compareTableRow, false);
        populateCompareColumn(col1, firstSensor, true);
        compareTableRow.addView(col1);

        // Add second sensor column
        View col2 = getLayoutInflater().inflate(R.layout.item_compare_column, compareTableRow, false);
        populateCompareColumn(col2, secondSensor, false);
        compareTableRow.addView(col2);

        // Generate interpretation
        String interpretation = generateInterpretation(firstSensor, secondSensor);
        tvInterpretation.setText(interpretation);
        tvInterpretation.setVisibility(View.VISIBLE);
    }

    private int findTypeForLabel(String label) {
        for (int i = 0; i < SPINNER_LABELS.length; i++) {
            if (SPINNER_LABELS[i].equals(label)) {
                return SPINNER_TYPES[i];
            }
        }
        return -1;
    }

    private void populateCompareColumn(View columnView, Sensor sensor, boolean isFirst) {
        TextView tvName = columnView.findViewById(R.id.tv_col_name);
        TextView tvType = columnView.findViewById(R.id.tv_col_type);
        TextView tvVendor = columnView.findViewById(R.id.tv_col_vendor);
        com.google.android.material.chip.Chip chipAvailable = columnView.findViewById(R.id.chip_col_available);
        LinearLayout metadataContainer = columnView.findViewById(R.id.col_metadata_container);

        tvName.setText(sensor.getName());
        String readableType = SensorInfoFormatter.getReadableTypeName(sensor);
        tvType.setText(String.format(Locale.getDefault(), "%s (type %d)", readableType, sensor.getType()));
        String vendor = sensor.getVendor();
        tvVendor.setText(getString(R.string.sensor_vendor_prefix) + (vendor != null && !vendor.isEmpty() ? vendor : getString(R.string.value_not_reported)));

        chipAvailable.setText(R.string.value_yes);
        chipAvailable.setChipBackgroundColorResource(R.color.status_available_bg);
        chipAvailable.setTextColor(getColor(R.color.status_available));

        // Add metadata rows
        metadataContainer.removeAllViews();
        addCompareMetadataRow(metadataContainer, R.string.detail_max_range,
                SensorInfoFormatter.formatRangeOrResolution(sensor.getMaximumRange(), sensor.getType(), this));
        addCompareMetadataRow(metadataContainer, R.string.detail_resolution,
                SensorInfoFormatter.formatRangeOrResolution(sensor.getResolution(), sensor.getType(), this));
        addCompareMetadataRow(metadataContainer, R.string.detail_power,
                SensorInfoFormatter.formatPower(sensor.getPower(), this));
        addCompareMetadataRow(metadataContainer, R.string.detail_min_delay,
                SensorInfoFormatter.formatMinDelay(sensor.getMinDelay(), this));
        addCompareMetadataRow(metadataContainer, R.string.detail_max_delay,
                SensorInfoFormatter.formatMaxDelay(sensor.getMaxDelay(), this));
        addCompareMetadataRow(metadataContainer, R.string.detail_reporting_mode,
                SensorInfoFormatter.getReportingModeString(sensor.getReportingMode(), this));
        addCompareMetadataRow(metadataContainer, R.string.detail_wakeup,
                SensorInfoFormatter.formatBoolean(sensor.isWakeUpSensor(), this));
        // API 29+ fields
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addCompareMetadataRow(metadataContainer, R.string.detail_dynamic,
                    SensorInfoFormatter.formatBoolean(sensor.isDynamicSensor(), this));
            addCompareMetadataRow(metadataContainer, R.string.detail_additional_info,
                    SensorInfoFormatter.formatBoolean(sensor.isAdditionalInfoSupported(), this));
        }
    }

    private void addCompareMetadataRow(LinearLayout container, int labelResId, String value) {
        View row = getLayoutInflater().inflate(R.layout.item_compare_metadata_row, container, false);
        TextView tvLabel = row.findViewById(R.id.tv_cmp_meta_label);
        TextView tvValue = row.findViewById(R.id.tv_cmp_meta_value);
        tvLabel.setText(labelResId);
        tvValue.setText(value);
        container.addView(row);
    }

    private String generateInterpretation(Sensor s1, Sensor s2) {
        StringBuilder sb = new StringBuilder();
        String name1 = SensorInfoFormatter.getReadableTypeName(s1);
        String name2 = SensorInfoFormatter.getReadableTypeName(s2);

        // Resolution comparison
        float res1 = s1.getResolution();
        float res2 = s2.getResolution();
        if (res1 > 0 && res2 > 0) {
            if (res1 < res2) {
                sb.append(String.format("%s has finer resolution (%.6g vs %.6g). ", name1, res1, res2));
            } else if (res2 < res1) {
                sb.append(String.format("%s has finer resolution (%.6g vs %.6g). ", name2, res2, res1));
            } else {
                sb.append("Both have similar resolution. ");
            }
        } else {
            sb.append("Resolution not reported for one or both sensors. ");
        }

        // Power comparison
        float p1 = s1.getPower();
        float p2 = s2.getPower();
        if (p1 > 0 && p2 > 0) {
            if (p1 < p2) {
                sb.append(String.format("%s uses less power (%.3f vs %.3f mA). ", name1, p1, p2));
            } else if (p2 < p1) {
                sb.append(String.format("%s uses less power (%.3f vs %.3f mA). ", name2, p2, p1));
            } else {
                sb.append("Similar power consumption. ");
            }
        } else {
            sb.append("Power not reported for one or both sensors. ");
        }

        // Min delay / reporting mode
        int d1 = s1.getMinDelay();
        int d2 = s2.getMinDelay();
        if (d1 == 0 && d2 > 0) {
            sb.append(String.format("%s is event-driven (on-change); %s supports fixed-rate sampling. ", name1, name2));
        } else if (d2 == 0 && d1 > 0) {
            sb.append(String.format("%s is event-driven (on-change); %s supports fixed-rate sampling. ", name2, name1));
        } else if (d1 > 0 && d2 > 0) {
            double hz1 = 1_000_000.0 / d1;
            double hz2 = 1_000_000.0 / d2;
            sb.append(String.format("Max rates: %s %.0f Hz, %s %.0f Hz. ", name1, hz1, name2, hz2));
        } else {
            sb.append("Sampling rates not fully reported. ");
        }

        // Wake-up
        if (s1.isWakeUpSensor() && !s2.isWakeUpSensor()) {
            sb.append(String.format("%s can wake the CPU; %s cannot. ", name1, name2));
        } else if (s2.isWakeUpSensor() && !s1.isWakeUpSensor()) {
            sb.append(String.format("%s can wake the CPU; %s cannot. ", name2, name1));
        }

        return sb.toString().trim();
    }

    private void setupMavunoLinkSection() {
        TextView tvMavunoLink = findViewById(R.id.tv_mavunolink_idea);
        tvMavunoLink.setText(R.string.mavunolink_idea);
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