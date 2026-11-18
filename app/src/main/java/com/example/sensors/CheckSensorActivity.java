package com.example.sensors;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.util.Locale;

/**
 * CheckSensorActivity: Checks availability of a specific sensor type (R4, C3).
 * Spinner with common sensor types. "Check" button calls getDefaultSensor(type).
 * Shows green card if available, red card if not. Logs each check to TestLog.
 */
public class CheckSensorActivity extends AppCompatActivity {

    private static final String TAG = "CheckSensorActivity";

    // Spinner sensor types
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

    private static final int[] SPINNER_LABEL_RES = {
            R.string.sensor_type_accelerometer,
            R.string.sensor_type_light,
            R.string.sensor_type_proximity,
            R.string.sensor_type_gyroscope,
            R.string.sensor_type_magnetic_field,
            R.string.sensor_type_pressure,
            R.string.sensor_type_ambient_temperature,
            R.string.sensor_type_relative_humidity,
            R.string.sensor_type_step_counter,
            R.string.sensor_type_heart_rate,
            R.string.sensor_type_gravity,
            R.string.sensor_type_rotation_vector
    };

    private SensorManager sensorManager;
    private MaterialAutoCompleteTextView spinnerSensorType;
    private MaterialButton btnCheck;
    private MaterialCardView resultCard;
    private LinearLayout resultContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_check_sensor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator), (v, insets) -> {
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
            getSupportActionBar().setTitle(R.string.check_sensor_title);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        spinnerSensorType = findViewById(R.id.spinner_sensor_type);
        btnCheck = findViewById(R.id.btn_check);
        resultCard = findViewById(R.id.result_card);
        resultContent = findViewById(R.id.result_content);

        setupSpinner();
        btnCheck.setOnClickListener(v -> checkSensor());
    }

    private void setupSpinner() {
        String[] labels = new String[SPINNER_LABEL_RES.length];
        for (int i = 0; i < SPINNER_LABEL_RES.length; i++) {
            labels[i] = getString(SPINNER_LABEL_RES[i]);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, labels);
        spinnerSensorType.setAdapter(adapter);
        spinnerSensorType.setText(labels[0], false); // Default to Accelerometer

        spinnerSensorType.setOnItemClickListener((parent, view, position, id) -> {
            // Clear previous result when selection changes
            resultCard.setVisibility(View.GONE);
        });
    }

    private void checkSensor() {
        String selectedLabel = spinnerSensorType.getText().toString();
        int sensorType = findTypeForLabel(selectedLabel);

        if (sensorType == -1) {
            return;
        }

        Sensor sensor = null;
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(sensorType);
        }

        boolean available = sensor != null;
        showResult(available, sensor, sensorType, selectedLabel);
        TestLog.getInstance().logSensorChecked(sensorType, available);
    }

    private int findTypeForLabel(String label) {
        for (int i = 0; i < SPINNER_LABEL_RES.length; i++) {
            if (getString(SPINNER_LABEL_RES[i]).equals(label)) {
                return SPINNER_TYPES[i];
            }
        }
        return -1;
    }

    private void showResult(boolean available, Sensor sensor, int sensorType, String label) {
        resultContent.removeAllViews();
        resultCard.setVisibility(View.VISIBLE);

        if (available) {
            // Green card - Available
            resultCard.setStrokeColor(getColor(R.color.status_available));
            resultCard.setStrokeWidth(2);

            // Header
            TextView tvHeader = new TextView(this);
            tvHeader.setText(R.string.check_result_available);
            tvHeader.setTextAppearance(R.style.TextAppearance_SensorLab_Heading);
            tvHeader.setTextColor(getColor(R.color.status_available));
            resultContent.addView(tvHeader);

            // Sensor summary
            View summaryView = getLayoutInflater().inflate(R.layout.item_metadata_row, resultContent, false);
            TextView tvName = summaryView.findViewById(R.id.tv_meta_label);
            TextView tvValue = summaryView.findViewById(R.id.tv_meta_value);
            TextView tvMeaning = summaryView.findViewById(R.id.tv_meta_meaning);

            tvName.setText(sensor.getName());
            String readableType = SensorInfoFormatter.getReadableTypeName(sensor);
            tvValue.setText(String.format(Locale.getDefault(), "%s (type %d) • Vendor: %s",
                    readableType, sensor.getType(),
                    sensor.getVendor() != null ? sensor.getVendor() : getString(R.string.value_not_reported)));
            tvMeaning.setVisibility(View.GONE);
            resultContent.addView(summaryView);

            // Open details button
            MaterialButton btnDetails = new MaterialButton(this);
            btnDetails.setText(R.string.check_open_details);
            btnDetails.setOnClickListener(v -> {
                Intent intent = new Intent(this, SensorDetailActivity.class);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_NAME, sensor.getName());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_TYPE, sensor.getType());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_VENDOR, sensor.getVendor());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_VERSION, sensor.getVersion());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_MAX_RANGE, sensor.getMaximumRange());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_RESOLUTION, sensor.getResolution());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_MIN_DELAY, sensor.getMinDelay());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_MAX_DELAY, sensor.getMaxDelay());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_POWER, sensor.getPower());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_REPORTING_MODE, sensor.getReportingMode());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_WAKEUP, sensor.isWakeUpSensor());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_FIFO_MAX, sensor.getFifoMaxEventCount());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_FIFO_RESERVED, sensor.getFifoReservedEventCount());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_STRING_TYPE, sensor.getStringType());
                // Guard each by its actual minimum API level
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_DYNAMIC, sensor.isDynamicSensor());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_ADDITIONAL_INFO, sensor.isAdditionalInfoSupported());
                }
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_SYNTHETIC, false);
                startActivity(intent);
                TestLog.getInstance().logDetailOpened(sensor.getName());
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, getResources().getDimensionPixelSize(R.dimen.spacing_md), 0, 0);
            btnDetails.setLayoutParams(params);
            resultContent.addView(btnDetails);

            // Accessibility
            resultCard.setContentDescription(getString(R.string.cd_check_result_available, label));

        } else {
            // Red card - Not available
            resultCard.setStrokeColor(getColor(R.color.status_unavailable));
            resultCard.setStrokeWidth(2);

            TextView tvHeader = new TextView(this);
            tvHeader.setText(R.string.check_result_unavailable);
            tvHeader.setTextAppearance(R.style.TextAppearance_SensorLab_Heading);
            tvHeader.setTextColor(getColor(R.color.status_unavailable));
            resultContent.addView(tvHeader);

            // Show which type was checked
            TextView tvType = new TextView(this);
            tvType.setText(String.format(Locale.getDefault(), "Sensor type: %s (type %d)", label, sensorType));
            tvType.setTextAppearance(R.style.TextAppearance_SensorLab_Value);
            tvType.setTextColor(getColor(R.color.text_secondary));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, getResources().getDimensionPixelSize(R.dimen.spacing_sm), 0, 0);
            tvType.setLayoutParams(params);
            resultContent.addView(tvType);

            // Accessibility
            resultCard.setContentDescription(getString(R.string.cd_check_result_unavailable, label));
        }
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