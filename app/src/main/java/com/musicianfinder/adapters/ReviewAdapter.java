package com.musicianfinder.adapters;

import android.view.*;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.musicianfinder.R;
import com.musicianfinder.models.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final List<Review> reviews;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public ReviewAdapter(List<Review> reviews) { this.reviews = reviews; }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Review r = reviews.get(position);
        h.tvName.setText(r.getRequesterName());
        h.tvComment.setText(r.getComment());
        h.ratingBar.setRating(r.getRating());
        h.tvDate.setText(sdf.format(new Date(r.getCreatedAt())));
    }

    @Override public int getItemCount() { return reviews.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView  tvName, tvComment, tvDate;
        RatingBar ratingBar;
        ViewHolder(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvReviewerName);
            tvComment = v.findViewById(R.id.tvComment);
            tvDate    = v.findViewById(R.id.tvDate);
            ratingBar = v.findViewById(R.id.ratingBar);
        }
    }
}
