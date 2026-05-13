package com.musicianfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.musicianfinder.R;
import com.musicianfinder.utils.FirebaseHelper;
import com.musicianfinder.utils.ValidationUtils;

public class MusicianLoginActivity extends AppCompatActivity {

    private EditText   etEmail, etPassword;
    private Button     btnLogin, btnRegister;
    private ProgressBar progressBar;
    private FirebaseHelper fb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musician_login);

        fb = FirebaseHelper.getInstance();

        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, MusicianRegisterActivity.class)));
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        String emailErr = ValidationUtils.getEmailError(email);
        if (emailErr != null) { etEmail.setError(emailErr); etEmail.requestFocus(); return; }
        if (password.isEmpty()) { etPassword.setError("Password is required"); etPassword.requestFocus(); return; }

        setLoading(true);
        fb.loginMusician(email, password, new FirebaseHelper.OnCompleteListener<String>() {
            @Override public void onSuccess(String uid) {
                fb.saveRole(MusicianLoginActivity.this, "musician");
                setLoading(false);
                startActivity(new Intent(MusicianLoginActivity.this, MusicianDashboardActivity.class));
                finish();
            }
            @Override public void onFailure(Exception e) {
                setLoading(false);
                Toast.makeText(MusicianLoginActivity.this,
                        "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
