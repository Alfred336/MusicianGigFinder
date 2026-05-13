package com.musicianfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.musicianfinder.R;
import com.musicianfinder.utils.FirebaseHelper;

/**
 * Entry / Splash screen.
 * Users choose: Continue as Requester | Musician Login/Register | Admin Login
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseHelper fb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode before setting content
        FirebaseHelper helper = FirebaseHelper.getInstance();
        if (helper.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fb = FirebaseHelper.getInstance();

        // If musician already logged in → go to their dashboard
        if (fb.isLoggedIn() && "musician".equals(fb.getRole(this))) {
            startActivity(new Intent(this, MusicianDashboardActivity.class));
            finish();
            return;
        }
        if (fb.isLoggedIn() && "admin".equals(fb.getRole(this))) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
            return;
        }

        Button btnRequester = findViewById(R.id.btnRequester);
        Button btnMusician  = findViewById(R.id.btnMusician);
        Button btnAdmin     = findViewById(R.id.btnAdmin);

        btnRequester.setOnClickListener(v ->
                startActivity(new Intent(this, RequesterHomeActivity.class)));

        btnMusician.setOnClickListener(v ->
                startActivity(new Intent(this, MusicianLoginActivity.class)));

        btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLoginActivity.class)));
    }
}
