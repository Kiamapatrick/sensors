package com.example.sensors;

import android.content.Context;
import android.hardware.Sensor;
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
 * Supports filtering by name, type, or vendor.
 */
public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.SensorViewHolder> {

    private final Context context;
    private final List<Sensor> allSensors = new ArrayList<>();
    private final List<Sensor> filteredSensors = new ArrayList<>();
    private final OnSensorClickListener listener;
    private String searchQuery = "";

    public interface OnSensorClickListener {
        void onSensorClick(Sensor sensor);
    }

    public SensorAdapter(@NonNull Context context, @NonNull OnSensorClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setSensors(@NonNull List<Sensor> sensors) {
        allSensors.clear();
        allSensors.addAll(sensors);
        applyFilter();
    }

    public void setSearchQuery(@NonNull String query) {
        this.searchQuery = query.toLowerCase(Locale.getDefault()).trim();
        applyFilter();
    }

    public int getFilteredCount() {
        return filteredSensors.size();
    }

    public int getTotalCount() {
        return allSensors.size();
    }

    private void applyFilter() {
        filteredSensors.clear();
        if (searchQuery.isEmpty()) {
            filteredSensors.addAll(allSensors);
        } else {
            for (Sensor sensor : allSensors) {
                String name = sensor.getName().toLowerCase(Locale.getDefault());
                String type = SensorInfoFormatter.getReadableTypeName(sensor).toLowerCase(Locale.getDefault());
                String vendor = (sensor.getVendor() != null) ? sensor.getVendor().toLowerCase(Locale.getDefault()) : "";
                String stringType = (sensor.getStringType() != null) ? sensor.getStringType().toLowerCase(Locale.getDefault()) : "";

                if (name.contains(searchQuery)
                        || type.contains(searchQuery)
                        || vendor.contains(searchQuery)
                        || stringType.contains(searchQuery)) {
                    filteredSensors.add(sensor);
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
        Sensor sensor = filteredSensors.get(position);
        holder.bind(sensor);
    }

    @Override
    public int getItemCount() {
        return filteredSensors.size();
    }

    class SensorViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvType;
        private final TextView tvVendor;
        private final com.google.android.material.chip.Chip chipWakeup;
        private final View accentStrip;

        SensorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_sensor_name);
            tvType = itemView.findViewById(R.id.tv_sensor_type);
            tvVendor = itemView.findViewById(R.id.tv_sensor_vendor);
            chipWakeup = itemView.findViewById(R.id.chip_wakeup);
            accentStrip = itemView.findViewById(R.id.accent_strip);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onSensorClick(filteredSensors.get(pos));
                }
            });
        }

        void bind(@NonNull Sensor sensor) {
            // Name (up to 3 lines, ellipsize at end)
            tvName.setText(sensor.getName());

            // Type: readable name + constant
            String readableType = SensorInfoFormatter.getReadableTypeName(sensor);
            String typeText = String.format("%s (type %d)", readableType, sensor.getType());
            tvType.setText(typeText);

            // Vendor
            String vendor = sensor.getVendor();
            if (vendor != null && !vendor.isEmpty()) {
                tvVendor.setText(context.getString(R.string.sensor_vendor_prefix) + vendor);
            } else {
                tvVendor.setText(context.getString(R.string.sensor_vendor_prefix) + context.getString(R.string.value_not_reported));
            }

            // Wake-up chip
            if (sensor.isWakeUpSensor()) {
                chipWakeup.setText(context.getString(R.string.chip_wakeup));
                chipWakeup.setChipBackgroundColorResource(R.color.status_emulator_bg);
                chipWakeup.setTextColor(ContextCompat.getColor(context, R.color.status_emulator));
                chipWakeup.setVisibility(View.VISIBLE);
            } else {
                chipWakeup.setVisibility(View.GONE);
            }

            // Accent strip: green for available (all listed sensors are available)
            accentStrip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_available));

            // Content description for accessibility
            itemView.setContentDescription(
                    context.getString(R.string.cd_sensor_row, sensor.getName(), readableType));
        }
    }
}