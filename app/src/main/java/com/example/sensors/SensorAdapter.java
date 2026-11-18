package com.example.sensors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the sensor inventory list.
 * Works with SensorItem (which wraps real sensors and synthetic test rows).
 * Supports filtering by name, type, or vendor.
 */
public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.SensorViewHolder> {

    private final Context context;
    private final List<SensorItem> allItems = new ArrayList<>();
    private final List<SensorItem> filteredItems = new ArrayList<>();
    private final OnSensorItemClickListener listener;
    private String searchQuery = "";

    public interface OnSensorItemClickListener {
        void onSensorItemClick(SensorItem item);
    }

    public SensorAdapter(@NonNull Context context, @NonNull OnSensorItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(@NonNull List<SensorItem> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter();
    }

    public void setSearchQuery(@NonNull String query) {
        this.searchQuery = query.toLowerCase(Locale.getDefault()).trim();
        applyFilter();
    }

    public int getFilteredCount() {
        return filteredItems.size();
    }

    public int getTotalCount() {
        // Count only real sensors (non-synthetic)
        int count = 0;
        for (SensorItem item : allItems) {
            if (!item.isSynthetic()) count++;
        }
        return count;
    }

    public int getRealItemCount() {
        return getTotalCount();
    }

    private void applyFilter() {
        filteredItems.clear();
        if (searchQuery.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            for (SensorItem item : allItems) {
                String name = item.getName().toLowerCase(Locale.getDefault());
                String type = SensorInfoFormatter.getReadableTypeNameFromItem(item).toLowerCase(Locale.getDefault());
                String vendor = item.getVendor().toLowerCase(Locale.getDefault());
                String stringType = item.getStringType().toLowerCase(Locale.getDefault());

                if (name.contains(searchQuery)
                        || type.contains(searchQuery)
                        || vendor.contains(searchQuery)
                        || stringType.contains(searchQuery)) {
                    filteredItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sensor, parent, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
        SensorItem item = filteredItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    class SensorViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvType;
        private final TextView tvVendor;
        private final com.google.android.material.chip.Chip chipWakeup;
        private final com.google.android.material.chip.Chip chipSynthetic;
        private final View accentStrip;

        SensorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_sensor_name);
            tvType = itemView.findViewById(R.id.tv_sensor_type);
            tvVendor = itemView.findViewById(R.id.tv_sensor_vendor);
            chipWakeup = itemView.findViewById(R.id.chip_wakeup);
            chipSynthetic = itemView.findViewById(R.id.chip_synthetic);
            accentStrip = itemView.findViewById(R.id.accent_strip);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onSensorItemClick(filteredItems.get(pos));
                }
            });
        }

        void bind(@NonNull SensorItem item) {
            // Name (up to 3 lines, ellipsize at end)
            tvName.setText(item.getName());

            // Type: readable name + constant
            String readableType = SensorInfoFormatter.getReadableTypeNameFromItem(item);
            String typeText = String.format("%s (type %d)", readableType, item.getType());
            tvType.setText(typeText);

            // Vendor
            String vendor = item.getVendor();
            if (vendor != null && !vendor.isEmpty()) {
                tvVendor.setText(context.getString(R.string.sensor_vendor_prefix) + vendor);
            } else {
                tvVendor.setText(context.getString(R.string.sensor_vendor_prefix) + context.getString(R.string.value_not_reported));
            }

            // Wake-up chip
            if (item.isWakeUpSensor()) {
                chipWakeup.setText(context.getString(R.string.chip_wakeup));
                chipWakeup.setChipBackgroundColorResource(R.color.status_emulator_bg);
                chipWakeup.setTextColor(ContextCompat.getColor(context, R.color.status_emulator));
                chipWakeup.setVisibility(View.VISIBLE);
            } else {
                chipWakeup.setVisibility(View.GONE);
            }

            // Synthetic chip
            if (item.isSynthetic()) {
                chipSynthetic.setText(context.getString(R.string.synthetic_test_row_label));
                chipSynthetic.setChipBackgroundColorResource(R.color.status_emulator_bg);
                chipSynthetic.setTextColor(ContextCompat.getColor(context, R.color.status_emulator));
                chipSynthetic.setVisibility(View.VISIBLE);
            } else {
                chipSynthetic.setVisibility(View.GONE);
            }

            // Accent strip: amber for synthetic, green for real available
            if (item.isSynthetic()) {
                accentStrip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_emulator));
            } else {
                accentStrip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_available));
            }

            // Content description for accessibility
            itemView.setContentDescription(
                    context.getString(R.string.cd_sensor_row, item.getName(), readableType));
        }
    }
}