package com.millenialzdev.reprosses;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InputBarangActivity extends AppCompatActivity {

    private EditText etNamaBarang, etSeller, etTanggal, etPcs, etRemaks;
    private Spinner spnKondisi;
    private ImageView imgBarang;
    private Button btnPilihGambar, btnSimpan, btnKembali;

    private Uri imageUri;
    private File photoFile;
    private FirebaseFirestore firestore;
    private Bitmap selectedBitmap;

    private static final int REQUEST_CAMERA = 1, REQUEST_GALLERY = 2;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_barang);

        etNamaBarang = findViewById(R.id.etNamaBarang);
        etSeller = findViewById(R.id.etSeller);
        etTanggal = findViewById(R.id.etTanggal);
        etPcs = findViewById(R.id.etPcs);
        etRemaks = findViewById(R.id.etRemarks);
        spnKondisi = findViewById(R.id.spnKondisi);
        imgBarang = findViewById(R.id.imgBarang);
        btnPilihGambar = findViewById(R.id.btnPilihGambar);
        btnSimpan = findViewById(R.id.btnSimpan);
        btnKembali = findViewById(R.id.btnKembali);

        firestore = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("userId", null);
        if (userId == null) {
            showToast("Gagal mengambil data user, silakan login ulang.");
            startActivity(new Intent(InputBarangActivity.this, Login.class));
            finish();
            return;
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.kondisi_list, android.R.layout.simple_spinner_dropdown_item);
        spnKondisi.setAdapter(adapter);

        spnKondisi.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = spnKondisi.getSelectedItem().toString().toLowerCase();
                etSeller.setVisibility(selected.equals("return") ? View.VISIBLE : View.GONE);
                etNamaBarang.setVisibility(selected.equals("tercecer") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                etSeller.setVisibility(View.GONE);
                etNamaBarang.setVisibility(View.GONE);
            }
        });

        btnPilihGambar.setOnClickListener(v -> showImagePicker());
        etTanggal.setOnClickListener(v -> showDatePicker());
        btnSimpan.setOnClickListener(v -> uploadData());
        btnKembali.setOnClickListener(v -> {
            startActivity(new Intent(InputBarangActivity.this, MainActivity.class));
            finish();
        });

        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            }, 100);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) ->
                        etTanggal.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showImagePicker() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Pilih Sumber Gambar");
        String[] options = {"Kamera", "Galeri"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                dispatchTakePictureIntent();
            } else {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQUEST_GALLERY);
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = File.createTempFile("IMG_", ".jpg", getExternalFilesDir(null));
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            } catch (IOException e) {
                showToast("Gagal membuat file untuk kamera");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == REQUEST_GALLERY && data != null) {
                    imageUri = data.getData();
                    Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    selectedBitmap = resizeBitmap(originalBitmap, 1024);
                    imgBarang.setImageBitmap(selectedBitmap);
                } else if (requestCode == REQUEST_CAMERA && imageUri != null && photoFile != null) {
                    FileInputStream fis = new FileInputStream(photoFile);
                    Bitmap originalBitmap = BitmapFactory.decodeStream(fis);
                    fis.close();
                    selectedBitmap = resizeBitmap(originalBitmap, 1024);
                    imgBarang.setImageBitmap(selectedBitmap);
                }
            } catch (IOException e) {
                showToast("Gagal memuat gambar");
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        int scaledWidth = Math.round(scale * width);
        int scaledHeight = Math.round(scale * height);
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos); // Kompresi 40%
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    private void uploadData() {
        String namaBarang = etNamaBarang.getText().toString().trim();
        String seller = etSeller.getText().toString().trim();
        String tanggal = etTanggal.getText().toString().trim();
        String kondisi = spnKondisi.getSelectedItem().toString();
        String pcsStr = etPcs.getText().toString().trim();
        String remaks = etRemaks.getText().toString().trim();

        if (kondisi.equalsIgnoreCase("Kondisi Barang")) {
            showToast("Silakan pilih kondisi barang yang valid");
            return;
        }

        if (TextUtils.isEmpty(tanggal) || TextUtils.isEmpty(pcsStr)) {
            showToast("Harap isi tanggal dan jumlah PCS");
            return;
        }

        int pcs;
        try {
            pcs = Integer.parseInt(pcsStr);
        } catch (NumberFormatException e) {
            showToast("Jumlah PCS harus berupa angka");
            return;
        }

        if (kondisi.equalsIgnoreCase("return") && TextUtils.isEmpty(seller)) {
            showToast("Nama seller harus diisi");
            return;
        }

        if (kondisi.equalsIgnoreCase("tercecer") && TextUtils.isEmpty(namaBarang)) {
            showToast("Nama barang harus diisi");
            return;
        }

        String imageBase64 = selectedBitmap != null ? encodeImageToBase64(selectedBitmap) : "";
        String formattedTanggal = formatTanggal(tanggal);

        Map<String, Object> barangMap = new HashMap<>();
        barangMap.put("photo", imageBase64);
        barangMap.put("namaBarang", namaBarang);
        barangMap.put("seller", seller);
        barangMap.put("tanggal", formattedTanggal);
        barangMap.put("kondisi", kondisi);
        barangMap.put("pcs", pcs);
        barangMap.put("remaks", remaks);
        barangMap.put("userId", userId);
        barangMap.put("status", "Belum diproses");
        barangMap.put("approvedBy", "");
        barangMap.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("barang").add(barangMap)
                .addOnSuccessListener(documentReference -> {
                    showToast("Data berhasil disimpan");
                    startActivity(new Intent(InputBarangActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Gagal menyimpan data"));
    }

    private void showToast(String message) {
        Toast.makeText(InputBarangActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private String formatTanggal(String tanggalInput) {
        try {
            String[] parts = tanggalInput.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);
        } catch (Exception e) {
            showToast("Format tanggal salah, gunakan dd/MM/yyyy");
            return tanggalInput;
        }
    }
}
