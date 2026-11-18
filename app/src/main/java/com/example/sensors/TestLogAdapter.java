package com.example.sensors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * RecyclerView adapter for the test log entries.
 */
public class TestLogAdapter extends RecyclerView.Adapter<TestLogAdapter.LogViewHolder> {

    private final Context context;
    private List<TestLog.Entry> entries;

    public TestLogAdapter(@NonNull Context context, @NonNull List<TestLog.Entry> entries) {
        this.context = context;
        this.entries = entries;
    }

    public void setEntries(@NonNull List<TestLog.Entry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_test_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        TestLog.Entry entry = entries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvId;
        private final TextView tvTimestamp;
        private final TextView tvDescription;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tv_log_id);
            tvTimestamp = itemView.findViewById(R.id.tv_log_timestamp);
            tvDescription = itemView.findViewById(R.id.tv_log_description);
        }

        void bind(@NonNull TestLog.Entry entry) {
            tvId.setText(entry.id);
            tvTimestamp.setText(entry.timestamp);
            tvDescription.setText(entry.description);
        }
    }
}