package com.millenialzdev.reprosses;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    private EditText etUsername, etPassword, etConfirmPassword;
    private Spinner spnDivision;
    private Button btnRegister;
    private FirebaseFirestore firestore;

    // Flag untuk toggle password visibility
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spnDivision = findViewById(R.id.spnDivision);
        btnRegister = findViewById(R.id.btnRegister);

        firestore = FirebaseFirestore.getInstance();

        String[] divisionList = {"Pilih Division", "Reguler Worker", "Team Leader", "Captain", "Supervisor", "Manager"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, divisionList);
        spnDivision.setAdapter(adapter);

        // Setup toggle visibility untuk etPassword dan etConfirmPassword
        setupPasswordToggle();

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void setupPasswordToggle() {
        // Set drawableEnd icon awal (mata tertutup)
        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(this, R.drawable.baseline_lock_24),
                null,
                ContextCompat.getDrawable(this, R.drawable.ic_baseline_visibility_off_24),
                null
        );
        etConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(this, R.drawable.baseline_lock_24),
                null,
                ContextCompat.getDrawable(this, R.drawable.ic_baseline_visibility_off_24),
                null
        );

        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2; // drawableEnd index
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    // Toggle password visibility
                    if (isPasswordVisible) {
                        // Sembunyikan password
                        etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                ContextCompat.getDrawable(this, R.drawable.baseline_lock_24),
                                null,
                                ContextCompat.getDrawable(this, R.drawable.ic_baseline_visibility_off_24),
                                null
                        );
                        isPasswordVisible = false;
                    } else {
                        // Tampilkan password
                        etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                ContextCompat.getDrawable(this, R.drawable.baseline_lock_24),
                                null,
                                ContextCompat.getDrawable(this, R.drawable.ic_baseline_visibility_24),
                                null
                        );
                        isPasswordVisible = true;
                    }
                    // Pindahkan cursor ke akhir
                    etPassword.setSelection(etPassword.getText().length());
                    return true;
                }
            }
            return false;
        });

        etConfirmPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2; // drawableEnd index
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etConfirmPassword.getRight() - etConfirmPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    if (isConfirmPasswordVisible) {
                        etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        etConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
                                ContextCompat.getDrawable(this, R.drawable.baseline_lock_24),
                                null,
                                ContextCompat.getDrawable(this, R.drawable.ic_baseline_visibility_off_24),
                                null
                        );
                        isConfirmPasswordVisible = false;
                    } else {
                        etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        etConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
                                ContextCompat.getDrawable(this, R.drawable.baseline_lock_24),
                                null,
                                ContextCompat.getDrawable(this, R.drawable.ic_baseline_visibility_24),
                                null
                        );
                        isConfirmPasswordVisible = true;
                    }
                    etConfirmPassword.setSelection(etConfirmPassword.getText().length());
                    return true;
                }
            }
            return false;
        });
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String division = spnDivision.getSelectedItem().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showToast("Semua kolom harus diisi");
            return;
        }

        if (password.length() < 6) {
            showToast("Password harus minimal 6 karakter");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showToast("Password tidak cocok");
            return;
        }

        if (division.equals("Pilih Division")) {
            showToast("Harap pilih divisi yang valid");
            return;
        }

        String role = (division.equals("Reguler Worker") || division.equals("Team Leader") || division.equals("Captain")) ? "user" : "admin";

        firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        showToast("Username sudah digunakan, coba dengan username lain");
                        return;
                    }

                    firestore.collection("users")
                            .orderBy("id", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                int nextId = 1;
                                if (!snapshot.isEmpty()) {
                                    DocumentSnapshot lastDoc = snapshot.getDocuments().get(0);
                                    String lastUserId = lastDoc.getString("id");
                                    if (lastUserId != null && lastUserId.startsWith("Lex")) {
                                        try {
                                            int lastNumber = Integer.parseInt(lastUserId.substring(3));
                                            nextId = lastNumber + 1;
                                        } catch (NumberFormatException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                String userId = String.format("Lex%05d", nextId);

                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("id", userId);
                                userMap.put("username", username);
                                userMap.put("division", division);
                                userMap.put("role", role);
                                userMap.put("password", password);

                                firestore.collection("users")
                                        .document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener(aVoid -> {
                                            showToast("Registrasi berhasil");
                                            finish();
                                        })
                                        .addOnFailureListener(e -> showToast("Registrasi gagal: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> showToast("Gagal mengambil data ID pengguna terakhir: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showToast("Gagal memeriksa username: " + e.getMessage()));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
