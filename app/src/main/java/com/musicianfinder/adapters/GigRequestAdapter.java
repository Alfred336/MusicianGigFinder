package com.musicianfinder.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.musicianfinder.R;
import com.musicianfinder.models.GigRequest;

import java.util.List;

public class GigRequestAdapter extends RecyclerView.Adapter<GigRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onApprove(GigRequest request);
        void onReject(GigRequest request);
    }

    private final List<GigRequest>       requests;
    private final OnRequestActionListener listener;

    public GigRequestAdapter(List<GigRequest> requests, OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gig_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        GigRequest r = requests.get(position);

        h.tvEvent.setText(r.getEventName());
        h.tvDate.setText(r.getEventDate() + " " + (r.getEventTime() != null ? r.getEventTime() : ""));
        h.tvLocation.setText(r.getEventLocation());
        h.tvRequester.setText("By: " + r.getRequesterName() + " (" + r.getRequesterPhone() + ")");
        h.tvAmount.setText(r.getAmount() + " TZS");

        String status = r.getStatus();
        h.tvStatus.setText(status);
        int color = "Approved".equals(status) ? Color.parseColor("#4CAF50") :
                    "Rejected".equals(status) ? Color.parseColor("#F44336") :
                    Color.parseColor("#FF9800");
        h.tvStatus.setTextColor(color);

        h.tvPaymentStatus.setText("Payment: " + r.getPaymentStatus());

        if (r.getAdminNotes() != null && !r.getAdminNotes().isEmpty()) {
            h.tvNotes.setVisibility(View.VISIBLE);
            h.tvNotes.setText("Note: " + r.getAdminNotes());
        } else {
            h.tvNotes.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return requests.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEvent, tvDate, tvLocation, tvRequester, tvAmount, tvStatus, tvPaymentStatus, tvNotes;
        ViewHolder(View v) {
            super(v);
            tvEvent        = v.findViewById(R.id.tvEvent);
            tvDate         = v.findViewById(R.id.tvDate);
            tvLocation     = v.findViewById(R.id.tvLocation);
            tvRequester    = v.findViewById(R.id.tvRequester);
            tvAmount       = v.findViewById(R.id.tvAmount);
            tvStatus       = v.findViewById(R.id.tvStatus);
            tvPaymentStatus= v.findViewById(R.id.tvPaymentStatus);
            tvNotes        = v.findViewById(R.id.tvNotes);
        }
    }
}
