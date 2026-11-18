package com.example.sensors;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity: Sensor inventory screen (R1, C1, C4).
 * Shows device card with environment detection, total sensor count,
 * and a searchable/filterable RecyclerView of all sensors.
 * Toolbar menu navigates to Compare, Check, Analysis, and Test Log screens.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String KEY_SCROLL_POSITION = "scroll_position";
    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_EMPTY_SIMULATED = "empty_simulated";
    private static final String KEY_SYNTHETIC_ROW = "synthetic_row";

    // UI
    private TextView tvManufacturer;
    private TextView tvModel;
    private TextView tvAndroidVersion;
    private TextView tvHardware;
    private com.google.android.material.chip.Chip chipEnvironment;
    private TextView tvTotalSensors;
    private TextView tvEmulatorCaution;
    private TextView tvSyntheticCaption;
    private TextInputEditText etSearch;
    private TextView tvShowingCount;
    private RecyclerView rvSensors;
    private LinearLayout emptyStateContainer;
    private TextView tvEmptyTotal;
    private SensorAdapter sensorAdapter;
    private SensorManager sensorManager;
    private List<Sensor> allSensors = new ArrayList<>();
    private List<SensorItem> allItems = new ArrayList<>();
    private boolean emptySimulated = false;
    private boolean syntheticRowVisible = false;

    // State restoration
    private int savedScrollPosition = 0;
    private String savedSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Long-press toolbar title to toggle empty inventory simulation
        toolbar.setOnLongClickListener(v -> {
            toggleEmptySimulation();
            return true;
        });

        initViews();
        setupRecyclerView();
        setupSearch();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Restore state BEFORE loading sensor data
        if (savedInstanceState != null) {
            savedScrollPosition = savedInstanceState.getInt(KEY_SCROLL_POSITION, 0);
            savedSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            emptySimulated = savedInstanceState.getBoolean(KEY_EMPTY_SIMULATED, false);
            syntheticRowVisible = savedInstanceState.getBoolean(KEY_SYNTHETIC_ROW, false);
            if (!savedSearchQuery.isEmpty()) {
                etSearch.setText(savedSearchQuery);
                etSearch.setSelection(savedSearchQuery.length());
            }
        }

        // Load sensor data and populate UI
        loadSensorData();
        refreshUi();

        // Log rotation if this is a recreation
        if (savedInstanceState != null) {
            TestLog.getInstance().logRotationOccurred(this);
        } else {
            // Fresh launch: log inventory and environment
            TestLog.getInstance().logInventoryLoaded(allSensors.size());
            EnvironmentDetector.Result envResult = EnvironmentDetector.detect();
            TestLog.getInstance().logEnvironmentIdentified(envResult.label);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save RecyclerView scroll position
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvSensors.getLayoutManager();
        if (layoutManager != null) {
            outState.putInt(KEY_SCROLL_POSITION, layoutManager.findFirstVisibleItemPosition());
        }
        outState.putString(KEY_SEARCH_QUERY, etSearch.getText().toString());
        outState.putBoolean(KEY_EMPTY_SIMULATED, emptySimulated);
        outState.putBoolean(KEY_SYNTHETIC_ROW, syntheticRowVisible);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restore scroll position after layout
        if (savedScrollPosition > 0) {
            rvSensors.post(() -> {
                LinearLayoutManager layoutManager = (LinearLayoutManager) rvSensors.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(savedScrollPosition, 0);
                }
                savedScrollPosition = 0;
            });
        }
    }

    private void initViews() {
        tvManufacturer = findViewById(R.id.tv_manufacturer);
        tvModel = findViewById(R.id.tv_model);
        tvAndroidVersion = findViewById(R.id.tv_android_version);
        tvHardware = findViewById(R.id.tv_hardware);
        chipEnvironment = findViewById(R.id.chip_environment);
        tvTotalSensors = findViewById(R.id.tv_total_sensors);
        tvEmulatorCaution = findViewById(R.id.tv_emulator_caution);
        tvSyntheticCaption = findViewById(R.id.tv_synthetic_caption);
        etSearch = findViewById(R.id.et_search);
        tvShowingCount = findViewById(R.id.tv_showing_count);
        rvSensors = findViewById(R.id.rv_sensors);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        tvEmptyTotal = findViewById(R.id.tv_empty_total);

        // Set up long-press on total sensors text for synthetic test row
        tvTotalSensors.setOnLongClickListener(v -> {
            toggleSyntheticRow();
            return true;
        });
    }

    private void setupRecyclerView() {
        sensorAdapter = new SensorAdapter(this, item -> {
            // Open SensorDetailActivity
            Intent intent = new Intent(this, SensorDetailActivity.class);
            if (item.isSynthetic()) {
                // Pass synthetic data
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_NAME, item.getName());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_TYPE, item.getType());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_VENDOR, item.getVendor());
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_VERSION, -1);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_MAX_RANGE, -1f);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_RESOLUTION, -1f);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_MIN_DELAY, -1);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_MAX_DELAY, -1);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_POWER, -1f);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_REPORTING_MODE, -1);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_WAKEUP, false);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_FIFO_MAX, -1);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_FIFO_RESERVED, -1);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_STRING_TYPE, "");
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_DYNAMIC, false);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_ADDITIONAL_INFO, false);
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_SYNTHETIC, true);
            } else {
                Sensor sensor = item.getSensor();
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
                // Dynamic sensor and additional info (guard by actual API level)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_DYNAMIC, sensor.isDynamicSensor());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_ADDITIONAL_INFO, sensor.isAdditionalInfoSupported());
                }
                intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_SYNTHETIC, false);
            }
            startActivity(intent);
            TestLog.getInstance().logDetailOpened(item.getName());
        });
        rvSensors.setLayoutManager(new LinearLayoutManager(this));
        rvSensors.setAdapter(sensorAdapter);
        rvSensors.setNestedScrollingEnabled(false);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sensorAdapter.setSearchQuery(s.toString());
                updateShowingCount();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSensorData() {
        if (sensorManager == null) {
            Log.w(TAG, "SensorManager is null");
            allSensors.clear();
            allItems.clear();
            showEmptyState(true);
            return;
        }

        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        if (sensorList == null) {
            sensorList = new ArrayList<>();
        }

        allSensors.clear();
        allSensors.addAll(sensorList);

        // Sort by type then name
        Collections.sort(allSensors, new Comparator<Sensor>() {
            @Override
            public int compare(Sensor s1, Sensor s2) {
                int typeCompare = Integer.compare(s1.getType(), s2.getType());
                if (typeCompare != 0) return typeCompare;
                return s1.getName().compareToIgnoreCase(s2.getName());
            }
        });

        // Build SensorItem list from real sensors
        allItems.clear();
        for (Sensor sensor : allSensors) {
            allItems.add(SensorItem.fromSensor(sensor));
        }
    }

    private void refreshUi() {
        // Apply empty simulation
        if (emptySimulated) {
            showEmptyState(true);
            sensorAdapter.setItems(new ArrayList<>());
        } else {
            showEmptyState(allSensors.isEmpty());
            // Build display list: synthetic row at top if visible, then real sensors
            List<SensorItem> displayItems = new ArrayList<>();
            if (syntheticRowVisible) {
                displayItems.add(SensorItem.syntheticLongName());
            }
            displayItems.addAll(allItems);
            sensorAdapter.setItems(displayItems);
        }

        // Update device card (total count, synthetic caption)
        populateDeviceCard();
        updateShowingCount();
    }

    private void populateDeviceCard() {
        // Device info
        tvManufacturer.setText(Build.MANUFACTURER);
        tvModel.setText(Build.MODEL);
        tvAndroidVersion.setText(String.format(Locale.getDefault(), "Android %s (API %d)",
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        tvHardware.setText(Build.HARDWARE);

        // Environment detection
        EnvironmentDetector.Result envResult = EnvironmentDetector.detect();
        chipEnvironment.setText(envResult.label);
        if (envResult.isEmulator) {
            chipEnvironment.setChipBackgroundColorResource(R.color.status_emulator_bg);
            chipEnvironment.setTextColor(getColor(R.color.status_emulator));
            tvEmulatorCaution.setVisibility(View.VISIBLE);
            tvEmulatorCaution.setText(envResult.caution);
        } else {
            chipEnvironment.setChipBackgroundColorResource(R.color.status_available_bg);
            chipEnvironment.setTextColor(getColor(R.color.status_available));
            tvEmulatorCaution.setVisibility(View.GONE);
        }

        // Total sensors count (real sensors only)
        int realCount = allSensors.size();
        tvTotalSensors.setText(String.format(Locale.getDefault(), "%d", realCount));
        tvEmptyTotal.setText(getString(R.string.total_sensors_format, realCount));

        // Synthetic row caption
        if (syntheticRowVisible && !emptySimulated) {
            tvSyntheticCaption.setText(R.string.synthetic_caption);
            tvSyntheticCaption.setVisibility(View.VISIBLE);
        } else {
            tvSyntheticCaption.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean empty) {
        if (empty) {
            rvSensors.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            rvSensors.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private void updateShowingCount() {
        int showing = sensorAdapter.getFilteredCount();
        int total = sensorAdapter.getTotalCount(); // real sensors only
        tvShowingCount.setText(getString(R.string.showing_count_format, showing, total));
    }

    private void toggleEmptySimulation() {
        emptySimulated = !emptySimulated;
        refreshUi();
        Snackbar.make(findViewById(R.id.main_coordinator),
                emptySimulated ? R.string.empty_inventory_simulated : R.string.empty_inventory_restored,
                Snackbar.LENGTH_SHORT).show();
        TestLog.getInstance().logEmptyInventorySimulated(emptySimulated);
    }

    private void toggleSyntheticRow() {
        if (emptySimulated) {
            // Can't show synthetic row when empty simulation is active
            Snackbar.make(findViewById(R.id.main_coordinator),
                    "Disable empty simulation first", Snackbar.LENGTH_SHORT).show();
            return;
        }
        syntheticRowVisible = !syntheticRowVisible;
        refreshUi();
        if (syntheticRowVisible) {
            // Log E5-08 only when synthetic row is added
            SensorItem synthetic = SensorItem.syntheticLongName();
            TestLog.getInstance().logLongNameVerified(synthetic.getName().length());
            Snackbar.make(findViewById(R.id.main_coordinator),
                    R.string.synthetic_row_added, Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(findViewById(R.id.main_coordinator),
                    R.string.synthetic_row_removed, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_compare_sensors) {
            startActivity(new Intent(this, CompareActivity.class));
            return true;
        } else if (id == R.id.menu_check_sensor) {
            startActivity(new Intent(this, CheckSensorActivity.class));
            return true;
        } else if (id == R.id.menu_analysis) {
            startActivity(new Intent(this, AnalysisActivity.class));
            return true;
        } else if (id == R.id.menu_test_log) {
            startActivity(new Intent(this, TestLogActivity.class));
            return true;
        } else if (id == R.id.menu_refresh) {
            // Refresh logs E5-01 and E5-02 on explicit refresh
            loadSensorData();
            refreshUi();
            TestLog.getInstance().logInventoryLoaded(allSensors.size());
            EnvironmentDetector.Result envResult = EnvironmentDetector.detect();
            TestLog.getInstance().logEnvironmentIdentified(envResult.label);
            return true;
        } else if (id == R.id.menu_test_synthetic_row) {
            toggleSyntheticRow();
            return true;
        } else if (id == R.id.menu_test_empty_inventory) {
            toggleEmptySimulation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}