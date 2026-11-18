package com.example.sensors;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;

/**
 * TestLogActivity: Evidence log (E5-01 through E5-08).
 * Lists log entries in a RecyclerView. "Copy log" menu action copies to clipboard.
 * Log persists across rotation (static singleton). Note shown that it resets on process death.
 */
public class TestLogActivity extends AppCompatActivity {

    private RecyclerView rvLog;
    private TextView tvEmpty;
    private TestLogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_test_log);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.test_log_title);
        }

        rvLog = findViewById(R.id.rv_test_log);
        tvEmpty = findViewById(R.id.tv_log_empty);

        adapter = new TestLogAdapter(this, TestLog.getInstance().getEntries());
        rvLog.setLayoutManager(new LinearLayoutManager(this));
        rvLog.setAdapter(adapter);

        updateEmptyState();

        TestLog.getInstance().logRotationOccurred();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh log entries in case new ones were added
        adapter.setEntries(TestLog.getInstance().getEntries());
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            rvLog.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvLog.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_test_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_copy_log) {
            copyLogToClipboard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyLogToClipboard() {
        String logText = TestLog.getInstance().getLogText();
        if (logText.isEmpty()) {
            Snackbar.make(rvLog, R.string.test_log_empty, Snackbar.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Sensor Lab Test Log", logText);
        clipboard.setPrimaryClip(clip);
        Snackbar.make(rvLog, R.string.test_log_copied, Snackbar.LENGTH_SHORT).show();
    }
}