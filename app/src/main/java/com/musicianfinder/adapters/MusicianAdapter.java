package com.musicianfinder.adapters;

import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.musicianfinder.R;
import com.musicianfinder.models.Musician;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MusicianAdapter extends RecyclerView.Adapter<MusicianAdapter.ViewHolder> {

    public interface OnMusicianClickListener {
        void onMusicianClick(Musician musician);
    }

    private final List<Musician>          musicians;
    private final OnMusicianClickListener listener;
    private static final NumberFormat     TZS = NumberFormat.getNumberInstance(Locale.US);

    public MusicianAdapter(List<Musician> musicians, OnMusicianClickListener listener) {
        this.musicians = musicians;
        this.listener  = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_musician_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Musician m = musicians.get(position);

        h.tvName.setText(m.getFullName());
        h.tvInstrument.setText(m.getInstrument());
        h.tvSkillLevel.setText(m.getSkillLevel());
        h.tvLocation.setText("📍 " + m.getLocation());
        h.tvPrice.setText("TZS " + TZS.format(m.getGigPrice()));
        h.ratingBar.setRating((float) m.getRating());
        h.tvRatingCount.setText(String.format("%.1f (%d)", m.getRating(), m.getTotalRatings()));

        // Availability badge
        boolean available = "Available".equals(m.getAvailabilityStatus());
        h.tvAvailability.setText(available ? "● Available" : "● Busy");
        h.tvAvailability.setTextColor(h.itemView.getContext().getResources()
                .getColor(available ? android.R.color.holo_green_dark
                                    : android.R.color.holo_red_dark, null));

        // 12-keys badge — only for Advanced musicians who confirmed it
        if ("Advanced".equals(m.getSkillLevel()) && m.isCanPlayAll12Keys()) {
            h.tvKeysTag.setVisibility(View.VISIBLE);
        } else {
            h.tvKeysTag.setVisibility(View.GONE);
        }

        // Profile image
        if (m.getProfileImageUrl() != null && !m.getProfileImageUrl().isEmpty()) {
            Glide.with(h.itemView)
                 .load(m.getProfileImageUrl())
                 .placeholder(R.drawable.ic_person)
                 .circleCrop()
                 .into(h.imgProfile);
        } else {
            h.imgProfile.setImageResource(R.drawable.ic_person);
        }

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onMusicianClick(m); });
    }

    @Override public int getItemCount() { return musicians.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView  tvName, tvInstrument, tvSkillLevel, tvLocation,
                  tvPrice, tvAvailability, tvRatingCount, tvKeysTag;
        RatingBar ratingBar;
        ViewHolder(View v) {
            super(v);
            imgProfile    = v.findViewById(R.id.imgProfile);
            tvName        = v.findViewById(R.id.tvName);
            tvInstrument  = v.findViewById(R.id.tvInstrument);
            tvSkillLevel  = v.findViewById(R.id.tvSkillLevel);
            tvLocation    = v.findViewById(R.id.tvLocation);
            tvPrice       = v.findViewById(R.id.tvPrice);
            tvAvailability= v.findViewById(R.id.tvAvailability);
            tvRatingCount = v.findViewById(R.id.tvRatingCount);
            tvKeysTag     = v.findViewById(R.id.tvKeysTag);
            ratingBar     = v.findViewById(R.id.ratingBar);
        }
    }
}
