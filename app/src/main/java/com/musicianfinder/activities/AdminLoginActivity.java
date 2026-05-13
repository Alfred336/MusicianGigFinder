package com.musicianfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.musicianfinder.R;
import com.musicianfinder.utils.FirebaseHelper;
import com.musicianfinder.utils.ValidationUtils;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText   etEmail, etPassword;
    private Button     btnLogin;
    private ProgressBar progressBar;
    private FirebaseHelper fb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Admin Login");

        fb = FirebaseHelper.getInstance();

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        progressBar= findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        String emailErr = ValidationUtils.getEmailError(email);
        if (emailErr != null)  { etEmail.setError(emailErr);               etEmail.requestFocus();    return; }
        if (password.isEmpty()){ etPassword.setError("Password required"); etPassword.requestFocus(); return; }

        setLoading(true);
        fb.loginAdmin(email, password, new FirebaseHelper.OnCompleteListener<String>() {
            @Override public void onSuccess(String uid) {
                fb.saveRole(AdminLoginActivity.this, "admin");
                setLoading(false);
                startActivity(new Intent(AdminLoginActivity.this, AdminDashboardActivity.class));
                finish();
            }
            @Override public void onFailure(Exception e) {
                setLoading(false);
                Toast.makeText(AdminLoginActivity.this,
                        "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean l) {
        progressBar.setVisibility(l ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!l);
    }
}
