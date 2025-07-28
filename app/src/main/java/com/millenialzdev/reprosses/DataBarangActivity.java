package com.millenialzdev.reprosses;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DataBarangActivity extends AppCompatActivity {
    private LinearLayout layoutContainer;
    private FirebaseFirestore firestore;
    private EditText etSearchNama, etSearchTanggal, etSearchSeller;
    private Spinner spnSearchKondisi;
    private Button btnSearch, btnReset, btnDeleteSelected, btnDownload;
    private TextView tvNoData;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_barang);

        layoutContainer = findViewById(R.id.layoutContainer);
        etSearchNama = findViewById(R.id.etSearchNama);
        etSearchTanggal = findViewById(R.id.etSearchTanggal);
        etSearchSeller = findViewById(R.id.etSearchSeller);
        spnSearchKondisi = findViewById(R.id.spnSearchKondisi);
        btnSearch = findViewById(R.id.btnSearch);
        btnReset = findViewById(R.id.btnReset);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        // *** TAMBAHAN: Tombol Download ***
        btnDownload = findViewById(R.id.btnDownload);

        tvNoData = findViewById(R.id.tvNoData);

        firestore = FirebaseFirestore.getInstance();

        loadData();

        etSearchTanggal.setOnClickListener(v -> showDatePicker());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.kondisi_list, android.R.layout.simple_spinner_dropdown_item);
        spnSearchKondisi.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String nama = etSearchNama.getText().toString().trim();
            String tanggal = etSearchTanggal.getText().toString().trim();
            String kondisi = spnSearchKondisi.getSelectedItem().toString();
            String seller = etSearchSeller.getText().toString().trim();
            if ("Kondisi Barang".equals(kondisi)) kondisi = "";
            loadDataWithFilter(nama, tanggal, kondisi, seller);
        });

        btnReset.setOnClickListener(v -> {
            etSearchNama.setText("");
            etSearchTanggal.setText("");
            etSearchSeller.setText("");
            spnSearchKondisi.setSelection(0);
            loadData();
        });

        btnDeleteSelected.setOnClickListener(v -> {
            boolean adaYangDipilih = false;

            for (int i = 0; i < layoutContainer.getChildCount(); i++) {
                View view = layoutContainer.getChildAt(i);
                if (view instanceof LinearLayout) {
                    CheckBox checkBox = (CheckBox) view.getTag(R.id.checkbox_tag);
                    if (checkBox != null && checkBox.isChecked()) {
                        adaYangDipilih = true;
                        break;
                    }
                }
            }

            if (!adaYangDipilih) {
                Toast.makeText(this, "Pilih barang yang ingin dihapus terlebih dahulu", Toast.LENGTH_SHORT).show();
            } else {
                showDeleteConfirmationDialog(); // Lanjut ke dialog konfirmasi jika ada yang dipilih
            }
        });

        // *** TAMBAHAN: fungsi download ***
        btnDownload.setOnClickListener(v -> {
            String tanggal = etSearchTanggal.getText().toString().trim();
            if (TextUtils.isEmpty(tanggal)) {
                Toast.makeText(this, "Pilih tanggal terlebih dahulu untuk download", Toast.LENGTH_SHORT).show();
            } else {
                downloadDataByTanggal(tanggal);
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    etSearchTanggal.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void loadData() {
        firestore.collection("barang")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutContainer.removeAllViews();
                    boolean hasData = false;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        hasData = true;
                        addBarangToLayout(doc);
                    }
                    tvNoData.setVisibility(hasData ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show());
    }

    private void loadDataWithFilter(String nama, String tanggal, String kondisi, String sellerFilter) {
        firestore.collection("barang")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutContainer.removeAllViews();
                    boolean hasData = false;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String namaBarang = doc.getString("namaBarang") != null ? doc.getString("namaBarang") : "";
                        String tanggalBarang = doc.getString("tanggal") != null ? doc.getString("tanggal") : "";
                        String kondisiBarang = doc.getString("kondisi") != null ? doc.getString("kondisi") : "";
                        String seller = doc.getString("seller") != null ? doc.getString("seller") : "";

                        boolean cocokNama = TextUtils.isEmpty(nama) || namaBarang.toLowerCase().contains(nama.toLowerCase());
                        boolean cocokTanggal = TextUtils.isEmpty(tanggal) || tanggalBarang.equals(tanggal);
                        boolean cocokKondisi = TextUtils.isEmpty(kondisi) || kondisiBarang.equalsIgnoreCase(kondisi);
                        boolean cocokSeller = TextUtils.isEmpty(sellerFilter) || seller.toLowerCase().contains(sellerFilter.toLowerCase());

                        if (cocokNama && cocokTanggal && cocokKondisi && cocokSeller) {
                            hasData = true;
                            addBarangToLayout(doc);
                        }
                    }
                    tvNoData.setVisibility(hasData ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show());
    }

    private void addBarangToLayout(DocumentSnapshot doc) {
        String currentUserId = getLoggedInUserId();
        String id = doc.getId();
        String namaBarang = doc.getString("namaBarang") != null ? doc.getString("namaBarang") : "-";
        String tanggal = doc.getString("tanggal") != null ? doc.getString("tanggal") : "-";
        String kondisi = doc.getString("kondisi") != null ? doc.getString("kondisi") : "-";
        String remaks = doc.getString("remaks") != null ? doc.getString("remaks") : "-";
        String userId = doc.getString("userId") != null ? doc.getString("userId") : "-";
        String status = doc.getString("status") != null ? doc.getString("status") : "Belum diproses";

        boolean isAutoApprove = kondisi.equalsIgnoreCase("Damage") ||
                kondisi.equalsIgnoreCase("Tercecer") ||
                kondisi.equalsIgnoreCase("Basah");

        // Jika auto approve dan status masih belum diproses, update ke "Sudah diproses"
        if (isAutoApprove && !status.equalsIgnoreCase("Sudah diproses")) {
            firestore.collection("barang").document(id)
                    .update("status", "Sudah diproses")
                    .addOnSuccessListener(aVoid -> loadData())
                    .addOnFailureListener(e -> Toast.makeText(this, "Gagal update status auto approve", Toast.LENGTH_SHORT).show());
            return; // jangan tampilkan data lama (yang statusnya belum terupdate)
        }

        // Layout tampilan seperti biasa
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 24, 24, 24);
        card.setBackgroundResource(R.drawable.card_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 32);
        card.setLayoutParams(params);

        String seller = doc.getString("seller") != null ? doc.getString("seller") : "-";
        if (kondisi.equalsIgnoreCase("Return")) {
            TextView tvSeller = new TextView(this);
            tvSeller.setText("Nama Seller: " + seller);
            tvSeller.setTextSize(16);
            tvSeller.setPadding(0, 4, 0, 4);
            card.addView(tvSeller);
        }

        String jumlah = "-";
        Object pcsObj = doc.get("pcs");
        if (pcsObj instanceof Long) {
            jumlah = String.valueOf((Long) pcsObj);
        } else if (pcsObj instanceof Double) {
            jumlah = String.valueOf(((Double) pcsObj).intValue());
        } else if (pcsObj instanceof String) {
            jumlah = (String) pcsObj;
        }

        String[][] details = {
                {"Nama Barang", namaBarang},
                {"Tanggal", tanggal},
                {"Kondisi", kondisi},
                {"Jumlah", jumlah},
                {"Remaks", remaks}
        };

        for (String[] item : details) {
            TextView tv = new TextView(this);
            tv.setText(item[0] + ": " + item[1]);
            tv.setTextSize(16);
            tv.setPadding(0, 4, 0, 4);
            card.addView(tv);
        }

        TextView tvStatus = new TextView(this);
        tvStatus.setText(status);
        tvStatus.setTextColor(status.equalsIgnoreCase("Sudah diproses") ? Color.GREEN : Color.RED);
        tvStatus.setTextSize(16);
        tvStatus.setPadding(0, 8, 0, 8);
        card.addView(tvStatus);

        CheckBox checkBox = null;
        if (userId.equals(currentUserId)) {
            checkBox = new CheckBox(this);
            checkBox.setText("Pilih untuk hapus");
            card.addView(checkBox);
        }

        // Tombol approve hanya untuk kondisi non-auto dan user yang berbeda
        if (!isAutoApprove && status.equalsIgnoreCase("Belum diproses") && !userId.equals(currentUserId)) {
            Button btnApprove = new Button(this);
            btnApprove.setText("Approve");
            btnApprove.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Konfirmasi")
                        .setMessage("Yakin ingin meng-approve barang ini?")
                        .setPositiveButton("Ya", (dialog, which) -> approveItem(id, userId))
                        .setNegativeButton("Batal", null)
                        .show();
            });
            card.addView(btnApprove);
        }

        TextView tvFoto = new TextView(this);
        tvFoto.setText("Lihat Foto");
        tvFoto.setTextColor(getResources().getColor(R.color.purple_700));
        tvFoto.setTypeface(null, android.graphics.Typeface.BOLD);
        tvFoto.setPadding(0, 12, 0, 0);
        tvFoto.setOnClickListener(v -> {
            String photoBase64 = doc.getString("photo");
            if (photoBase64 != null && !photoBase64.isEmpty()) {
                Bitmap bmp = decodeBase64ToBitmap(photoBase64);
                if (bmp != null) {
                    showPhotoDialog(bmp);
                } else {
                    Toast.makeText(this, "Gagal mendekode gambar", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Foto tidak tersedia", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(tvFoto);

        card.setTag(id);
        if (checkBox != null) {
            card.setTag(R.id.checkbox_tag, checkBox);
        }

        layoutContainer.addView(card);
    }

    private void approveItem(String id, String inputUserId) {
        String currentUserId = getLoggedInUserId();
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Gagal membaca ID user saat ini", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("barang").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String status = documentSnapshot.getString("status");
                    String docUserId = documentSnapshot.getString("userId");
                    String kondisi = documentSnapshot.getString("kondisi");

                    boolean isAutoApprove = kondisi.equalsIgnoreCase("Damage") ||
                            kondisi.equalsIgnoreCase("Tercecer") ||
                            kondisi.equalsIgnoreCase("Basah");

                    if ("Sudah diproses".equalsIgnoreCase(status)) {
                        Toast.makeText(this, "Barang ini sudah diproses", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isAutoApprove) {
                        Toast.makeText(this, "Barang ini sudah otomatis diproses", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentUserId.equals(docUserId)) {
                        Toast.makeText(this, "Anda tidak bisa approve barang yang Anda input sendiri", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firestore.collection("barang").document(id)
                            .update("status", "Sudah diproses", "approvedBy", currentUserId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Barang berhasil diproses", Toast.LENGTH_SHORT).show();
                                loadData();
                            });

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal approve barang", Toast.LENGTH_SHORT).show());
    }

    private String getLoggedInUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        return sharedPreferences.getString("userId", "");
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showPhotoDialog(Bitmap bmp) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Buat layout container
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 32, 32, 32);
        container.setGravity(Gravity.CENTER);

        // Buat ImageView dan atur skalanya
        ImageView imageView = new ImageView(this);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        int maxWidth = (int) (screenWidth * 0.9);  // 90% lebar layar
        int maxHeight = (int) (screenHeight * 0.7); // 70% tinggi layar

        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        float ratio = Math.min((float) maxWidth / bmpWidth, (float) maxHeight / bmpHeight);

        int scaledWidth = (int) (bmpWidth * ratio);
        int scaledHeight = (int) (bmpHeight * ratio);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(scaledWidth, scaledHeight);
        imageView.setLayoutParams(params);
        imageView.setImageBitmap(bmp);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        container.addView(imageView);
        scrollView.addView(container);
        builder.setView(scrollView);

        builder.setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Konfirmasi");
        builder.setMessage("Apakah Anda yakin ingin menghapus data yang dipilih?");
        builder.setPositiveButton("Ya", (dialog, which) -> deleteSelectedItems());
        builder.setNegativeButton("Tidak", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void deleteSelectedItems() {
        int count = layoutContainer.getChildCount();
        boolean anySelected = false;
        for (int i = 0; i < count; i++) {
            View child = layoutContainer.getChildAt(i);
            CheckBox cb = (CheckBox) child.getTag(R.id.checkbox_tag);
            if (cb != null && cb.isChecked()) {
                anySelected = true;
                String docId = (String) child.getTag();
                firestore.collection("barang").document(docId).delete();
            }
        }
        if (anySelected) {
            Toast.makeText(this, "Data berhasil dihapus", Toast.LENGTH_SHORT).show();
            loadData();
        } else {
            Toast.makeText(this, "Pilih data terlebih dahulu", Toast.LENGTH_SHORT).show();
        }
    }

    // ====== TAMBAHAN: Fungsi download data barang berdasarkan tanggal ======

    private void downloadDataByTanggal(String tanggal) {
        firestore.collection("barang")
                .whereEqualTo("tanggal", tanggal)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Tidak ada data pada tanggal tersebut", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        StringBuilder csvBuilder = new StringBuilder();
                        csvBuilder.append("Nama Barang,Kondisi,Jumlah,Remaks,Seller,Tanggal,Status\n");

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String namaBarang = doc.getString("namaBarang");
                            String kondisi = doc.getString("kondisi");
                            Object pcsObj = doc.get("pcs");
                            String jumlah = "";
                            if (pcsObj instanceof Long) {
                                jumlah = String.valueOf((Long) pcsObj);
                            } else if (pcsObj instanceof Double) {
                                jumlah = String.valueOf(((Double) pcsObj).intValue());
                            } else if (pcsObj instanceof String) {
                                jumlah = (String) pcsObj;
                            } else {
                                jumlah = "-";
                            }
                            String remaks = doc.getString("remaks");
                            String seller = doc.getString("seller");
                            String status = doc.getString("status");

                            csvBuilder.append("\"").append(namaBarang).append("\",")
                                    .append("\"").append(kondisi).append("\",")
                                    .append("\"").append(jumlah).append("\",")
                                    .append("\"").append(remaks).append("\",")
                                    .append("\"").append(seller).append("\",")
                                    .append("\"").append(tanggal).append("\",")
                                    .append("\"").append(status).append("\"\n");
                        }

                        saveCsvToFile(csvBuilder.toString(), "DataBarang_" + tanggal + ".csv");

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Gagal membuat file CSV", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal mengambil data untuk download", Toast.LENGTH_SHORT).show());
    }

    private void saveCsvToFile(String csvData, String fileName) {
        try {
            OutputStream outputStream;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    Toast.makeText(this, "Gagal membuat file", Toast.LENGTH_SHORT).show();
                    return;
                }
                outputStream = getContentResolver().openOutputStream(uri);
            } else {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                java.io.File file = new java.io.File(path, fileName);
                outputStream = new java.io.FileOutputStream(file);
            }

            if (outputStream != null) {
                outputStream.write(csvData.getBytes());
                outputStream.close();
                Toast.makeText(this, "File berhasil disimpan di folder Download", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Gagal menyimpan file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saat menyimpan file", Toast.LENGTH_SHORT).show();
        }
    }
}
