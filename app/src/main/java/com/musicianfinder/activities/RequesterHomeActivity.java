package com.musicianfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.musicianfinder.R;
import com.musicianfinder.adapters.MusicianAdapter;
import com.musicianfinder.models.Musician;
import com.musicianfinder.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RequesterHomeActivity extends AppCompatActivity
        implements MusicianAdapter.OnMusicianClickListener {

    // ── Views ─────────────────────────────────────────────────────────────────
    private RadioGroup rgScope;         // Tanzania / International
    private Spinner    spRegion;        // Tanzania regions
    private EditText   etCountry;       // International country
    private Spinner    spInstrument;    // Filter
    private Spinner    spSkillLevel;    // Filter
    private EditText   etSearch;        // Name search
    private RecyclerView rvMusicians;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    // ── Data ──────────────────────────────────────────────────────────────────
    private FirebaseHelper       fb;
    private MusicianAdapter      adapter;
    private List<Musician>       allMusicians  = new ArrayList<>();
    private List<Musician>       displayList   = new ArrayList<>();

    private static final String[] REGIONS = {
        "All Regions","Arusha","Dar es Salaam","Dodoma","Geita","Iringa",
        "Kagera","Katavi","Kigoma","Kilimanjaro","Lindi","Manyara","Mara",
        "Mbeya","Morogoro","Mtwara","Mwanza","Njombe","Pwani","Rukwa",
        "Ruvuma","Shinyanga","Singida","Tabora","Tanga"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requester_home);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Find a Musician");

        fb = FirebaseHelper.getInstance();
        bindViews();
        setupSpinners();
        setupListeners();
        loadMusicians("Tanzania", null, null);
    }

    private void bindViews() {
        rgScope      = findViewById(R.id.rgScope);
        spRegion     = findViewById(R.id.spRegion);
        etCountry    = findViewById(R.id.etCountry);
        spInstrument = findViewById(R.id.spInstrument);
        spSkillLevel = findViewById(R.id.spSkillLevel);
        etSearch     = findViewById(R.id.etSearch);
        rvMusicians  = findViewById(R.id.rvMusicians);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        rvMusicians.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new MusicianAdapter(displayList, this);
        rvMusicians.setAdapter(adapter);
    }

    private void setupSpinners() {
        setupSpinner(spRegion,    REGIONS);
        setupSpinner(spInstrument,new String[]{"All Instruments","Drums","Piano","Guitar","Others"});
        setupSpinner(spSkillLevel,new String[]{"All Levels","Medium","Advanced"});
    }

    private void setupSpinner(Spinner sp, String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
    }

    private void setupListeners() {
        rgScope.setOnCheckedChangeListener((g, id) -> {
            boolean isTz = (id == R.id.rbTanzania);
            spRegion.setVisibility(isTz ? View.VISIBLE : View.GONE);
            etCountry.setVisibility(isTz ? View.GONE   : View.VISIBLE);
            refreshData();
        });

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { refreshData(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spRegion.setOnItemSelectedListener(filterListener);
        spInstrument.setOnItemSelectedListener(filterListener);
        spSkillLevel.setOnItemSelectedListener(filterListener);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filterLocally(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void refreshData() {
        boolean isTz = rgScope.getCheckedRadioButtonId() == R.id.rbTanzania;
        String scope    = isTz ? "Tanzania" : "International";
        String region   = isTz ? spRegion.getSelectedItem().toString() : null;
        String country  = isTz ? null : etCountry.getText().toString().trim();
        loadMusicians(scope, region, country);
    }

    private void loadMusicians(String scope, String region, String country) {
        progressBar.setVisibility(View.VISIBLE);
        allMusicians.clear();
        displayList.clear();

        Query query;
        if ("Tanzania".equals(scope)) {
            String r = (region != null && !region.equals("All Regions")) ? region : null;
            query = fb.getTanzaniaMusicians(r);
        } else {
            query = fb.getInternationalMusicians(
                    (country != null && !country.isEmpty()) ? country : null);
        }

        // Apply instrument / skill filters
        String inst  = spInstrument.getSelectedItem().toString();
        String skill = spSkillLevel.getSelectedItem().toString();
        if (!inst.startsWith("All"))  query = query.whereEqualTo("instrument",  inst);
        if (!skill.startsWith("All")) query = query.whereEqualTo("skillLevel", skill);

        query.orderBy("rating", Query.Direction.DESCENDING)
             .get()
             .addOnSuccessListener(snap -> {
                 progressBar.setVisibility(View.GONE);
                 for (DocumentSnapshot doc : snap.getDocuments()) {
                     Musician m = doc.toObject(Musician.class);
                     if (m != null) allMusicians.add(m);
                 }
                 filterLocally(etSearch.getText().toString());
             })
             .addOnFailureListener(e -> {
                 progressBar.setVisibility(View.GONE);
                 Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
             });
    }

    /** Client-side name search */
    private void filterLocally(String query) {
        displayList.clear();
        String q = query.toLowerCase(Locale.ROOT).trim();
        for (Musician m : allMusicians) {
            if (q.isEmpty() || m.getFullName().toLowerCase(Locale.ROOT).contains(q)
                    || m.getInstrument().toLowerCase(Locale.ROOT).contains(q)) {
                displayList.add(m);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onMusicianClick(Musician musician) {
        Intent intent = new Intent(this, MusicianProfileActivity.class);
        intent.putExtra("musicianId", musician.getId());
        startActivity(intent);
    }
}
