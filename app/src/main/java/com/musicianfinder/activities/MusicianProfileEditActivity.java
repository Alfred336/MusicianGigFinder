package com.musicianfinder.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.musicianfinder.R;
import com.musicianfinder.models.Musician;
import com.musicianfinder.utils.FirebaseHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class MusicianProfileEditActivity extends AppCompatActivity {

    private CircleImageView imgProfile;
    private EditText        etBio;
    private Spinner         spAvailability, spWorkType;
    private Button          btnSave;
    private ProgressBar     progressBar;

    private FirebaseHelper fb;
    private Musician       musician;
    private Uri            newImageUri;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) { newImageUri = uri; imgProfile.setImageURI(uri); }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musician_profile_edit);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Edit Profile");

        fb = FirebaseHelper.getInstance();
        imgProfile    = findViewById(R.id.imgProfile);
        etBio         = findViewById(R.id.etBio);
        spAvailability= findViewById(R.id.spAvailability);
        spWorkType    = findViewById(R.id.spWorkType);
        btnSave       = findViewById(R.id.btnSave);
        progressBar   = findViewById(R.id.progressBar);

        setupSpinners();
        loadProfile();

        imgProfile.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupSpinners() {
        setupSpinner(spAvailability, new String[]{"Available","Busy"});
        setupSpinner(spWorkType,     new String[]{"Anywhere","Church only"});
    }

    private void setupSpinner(Spinner sp, String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = fb.getCurrentUser().getUid();
        fb.getDb().collection(FirebaseHelper.COL_MUSICIANS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    if (doc.exists()) {
                        musician = doc.toObject(Musician.class);
                        populateFields();
                    }
                });
    }

    private void populateFields() {
        etBio.setText(musician.getBio());
        setSpinnerValue(spAvailability, musician.getAvailabilityStatus());
        setSpinnerValue(spWorkType,     musician.getWorkType());
        if (musician.getProfileImageUrl() != null)
            Glide.with(this).load(musician.getProfileImageUrl())
                 .placeholder(R.drawable.ic_person).into(imgProfile);
    }

    private void setSpinnerValue(Spinner sp, String value) {
        for (int i = 0; i < sp.getCount(); i++) {
            if (sp.getItemAtPosition(i).toString().equals(value)) {
                sp.setSelection(i); break;
            }
        }
    }

    private void saveProfile() {
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (newImageUri != null) {
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("profile_images/" + UUID.randomUUID() + ".jpg");
            ref.putFile(newImageUri)
               .addOnSuccessListener(snap -> ref.getDownloadUrl()
                   .addOnSuccessListener(url -> pushUpdate(url.toString())))
               .addOnFailureListener(e -> { toast("Image upload failed"); finishLoading(); });
        } else {
            pushUpdate(musician.getProfileImageUrl());
        }
    }

    private void pushUpdate(String imageUrl) {
        Map<String,Object> updates = new HashMap<>();
        updates.put("bio",                etBio.getText().toString().trim());
        updates.put("availabilityStatus", spAvailability.getSelectedItem().toString());
        updates.put("workType",           spWorkType.getSelectedItem().toString());
        if (imageUrl != null) updates.put("profileImageUrl", imageUrl);

        fb.getDb().collection(FirebaseHelper.COL_MUSICIANS)
                .document(fb.getCurrentUser().getUid())
                .update(updates)
                .addOnSuccessListener(v -> { toast("Profile updated ✔"); finish(); })
                .addOnFailureListener(e -> { toast("Update failed: " + e.getMessage()); finishLoading(); });
    }

    private void finishLoading() {
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(true);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
