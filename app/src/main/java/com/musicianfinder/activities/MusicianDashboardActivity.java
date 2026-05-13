package com.musicianfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.musicianfinder.R;
import com.musicianfinder.adapters.GigRequestAdapter;
import com.musicianfinder.models.GigRequest;
import com.musicianfinder.models.Musician;
import com.musicianfinder.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MusicianDashboardActivity extends AppCompatActivity {

    private CircleImageView imgProfile;
    private TextView        tvName, tvStatus, tvRating, tvPendingCount;
    private Switch          swAvailability;
    private RecyclerView    rvRequests;
    private ProgressBar     progressBar;

    private FirebaseHelper    fb;
    private Musician          musician;
    private GigRequestAdapter adapter;
    private List<GigRequest>  requestList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musician_dashboard);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("My Dashboard");

        fb = FirebaseHelper.getInstance();
        bindViews();
        loadProfile();
    }

    private void bindViews() {
        imgProfile    = findViewById(R.id.imgProfile);
        tvName        = findViewById(R.id.tvName);
        tvStatus      = findViewById(R.id.tvStatus);
        tvRating      = findViewById(R.id.tvRating);
        tvPendingCount= findViewById(R.id.tvPendingCount);
        swAvailability= findViewById(R.id.swAvailability);
        rvRequests    = findViewById(R.id.rvRequests);
        progressBar   = findViewById(R.id.progressBar);

        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GigRequestAdapter(requestList, null);
        rvRequests.setAdapter(adapter);
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = fb.getCurrentUser().getUid();

        fb.getDb().collection(FirebaseHelper.COL_MUSICIANS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    if (doc.exists()) {
                        musician = doc.toObject(Musician.class);
                        renderProfile();
                        loadRequests(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    toast("Failed to load profile");
                });
    }

    private void renderProfile() {
        tvName.setText(musician.getFullName());
        tvRating.setText(String.format("⭐ %.1f (%d ratings)", musician.getRating(), musician.getTotalRatings()));
        tvStatus.setText(musician.getStatus());

        String statusColor = musician.getStatus().equals("Active") ? "#4CAF50" :
                             musician.getStatus().equals("Pending") ? "#FF9800" : "#F44336";
        tvStatus.setTextColor(android.graphics.Color.parseColor(statusColor));

        swAvailability.setChecked(musician.getAvailabilityStatus().equals("Available"));
        swAvailability.setOnCheckedChangeListener((btn, checked) -> {
            String newStatus = checked ? "Available" : "Busy";
            fb.updateAvailability(musician.getId(), newStatus, new FirebaseHelper.OnCompleteListener<Void>() {
                @Override public void onSuccess(Void r) { toast("Status: " + newStatus); }
                @Override public void onFailure(Exception e) { toast("Update failed"); }
            });
        });

        if (musician.getProfileImageUrl() != null)
            Glide.with(this).load(musician.getProfileImageUrl())
                 .placeholder(R.drawable.ic_person).into(imgProfile);

        // Pending status notice
        if ("Pending".equals(musician.getStatus())) {
            new AlertDialog.Builder(this)
                    .setTitle("Account Pending")
                    .setMessage("Your account is awaiting admin approval. You'll be notified once approved.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void loadRequests(String musicianId) {
        fb.getMusicianRequests(musicianId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    requestList.clear();
                    int pending = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        GigRequest req = doc.toObject(GigRequest.class);
                        if (req != null) {
                            requestList.add(req);
                            if ("Pending".equals(req.getStatus())) pending++;
                        }
                    }
                    tvPendingCount.setText("Pending Gigs: " + pending);
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.musician_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_profile) {
            startActivity(new Intent(this, MusicianProfileEditActivity.class));
        } else if (id == R.id.menu_dark_mode) {
            boolean dark = !fb.isDarkMode(this);
            fb.setDarkMode(this, dark);
            AppCompatDelegate.setDefaultNightMode(
                    dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } else if (id == R.id.menu_logout) {
            fb.logout();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
