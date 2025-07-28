package com.millenialzdev.reprosses;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnKeluar, btnInputBarang, btnDataBarang;
    private TextView tvMonitorData, tvProfilUser;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore firestore;
    private String selectedDate;
    private final String[] conditions = {"Damage", "Return", "Tercecer", "Basah", "Blank LOP", "Doubell TN"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            startActivity(new Intent(MainActivity.this, Login.class));
            finish();
        }

        tvMonitorData = findViewById(R.id.tvMonitorData);
        tvMonitorData.setTypeface(Typeface.MONOSPACE);

        tvProfilUser = findViewById(R.id.tvProfilUser);
        setupUserProfile();

        btnKeluar = findViewById(R.id.btnKeluar);
        btnInputBarang = findViewById(R.id.btnInputBarang);
        btnDataBarang = findViewById(R.id.btnDataBarang);

        btnKeluar.setOnClickListener(v -> logout());
        btnInputBarang.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, InputBarangActivity.class)));
        btnDataBarang.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DataBarangActivity.class)));

        getCurrentDate();
        displayTotalBasedOnConditions();
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        startActivity(new Intent(MainActivity.this, Login.class));
        finish();
    }

    private void setupUserProfile() {
        String username = sharedPreferences.getString("username", "User");
        String userId = sharedPreferences.getString("userId", "Unknown ID");
        SpannableString profileInfo = new SpannableString("👤 " + username + " (ID: " + userId + ") - Edit Profil");

        int start = profileInfo.toString().indexOf("Edit Profil");
        int end = start + "Edit Profil".length();

        if (start != -1) {
            profileInfo.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            profileInfo.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    startActivity(new Intent(MainActivity.this, EditUserActivity.class));
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            profileInfo.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvProfilUser.setText(profileInfo);
        tvProfilUser.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = sdf.parse(selectedDate);
            calendar.setTime(parsedDate);
        } catch (Exception e) {
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(selectedCal.getTime());
                    displayTotalBasedOnConditions();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void displayTotalBasedOnConditions() {
        String displayDate;
        try {
            Date parsedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate);
            displayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsedDate);
        } catch (Exception e) {
            displayDate = selectedDate;
        }

        String headerText = "Tanggal: " + displayDate + "  (Pilih Tanggal)  [Reset Tanggal]\n\n";
        SpannableString spannableString = new SpannableString(headerText);

        int pilihStart = headerText.indexOf("(Pilih Tanggal)");
        int pilihEnd = pilihStart + "(Pilih Tanggal)".length();
        if (pilihStart != -1) {
            spannableString.setSpan(new UnderlineSpan(), pilihStart, pilihEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    showDatePicker();
                }
            }, pilihStart, pilihEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), pilihStart, pilihEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int resetStart = headerText.indexOf("[Reset Tanggal]");
        int resetEnd = resetStart + "[Reset Tanggal]".length();
        if (resetStart != -1) {
            spannableString.setSpan(new UnderlineSpan(), resetStart, resetEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    getCurrentDate();
                    displayTotalBasedOnConditions();
                }
            }, resetStart, resetEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), resetStart, resetEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvMonitorData.setText(spannableString);
        tvMonitorData.setMovementMethod(LinkMovementMethod.getInstance());

        Arrays.sort(conditions);
        final SpannableString[] conditionResults = new SpannableString[conditions.length];
        final int[] completedConditions = {0};

        SpannableString openingLine = new SpannableString("\n--- Data Barang berdasarkan Kondisi ---\n");
        tvMonitorData.append(openingLine);

        for (int i = 0; i < conditions.length; i++) {
            final int index = i;
            String condition = conditions[i];
            String dbCondition = capitalizeWords(condition);

            firestore.collection("barang")
                    .whereEqualTo("tanggal", selectedDate)
                    .whereEqualTo("kondisi", dbCondition)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            int totalPcs = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Object pcsObj = document.get("pcs");
                                int pcs = 0;
                                try {
                                    if (pcsObj instanceof String) {
                                        pcs = Integer.parseInt(((String) pcsObj).trim());
                                    } else if (pcsObj instanceof Number) {
                                        pcs = ((Number) pcsObj).intValue();
                                    }
                                    totalPcs += pcs;
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Error membaca pcs", Toast.LENGTH_SHORT).show();
                                }
                            }

                            String conditionText = String.format(Locale.getDefault(), "%-25s : %5d pcs", dbCondition, totalPcs);
                            SpannableString conditionSpan = new SpannableString(conditionText);
                            conditionSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, conditionText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            conditionSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), 0, conditionText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            conditionResults[index] = conditionSpan;
                        }

                        completedConditions[0]++;
                        if (completedConditions[0] == conditions.length) {
                            for (SpannableString conditionSpan : conditionResults) {
                                if (conditionSpan != null) {
                                    tvMonitorData.append("\n");
                                    tvMonitorData.append(conditionSpan);
                                }
                            }
                            tvMonitorData.append("\n\n--- Data diperbarui ---");
                        }
                    });
        }
    }

    private String capitalizeWords(String input) {
        StringBuilder output = new StringBuilder();
        String[] words = input.trim().toLowerCase().split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                output.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return output.toString().trim();
    }
}
