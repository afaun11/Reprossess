package com.millenialzdev.reprosses;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditUserActivity extends AppCompatActivity {

    private EditText etUserId, etUsername, etPasswordLama, etPasswordBaru;
    private Button btnPerbaharuiData, btnSimpan;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore firestore;
    private String userId, currentUsername, currentPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        // Ambil data dari SharedPreferences
        userId = sharedPreferences.getString("userId", "");
        currentUsername = sharedPreferences.getString("username", "");
        currentPassword = sharedPreferences.getString("password", "");

        // Inisialisasi komponen UI
        etUserId = findViewById(R.id.etUserId);
        etUsername = findViewById(R.id.etUsername);
        etPasswordLama = findViewById(R.id.etPasswordLama);
        etPasswordBaru = findViewById(R.id.etPasswordBaru);
        btnPerbaharuiData = findViewById(R.id.btnPerbaharuiData);
        btnSimpan = findViewById(R.id.btnSimpan);

        // Set data awal
        etUserId.setText(userId);
        etUsername.setText(currentUsername);

        // Nonaktifkan input awal
        etUsername.setEnabled(false);
        etPasswordLama.setVisibility(View.GONE);
        etPasswordBaru.setVisibility(View.GONE);
        btnSimpan.setVisibility(View.GONE);

        // Saat klik "Perbaharui Data"
        btnPerbaharuiData.setOnClickListener(v -> {
            etUsername.setEnabled(true);
            etPasswordLama.setVisibility(View.VISIBLE);
            etPasswordBaru.setVisibility(View.VISIBLE);
            btnSimpan.setVisibility(View.VISIBLE);
        });

        // Saat klik "Simpan"
        btnSimpan.setOnClickListener(v -> {
            String newUsername = etUsername.getText().toString().trim();
            String oldPassword = etPasswordLama.getText().toString().trim();
            String newPassword = etPasswordBaru.getText().toString().trim();

            if (newUsername.isEmpty() || oldPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!oldPassword.equals(currentPassword)) {
                Toast.makeText(this, "Password lama tidak sesuai", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update ke Firestore
            DocumentReference userRef = firestore.collection("users").document(userId);
            userRef.update(
                    "username", newUsername,
                    "password", newPassword
            ).addOnSuccessListener(unused -> {
                // Update SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("username", newUsername);
                editor.putString("password", newPassword);
                editor.apply();

                Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show();
                finish();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Gagal memperbarui: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
    }
}
