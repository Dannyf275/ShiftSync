package com.example.shiftsync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shiftsync.models.ShiftRequestItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * אדפטר המציג את רשימת הבקשות הממתינות לאישור המנהל.
 */
public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    private List<ShiftRequestItem> requests;
    private OnRequestActionListener listener;

    // ממשק להעברת אירועי לחיצה (אשר/דחה) ל-Activity
    public interface OnRequestActionListener {
        void onApprove(ShiftRequestItem item);
        void onDeny(ShiftRequestItem item);
    }

    public RequestsAdapter(List<ShiftRequestItem> requests, OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShiftRequestItem item = requests.get(position);

        // הצגת פרטי הזמן
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

        String timeStr = sdfDate.format(item.getShift().getStartTime()) + " | " +
                sdfTime.format(item.getShift().getStartTime()) + " - " +
                sdfTime.format(item.getShift().getEndTime());

        holder.tvShiftDetails.setText(timeStr);
        holder.tvEmployeeName.setText(item.getUserName());

        // הגדרת הכפתורים
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(item));
        holder.btnDeny.setOnClickListener(v -> listener.onDeny(item));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShiftDetails, tvEmployeeName;
        Button btnApprove, btnDeny;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShiftDetails = itemView.findViewById(R.id.tvShiftDetails);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDeny = itemView.findViewById(R.id.btnDeny);
        }
    }
}