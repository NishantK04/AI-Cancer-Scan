package com.nishant.cancerprediction;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.util.Log;
import android.view.View;

import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HeapMap2Activity extends AppCompatActivity {

    // ─────────── CONFIG ───────────
    private static final String TAG = "HeapMap2Activity";
    private static final String FLASK_IP = "192.168.0.190";   // <- change if different
    private static final String POST_URL  = "http://" + FLASK_IP + ":5001/gradcam";
    private static final String IMAGE_URL = "http://" + FLASK_IP + ":5001/gradcam-image";
    // ──────────────────────────────

    private ImageView photoView, btnBack;
    private ProgressBar progressBar;
    private ImageButton btnZoom;
    private boolean isZoomed = false;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_heap_map2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        photoView   = findViewById(R.id.photoView);
        progressBar = findViewById(R.id.progressBar);
        btnZoom     = findViewById(R.id.btnZoom);
        btnBack     = findViewById(R.id.btnBack);
        progressBar.bringToFront();

        btnBack.setOnClickListener(v -> onBackPressed());

        btnZoom.setOnClickListener(v -> {
            photoView.animate()
                    .scaleX(isZoomed ? 1f : 2f)
                    .scaleY(isZoomed ? 1f : 2f)
                    .setDuration(250)
                    .start();
            isZoomed = !isZoomed;
        });
        String uriStr = getIntent().getStringExtra("image_uri");
        if (uriStr != null) {
            imageUri = Uri.parse(uriStr);
            uploadImage(imageUri);
        } else {
            Toast.makeText(this, "No image URI provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void uploadImage(Uri uri) {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open image stream");

            byte[] imgBytes = readBytes(in);

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "breast.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imgBytes))
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(java.time.Duration.ofSeconds(30))
                    .build();

            Request request = new Request.Builder()
                    .url(POST_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call,@NonNull IOException e) {
                    showError("Upload failed: " + e.getMessage());
                }

                @Override public void onResponse(@NonNull Call call,@NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        showError("Server error: " + response.code());
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        int pred     = json.getInt("prediction");   // 0 = Cancer, 1 = Non-Cancer
                        double conf  = json.getDouble("confidence");

                        if (pred == 0) {   // Cancer: download heatmap
                            fetchHeatmap(conf);
                        } else {           // Non-Cancer: just toast
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(HeapMap2Activity.this,
                                        String.format("Prediction: Non-Cancer\nConfidence: %.3f", conf),
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    } catch (JSONException e) {
                        showError("Bad JSON from server");
                    }
                }
            });

        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    private void fetchHeatmap(double conf) {
        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder().url(IMAGE_URL).get().build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call c,@NonNull IOException e) {
                showError("Fetch failed: " + e.getMessage());
            }

            @Override public void onResponse(@NonNull Call c,@NonNull Response r) throws IOException {
                if (!r.isSuccessful()) {
                    showError("Image fetch error: " + r.code());
                    return;
                }
                byte[] bytes = r.body().bytes();
                Bitmap bmp   = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

                runOnUiThread(() -> {
                    photoView.setImageBitmap(bmp);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(HeapMap2Activity.this,
                            String.format("Prediction: Cancer\nConfidence: %.3f", conf),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ───────────────────────── Utilities ─────────────────────────
    private void showError(String msg) {
        Log.e(TAG, msg);
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(HeapMap2Activity.this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[1024];
        int n;
        while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }
}