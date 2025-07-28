package com.millenialzdev.reprosses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi sharedPreferences
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        firestore = FirebaseFirestore.getInstance();

        // Cek login sebelumnya
        checkLoginStatus();

        setupPasswordToggle();

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });
    }

    private void setupPasswordToggle() {
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etPassword.getCompoundDrawables()[DRAWABLE_END] != null) {
                    if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                        if (isPasswordVisible) {
                            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                            etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                    ContextCompat.getDrawable(this, R.drawable.baseline_lock_24),
                                    null,
                                    ContextCompat.getDrawable(this, R.drawable.ic_baseline_visibility_off_24),
                                    null
                            );
                            isPasswordVisible = false;
                        } else {
                            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                            etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                    ContextCompat.getDrawable(this, R.drawable.baseline_lock_24),
                                    null,
                                    ContextCompat.getDrawable(this, R.drawable.ic_baseline_visibility_24),
                                    null
                            );
                            isPasswordVisible = true;
                        }
                        etPassword.setSelection(etPassword.getText().length());
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showToast("Username dan Password harus diisi");
            return;
        }

        firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            if (document.exists()) {
                                String storedPassword = document.getString("password");
                                String userId = document.getString("id");
                                String role = document.getString("role");

                                if (storedPassword == null || userId == null || role == null) {
                                    showToast("Data pengguna tidak lengkap.");
                                    return;
                                }

                                if (storedPassword.equals(password)) {
                                    saveLoginSession(document); // Simpan data login

                                    showToast("Login berhasil");

                                    if ("admin".equals(role)) {
                                        startActivity(new Intent(Login.this, AdminActivity.class));
                                    } else {
                                        startActivity(new Intent(Login.this, MainActivity.class));
                                    }
                                    finish();
                                } else {
                                    showToast("Password salah");
                                }
                                return;
                            }
                        }
                    } else {
                        showToast("Akun tidak ditemukan");
                    }
                })
                .addOnFailureListener(e -> showToast("Gagal login: " + e.getMessage()));
    }

    private void saveLoginSession(DocumentSnapshot document) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", document.getString("id"));
        editor.putString("username", document.getString("username"));
        editor.putString("password", document.getString("password"));
        editor.putString("role", document.getString("role"));
        editor.putString("user_id", document.getId()); // Firestore document ID
        editor.apply();
    }

    private void checkLoginStatus() {
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String role = sharedPreferences.getString("role", "");

        if (isLoggedIn) {
            if ("admin".equals(role)) {
                startActivity(new Intent(Login.this, AdminActivity.class));
            } else {
                startActivity(new Intent(Login.this, MainActivity.class));
            }
            finish();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
