package com.musicianfinder.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.musicianfinder.R;
import com.musicianfinder.adapters.ReviewAdapter;
import com.musicianfinder.models.GigRequest;
import com.musicianfinder.models.Musician;
import com.musicianfinder.models.Review;
import com.musicianfinder.utils.FirebaseHelper;
import com.musicianfinder.utils.ValidationUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MusicianProfileActivity extends AppCompatActivity {

    private CircleImageView imgProfile;
    private TextView   tvName, tvSkill, tvInstrument, tvGender,
                       tvLocation, tvWorkType, tvBio, tvRating,
                       tvAvailability, tvPrice, tvKeysInfo;
    private RatingBar  ratingBar;
    private RecyclerView rvReviews;
    private com.google.android.material.button.MaterialButton btnRequest;
    private ProgressBar progressBar;

    private FirebaseHelper  fb;
    private Musician        musician;
    private ReviewAdapter   reviewAdapter;
    private List<Review>    reviewList = new ArrayList<>();

    private static final NumberFormat TZS = NumberFormat.getNumberInstance(Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musician_profile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Musician Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        fb = FirebaseHelper.getInstance();
        bindViews();

        String id = getIntent().getStringExtra("musicianId");
        if (id == null) { finish(); return; }
        loadMusician(id);
    }

    private void bindViews() {
        imgProfile    = findViewById(R.id.imgProfile);
        tvName        = findViewById(R.id.tvName);
        tvSkill       = findViewById(R.id.tvSkill);
        tvInstrument  = findViewById(R.id.tvInstrument);
        tvGender      = findViewById(R.id.tvGender);
        tvLocation    = findViewById(R.id.tvLocation);
        tvWorkType    = findViewById(R.id.tvWorkType);
        tvBio         = findViewById(R.id.tvBio);
        tvRating      = findViewById(R.id.tvRating);
        tvAvailability= findViewById(R.id.tvAvailability);
        tvPrice       = findViewById(R.id.tvPrice);
        tvKeysInfo    = findViewById(R.id.tvKeysInfo);
        ratingBar     = findViewById(R.id.ratingBar);
        rvReviews     = findViewById(R.id.rvReviews);
        btnRequest    = findViewById(R.id.btnRequest);
        progressBar   = findViewById(R.id.progressBar);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(reviewList);
        rvReviews.setAdapter(reviewAdapter);
    }

    private void loadMusician(String id) {
        progressBar.setVisibility(View.VISIBLE);
        fb.getDb().collection(FirebaseHelper.COL_MUSICIANS).document(id).get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    if (doc.exists()) {
                        musician = doc.toObject(Musician.class);
                        renderProfile();
                        loadReviews(id);
                    }
                })
                .addOnFailureListener(e -> { progressBar.setVisibility(View.GONE); finish(); });
    }

    private void renderProfile() {
        tvName.setText(musician.getFullName());
        tvSkill.setText("Level: " + musician.getSkillLevel());
        tvInstrument.setText("Instrument: " + musician.getInstrument());
        tvGender.setText("Gender: " + musician.getGender());
        tvLocation.setText("Location: " + musician.getLocation());
        tvWorkType.setText("Work Type: " + musician.getWorkType());
        tvBio.setText(musician.getBio() != null && !musician.getBio().isEmpty()
                ? musician.getBio() : "No bio provided.");
        tvRating.setText(String.format("%.1f / 5.0  (%d reviews)", musician.getRating(), musician.getTotalRatings()));
        tvAvailability.setText("● " + musician.getAvailabilityStatus());

        boolean available = "Available".equals(musician.getAvailabilityStatus());
        tvAvailability.setTextColor(available ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        // New pricing
        tvPrice.setText("Booking Fee: TZS " + TZS.format(musician.getGigPrice()));
        ratingBar.setRating((float) musician.getRating());

        // Advanced 12-keys indicator
        if ("Advanced".equals(musician.getSkillLevel())) {
            tvKeysInfo.setVisibility(View.VISIBLE);
            tvKeysInfo.setText(musician.isCanPlayAll12Keys()
                    ? "🎹 Can play all 12 musical keys ✓"
                    : "🎹 12-keys capability not confirmed");
            tvKeysInfo.setTextColor(musician.isCanPlayAll12Keys()
                    ? Color.parseColor("#1B5E20") : Color.parseColor("#B71C1C"));
        } else {
            tvKeysInfo.setVisibility(View.GONE);
        }

        if (musician.getProfileImageUrl() != null)
            Glide.with(this).load(musician.getProfileImageUrl())
                 .placeholder(R.drawable.ic_person).into(imgProfile);

        btnRequest.setEnabled(available);
        btnRequest.setText(available ? "🎵  Request This Musician" : "Currently Unavailable");
        btnRequest.setOnClickListener(v -> showRequestDialog());
    }

    private void loadReviews(String id) {
        fb.getMusicianReviews(id).get().addOnSuccessListener(snap -> {
            reviewList.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Review r = doc.toObject(Review.class);
                if (r != null) reviewList.add(r);
            }
            reviewAdapter.notifyDataSetChanged();
        });
    }

    // ── Gig request dialog ────────────────────────────────────────────────────
    private void showRequestDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_request_gig, null);

        com.google.android.material.textfield.TextInputEditText
                etName     = v.findViewById(R.id.etRequesterName),
                etPhone    = v.findViewById(R.id.etRequesterPhone),
                etEvent    = v.findViewById(R.id.etEventName),
                etDate     = v.findViewById(R.id.etEventDate),
                etTime     = v.findViewById(R.id.etEventTime),
                etLocation = v.findViewById(R.id.etEventLocation),
                etNotes    = v.findViewById(R.id.etNotes);

        new AlertDialog.Builder(this)
                .setTitle("Book " + musician.getFullName())
                .setMessage("Booking fee: TZS " + TZS.format(musician.getGigPrice()))
                .setView(v)
                .setPositiveButton("Proceed to Payment", (d, w) -> {
                    String name   = etName.getText().toString().trim();
                    String phone  = etPhone.getText().toString().trim();
                    String event  = etEvent.getText().toString().trim();
                    String date   = etDate.getText().toString().trim();
                    String time   = etTime.getText().toString().trim();
                    String loc    = etLocation.getText().toString().trim();
                    String notes  = etNotes.getText().toString().trim();

                    if (name.isEmpty())                             { toast("Your name is required");         return; }
                    if (!ValidationUtils.isValidPhone(phone))       { toast("Enter a valid phone number");     return; }
                    if (event.isEmpty())                            { toast("Event name is required");         return; }
                    if (date.isEmpty())                             { toast("Event date is required");         return; }
                    if (loc.isEmpty())                              { toast("Event location is required");     return; }

                    submitGigRequest(name, phone, event, date, time, loc, notes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitGigRequest(String name, String phone, String event,
                                   String date, String time, String loc, String notes) {
        GigRequest req = new GigRequest(musician.getId(), musician.getFullName(),
                name, phone, event, date, time, loc, notes, musician.getGigPrice());

        fb.submitGigRequest(req, new FirebaseHelper.OnCompleteListener<String>() {
            @Override public void onSuccess(String requestId) {
                // Open ClickPesa payment screen
                Intent intent = new Intent(MusicianProfileActivity.this, PaymentActivity.class);
                intent.putExtra(PaymentActivity.EXTRA_REQUEST_ID,    requestId);
                intent.putExtra(PaymentActivity.EXTRA_MUSICIAN_ID,   musician.getId());
                intent.putExtra(PaymentActivity.EXTRA_MUSICIAN_NAME, musician.getFullName());
                intent.putExtra(PaymentActivity.EXTRA_REQUESTER_NAME, name);
                intent.putExtra(PaymentActivity.EXTRA_AMOUNT,        musician.getGigPrice());
                startActivity(intent);
            }
            @Override public void onFailure(Exception e) { toast("Failed: " + e.getMessage()); }
        });
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
