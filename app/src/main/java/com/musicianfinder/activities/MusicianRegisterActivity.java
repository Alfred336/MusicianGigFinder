package com.musicianfinder.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.musicianfinder.R;
import com.musicianfinder.models.Musician;
import com.musicianfinder.utils.FirebaseHelper;
import com.musicianfinder.utils.ValidationUtils;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class MusicianRegisterActivity extends AppCompatActivity {

    private CircleImageView imgProfile;
    private com.google.android.material.textfield.TextInputEditText
            etFirstName, etLastName, etEmail, etPhone,
            etPassword, etConfirmPassword, etBio, etCountry;
    private Spinner  spSkillLevel, spInstrument, spGender, spWorkType, spRegion;
    private CheckBox cbAll12Keys;
    private LinearLayout layoutAdvancedCriteria;
    private RadioGroup rgLocation;
    private com.google.android.material.button.MaterialButton btnRegister;
    private ProgressBar progressBar;

    private Uri            profileImageUri;
    private FirebaseHelper fb;

    private static final String[] REGIONS = {
        "Select Region","Arusha","Dar es Salaam","Dodoma","Geita","Iringa",
        "Kagera","Katavi","Kigoma","Kilimanjaro","Lindi","Manyara","Mara",
        "Mbeya","Morogoro","Mtwara","Mwanza","Njombe","Pemba North",
        "Pemba South","Pwani","Rukwa","Ruvuma","Shinyanga","Simiyu",
        "Singida","Songwe","Tabora","Tanga","Zanzibar North","Zanzibar South",
        "Zanzibar West"
    };

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) { profileImageUri = uri; imgProfile.setImageURI(uri); }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musician_register);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Musician Registration");

        fb = FirebaseHelper.getInstance();
        bindViews();
        setupSpinners();
        setupListeners();
    }

    private void bindViews() {
        imgProfile              = findViewById(R.id.imgProfile);
        etFirstName             = findViewById(R.id.etFirstName);
        etLastName              = findViewById(R.id.etLastName);
        etEmail                 = findViewById(R.id.etEmail);
        etPhone                 = findViewById(R.id.etPhone);
        etPassword              = findViewById(R.id.etPassword);
        etConfirmPassword       = findViewById(R.id.etConfirmPassword);
        etBio                   = findViewById(R.id.etBio);
        etCountry               = findViewById(R.id.etCountry);
        spSkillLevel            = findViewById(R.id.spSkillLevel);
        spInstrument            = findViewById(R.id.spInstrument);
        spGender                = findViewById(R.id.spGender);
        spWorkType              = findViewById(R.id.spWorkType);
        spRegion                = findViewById(R.id.spRegion);
        cbAll12Keys             = findViewById(R.id.cbAll12Keys);
        layoutAdvancedCriteria  = findViewById(R.id.layoutAdvancedCriteria);
        rgLocation              = findViewById(R.id.rgLocation);
        btnRegister             = findViewById(R.id.btnRegister);
        progressBar             = findViewById(R.id.progressBar);
    }

    private void setupSpinners() {
        setupSpinner(spSkillLevel, new String[]{"Select Level","Medium","Advanced"});
        setupSpinner(spInstrument, new String[]{"Select Instrument","Drums","Piano","Guitar","Others"});
        setupSpinner(spGender,     new String[]{"Select Gender","Male","Female","Prefer not to say"});
        setupSpinner(spWorkType,   new String[]{"Select Work Type","Anywhere","Church only"});
        setupSpinner(spRegion,     REGIONS);
    }

    private void setupSpinner(Spinner sp, String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
    }

    private void setupListeners() {
        imgProfile.setOnClickListener(v -> imagePicker.launch("image/*"));

        // Show 12-keys checkbox only when Advanced is selected
        spSkillLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean isAdvanced = "Advanced".equals(spSkillLevel.getSelectedItem().toString());
                layoutAdvancedCriteria.setVisibility(isAdvanced ? View.VISIBLE : View.GONE);
                if (!isAdvanced) cbAll12Keys.setChecked(false);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        rgLocation.setOnCheckedChangeListener((g, id) -> {
            boolean isTz = (id == R.id.rbTanzania);
            spRegion.setVisibility(isTz ? View.VISIBLE : View.GONE);
            etCountry.setVisibility(isTz ? View.GONE : View.VISIBLE);
        });

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String firstName  = etFirstName.getText().toString().trim();
        String lastName   = etLastName.getText().toString().trim();
        String email      = etEmail.getText().toString().trim();
        String phone      = etPhone.getText().toString().trim();
        String password   = etPassword.getText().toString();
        String confirm    = etConfirmPassword.getText().toString();
        String bio        = etBio.getText().toString().trim();
        String skillLevel = spSkillLevel.getSelectedItem().toString();
        String instrument = spInstrument.getSelectedItem().toString();
        String gender     = spGender.getSelectedItem().toString();
        String workType   = spWorkType.getSelectedItem().toString();
        boolean isTanzania = rgLocation.getCheckedRadioButtonId() == R.id.rbTanzania;
        String region     = spRegion.getSelectedItem() != null ? spRegion.getSelectedItem().toString() : "";
        String country    = etCountry.getText().toString().trim();
        boolean canPlay12 = cbAll12Keys.isChecked();

        // ── Validation ────────────────────────────────────────────────────────
        String err;
        if ((err = ValidationUtils.getNameError(firstName))  != null) { etFirstName.setError(err);      etFirstName.requestFocus();      return; }
        if ((err = ValidationUtils.getNameError(lastName))   != null) { etLastName.setError(err);       etLastName.requestFocus();       return; }
        if ((err = ValidationUtils.getEmailError(email))     != null) { etEmail.setError(err);          etEmail.requestFocus();          return; }
        if ((err = ValidationUtils.getPhoneError(phone))     != null) { etPhone.setError(err);          etPhone.requestFocus();          return; }
        if ((err = ValidationUtils.getPasswordError(password))!=null) { etPassword.setError(err);       etPassword.requestFocus();       return; }
        if (!password.equals(confirm))                                 { etConfirmPassword.setError("Passwords do not match"); etConfirmPassword.requestFocus(); return; }
        if (skillLevel.startsWith("Select")) { toast("Please select a skill level"); return; }
        if (instrument.startsWith("Select")) { toast("Please select an instrument"); return; }
        if (gender.startsWith("Select"))     { toast("Please select gender");         return; }
        if (workType.startsWith("Select"))   { toast("Please select work type");      return; }
        if ("Advanced".equals(skillLevel) && !canPlay12) {
            toast("⚠️  Advanced musicians must be able to play all 12 musical keys.");
            cbAll12Keys.requestFocus();
            return;
        }
        if (isTanzania && region.startsWith("Select")) { toast("Please select your region"); return; }
        if (!isTanzania && country.isEmpty()) { etCountry.setError("Country is required"); etCountry.requestFocus(); return; }

        setLoading(true);

        Musician musician = new Musician(firstName, lastName, email, phone, gender,
                skillLevel, canPlay12, instrument, workType,
                isTanzania ? region : "",
                isTanzania ? "" : country);
        musician.setBio(bio);

        if (profileImageUri != null) {
            uploadProfileImage(profileImageUri, musician, password);
        } else {
            fb.registerMusician(musician, password, new FirebaseHelper.OnCompleteListener<String>() {
                @Override public void onSuccess(String uid) { onRegistered(); }
                @Override public void onFailure(Exception e) { showError(e.getMessage()); setLoading(false); }
            });
        }
    }

    private void uploadProfileImage(Uri uri, Musician musician, String password) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("profile_images/" + UUID.randomUUID() + ".jpg");
        ref.putFile(uri)
                .addOnSuccessListener(snap -> ref.getDownloadUrl()
                        .addOnSuccessListener(url -> {
                            musician.setProfileImageUrl(url.toString());
                            fb.registerMusician(musician, password, new FirebaseHelper.OnCompleteListener<String>() {
                                @Override public void onSuccess(String uid) { onRegistered(); }
                                @Override public void onFailure(Exception e) { showError(e.getMessage()); setLoading(false); }
                            });
                        }))
                .addOnFailureListener(e -> { showError("Image upload failed"); setLoading(false); });
    }

    private void onRegistered() {
        setLoading(false);
        fb.saveRole(this, "musician");
        toast("Registered! Awaiting admin approval.");
        startActivity(new Intent(this, MusicianDashboardActivity.class));
        finish();
    }

    private void setLoading(boolean l) {
        progressBar.setVisibility(l ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!l);
    }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); }
    private void showError(String msg) { toast("Error: " + msg); }
}
